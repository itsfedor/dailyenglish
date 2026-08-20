package com.nous.dailyenglish;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

public class TaskManager {
    private final DailyEnglish plugin;
    private final TaskPool pool;
    private final PlayerTaskData data;
    private final EconomyManager economy;
    private final ConfigManager config;
    private final Logger logger;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    public TaskManager(DailyEnglish plugin, TaskPool pool, PlayerTaskData data,
                       EconomyManager economy, ConfigManager config) {
        this.plugin = plugin;
        this.pool = pool;
        this.data = data;
        this.economy = economy;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    /** Get player's English level from LuckPerms resolved prefix (e.g. [D1] [Admin] → D1) */
    public String getPlayerLevel(Player player) {
        try {
            LuckPerms lp = Bukkit.getServicesManager().load(LuckPerms.class);
            if (lp != null) {
                User user = lp.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    // Get resolved prefix from LP's cached meta (e.g. "§6[§lD1§r§6] §c[Admin]")
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null) {
                        logger.info("[" + player.getName() + "] LP prefix raw: " + prefix);
                        // Strip Minecraft color/format codes (§x or &x)
                        String clean = prefix.replaceAll("[§&][0-9a-fA-Fk-oK-OrR]", "");
                        logger.info("[" + player.getName() + "] LP prefix clean: " + clean);
                        // Extract first tag: [D1], [A0], [B2], etc.
                        java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("\\[([A-C][0-2]|D1)\\]").matcher(clean);
                        if (m.find()) {
                            String level = m.group(1).toLowerCase();
                            logger.info("[" + player.getName() + "] English level from prefix: " + level.toUpperCase());
                            return level;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Error getting player level: " + e.getMessage());
        }
        logger.info("[" + player.getName() + "] No level in prefix, using default: " + config.getDefaultLevel());
        return config.getDefaultLevel();
    }

    /** Check refresh for player — fill empty slots */
    public void checkRefresh(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastRefresh = data.getLastRefresh(uuid);
        long refreshMs = config.getRefreshMinutes() * 60L * 1000L;
        long timeoutMs = config.getSlotTimeoutMinutes() * 60L * 1000L;
        int slots = config.getSlots();
        String level = getPlayerLevel(player);

        if (lastRefresh == 0) {
            // First time: assign all slots
            Set<String> used = new HashSet<>();
            for (int i = 0; i < slots; i++) {
                Task task = pool.getRandomTaskExcluding(level, used);
                if (task != null) {
                    data.assignSlot(uuid, i, task.id);
                    used.add(task.id);
                }
            }
            data.setLastRefresh(uuid, now);
            return;
        }

        if (now - lastRefresh < refreshMs) return; // Not time yet

        // Time for refresh — fill empty/timed-out slots
        Set<String> used = new HashSet<>();
        for (int i = 0; i < slots; i++) {
            String taskId = data.getSlotTaskId(uuid, i);
            boolean completed = data.isSlotCompleted(uuid, i);
            long assignedAt = data.getSlotAssignedAt(uuid, i);

            if (completed) {
                // Slot done, fill with new
                Task task = pool.getRandomTaskExcluding(level, used);
                if (task != null) {
                    data.assignSlot(uuid, i, task.id);
                    used.add(task.id);
                }
            } else if (taskId == null) {
                // Empty slot, fill
                Task task = pool.getRandomTaskExcluding(level, used);
                if (task != null) {
                    data.assignSlot(uuid, i, task.id);
                    used.add(task.id);
                }
            } else if (now - assignedAt > timeoutMs) {
                // Timed out, replace
                Task task = pool.getRandomTaskExcluding(level, used);
                if (task != null) {
                    data.assignSlot(uuid, i, task.id);
                    used.add(task.id);
                }
            } else {
                // Active task — keep it, track ID to avoid duplicates
                used.add(taskId);
            }
        }
        data.setLastRefresh(uuid, now);
    }

    /** Try to complete a task by player action. Returns true if completed. */
    public boolean tryCompleteTask(Player player, TaskType type, String targetMatched, int count) {
        checkRefresh(player);
        UUID uuid = player.getUniqueId();
        int slots = config.getSlots();
        boolean foundType = false;

        for (int i = 0; i < slots; i++) {
            String taskId = data.getSlotTaskId(uuid, i);
            if (taskId == null || data.isSlotCompleted(uuid, i)) continue;

            Task task = pool.getById(taskId);
            if (task == null || task.type != type) continue;
            foundType = true;

            // Match target
            if (!matchesTarget(task, targetMatched)) {
                logger.info("[" + player.getName() + "] Target mismatch: task=" + task.target + " vs caught=" + targetMatched);
                continue;
            }

            completeTask(player, i, task);
            return true;
        }
        if (!foundType && type == TaskType.FISH) {
            logger.info("[" + player.getName() + "] No active FISH task found. Slots: " +
                java.util.Arrays.toString(getActiveSlotTypes(uuid)));
        }
        return false;
    }

    private String[] getActiveSlotTypes(UUID uuid) {
        String[] types = new String[config.getSlots()];
        for (int i = 0; i < config.getSlots(); i++) {
            String tid = data.getSlotTaskId(uuid, i);
            if (tid != null && !data.isSlotCompleted(uuid, i)) {
                Task t = pool.getById(tid);
                types[i] = t != null ? t.type.name() : "?";
            } else {
                types[i] = "-";
            }
        }
        return types;
    }

    private boolean matchesTarget(Task task, String matched) {
        if (task.target == null || task.target.isEmpty()) return false;
        String target = task.target.toLowerCase().replace("_", "");
        String match = matched.toLowerCase().replace("_", "");
        return match.contains(target) || target.contains(match) 
            || match.equalsIgnoreCase(task.target);
    }

    /** Complete a task and reward player */
    public void completeTask(Player player, int slot, Task task) {
        UUID uuid = player.getUniqueId();
        data.completeSlot(uuid, slot);

        double reward = task.getReward(config.getRewardEasy(), config.getRewardMedium(), config.getRewardHard());

        // Apply EnglishProgression multiplier
        try {
            Class<?> ep = Class.forName("com.nous.progression.EnglishProgression");
            double mult = (double) ep.getMethod("getMultiplier", org.bukkit.entity.Player.class).invoke(null, player);
            reward *= mult;
        } catch (Exception ignored) {}

        economy.deposit(player, reward);

        // Report to EnglishProgression
        try {
            Class<?> ep = Class.forName("com.nous.progression.EnglishProgression");
            ep.getMethod("addEarnings", org.bukkit.entity.Player.class, double.class).invoke(null, player, reward);
        } catch (Exception ignored) {}

        Component msg = LEGACY.deserialize(
            "&a\u2713 &f" + task.instruction + " &a+$" + String.format("%.0f", reward)
        );
        player.sendMessage(msg);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        logger.info("[" + player.getName() + "] Completed task: " + task.id + " +$" + String.format("%.0f", reward));
    }

    /** Find active DESCRIBE task for a player */
    public Task findActiveDescribeTask(Player player) {
        checkRefresh(player);
        UUID uuid = player.getUniqueId();
        for (int i = 0; i < config.getSlots(); i++) {
            String taskId = data.getSlotTaskId(uuid, i);
            if (taskId == null || data.isSlotCompleted(uuid, i)) continue;
            Task task = pool.getById(taskId);
            if (task != null && task.type == TaskType.DESCRIBE) {
                return task;
            }
        }
        return null;
    }

    /** Complete DESCRIBE task by slot index */
    public void completeDescribeTask(Player player, int slot, Task task) {
        completeTask(player, slot, task);
    }

    /** Find slot index for a task */
    public int findSlotForTask(UUID uuid, String taskId) {
        for (int i = 0; i < config.getSlots(); i++) {
            if (taskId.equals(data.getSlotTaskId(uuid, i))) return i;
        }
        return -1;
    }

    /** Get task at slot */
    public Task getTaskAtSlot(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        String taskId = data.getSlotTaskId(uuid, slot);
        if (taskId == null) return null;
        return pool.getById(taskId);
    }

    public boolean isSlotCompleted(Player player, int slot) {
        return data.isSlotCompleted(player.getUniqueId(), slot);
    }

    public long getSlotAssignedAt(Player player, int slot) {
        return data.getSlotAssignedAt(player.getUniqueId(), slot);
    }

    public long getLastRefresh(Player player) {
        return data.getLastRefresh(player.getUniqueId());
    }
}
