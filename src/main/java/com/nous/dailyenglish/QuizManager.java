package com.nous.dailyenglish;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.*;

public class QuizManager implements Listener {
    private final TaskManager manager;
    private final DailyEnglish plugin;
    private final Map<UUID, String> pendingQuiz = new HashMap<>();

    public QuizManager(DailyEnglish plugin, TaskManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /** Send quiz question with clickable answers */
    public void sendQuiz(Player player, Task task) {
        pendingQuiz.put(player.getUniqueId(), task.id);
        player.sendMessage(Component.text(" "));
        player.sendMessage(Component.text("❓ " + task.instruction, NamedTextColor.GOLD));
        player.sendMessage(Component.text(" "));

        List<String> options = task.quizOptions;
        if (options == null || options.isEmpty()) return;

        String[] letters = {"A", "B", "C", "D"};
        for (int i = 0; i < options.size() && i < letters.length; i++) {
            String cmd = "/dailyenglish answer " + task.id + " " + i;
            Component btn = Component.text("    [" + letters[i] + "] ", NamedTextColor.GRAY)
                .append(Component.text(options.get(i), NamedTextColor.YELLOW))
                .clickEvent(ClickEvent.runCommand(cmd));
            player.sendMessage(btn);
        }
        player.sendMessage(Component.text(" "));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        Player player = event.getPlayer();

        // Handle /dailyenglish quiz <taskId>
        if (msg.startsWith("/dailyenglish quiz ")) {
            event.setCancelled(true);
            String[] parts = msg.split(" ");
            if (parts.length < 3) return;
            String taskId = parts[2];

            manager.checkRefresh(player);
            int slot = manager.findSlotForTask(player.getUniqueId(), taskId);
            if (slot < 0) return;

            Task task = manager.getTaskAtSlot(player, slot);
            if (task == null || task.type != TaskType.QUIZ) return;
            if (manager.isSlotCompleted(player, slot)) return;

            sendQuiz(player, task);
            return;
        }

        // Handle /dailyenglish answer <taskId> <index>
        if (!msg.startsWith("/dailyenglish answer ")) return;
        event.setCancelled(true);

        String[] parts = msg.split(" ");
        if (parts.length < 4) return;

        String taskId = parts[2];
        int answerIdx;
        try { answerIdx = Integer.parseInt(parts[3]); } catch (NumberFormatException e) { return; }

        String pending = pendingQuiz.get(player.getUniqueId());
        if (pending == null || !pending.equals(taskId)) return;

        int slot = manager.findSlotForTask(player.getUniqueId(), taskId);
        Task task = manager.getTaskAtSlot(player, slot);
        if (task == null || task.type != TaskType.QUIZ) return;

        if (answerIdx == task.quizCorrect) {
            pendingQuiz.remove(player.getUniqueId());
            manager.completeTask(player, slot, task);
        } else {
            player.sendMessage(Component.text("✗ Not quite! Try again.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        }
    }
}
