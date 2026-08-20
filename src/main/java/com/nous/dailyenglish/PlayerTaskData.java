package com.nous.dailyenglish;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerTaskData {
    private final File file;
    private YamlConfiguration data;
    private final DailyEnglish plugin;

    public PlayerTaskData(DailyEnglish plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); }
            catch (IOException e) { plugin.getLogger().warning("Cannot create playerdata.yml"); }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    // Slot structure: players.<uuid>.slots.<0|1|2> = taskId|assignedAt|completed
    //                 players.<uuid>.lastRefresh = epoch ms

    public String getSlotTaskId(UUID uuid, int slot) {
        return data.getString(path(uuid, "slots." + slot + ".taskId"), null);
    }

    public long getSlotAssignedAt(UUID uuid, int slot) {
        return data.getLong(path(uuid, "slots." + slot + ".assignedAt"), 0);
    }

    public boolean isSlotCompleted(UUID uuid, int slot) {
        return data.getBoolean(path(uuid, "slots." + slot + ".completed"), false);
    }

    public void assignSlot(UUID uuid, int slot, String taskId) {
        String p = path(uuid, "slots." + slot);
        data.set(p + ".taskId", taskId);
        data.set(p + ".assignedAt", System.currentTimeMillis());
        data.set(p + ".completed", false);
        save();
    }

    public void completeSlot(UUID uuid, int slot) {
        data.set(path(uuid, "slots." + slot + ".completed"), true);
        save();
    }

    public void clearSlot(UUID uuid, int slot) {
        data.set(path(uuid, "slots." + slot), null);
        save();
    }

    public long getLastRefresh(UUID uuid) {
        return data.getLong(path(uuid, "lastRefresh"), 0);
    }

    public void setLastRefresh(UUID uuid, long time) {
        data.set(path(uuid, "lastRefresh"), time);
        save();
    }

    private String path(UUID uuid, String key) {
        return "players." + uuid.toString() + "." + key;
    }

    private void save() {
        try { data.save(file); } catch (IOException e) { plugin.getLogger().warning("Cannot save playerdata.yml"); }
    }
}
