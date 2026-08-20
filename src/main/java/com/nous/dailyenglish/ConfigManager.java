package com.nous.dailyenglish;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final DailyEnglish plugin;
    private FileConfiguration config;

    public ConfigManager(DailyEnglish plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public String getGroqApiKey() { return config.getString("groq.api-key", ""); }
    public String getGroqModel() { return config.getString("groq.model", "llama-3.1-8b-instant"); }
    public int getGroqTimeout() { return config.getInt("groq.timeout-seconds", 10); }
    public double getRewardEasy() { return config.getDouble("rewards.easy", 5.0); }
    public double getRewardMedium() { return config.getDouble("rewards.medium", 10.0); }
    public double getRewardHard() { return config.getDouble("rewards.hard", 15.0); }
    public int getRefreshMinutes() { return config.getInt("refresh.interval-minutes", 120); }
    public int getSlotTimeoutMinutes() { return config.getInt("refresh.slot-timeout-minutes", 240); }
    public int getSlots() { return config.getInt("refresh.slots", 3); }
    public String getLevelTrack() { return config.getString("levels.track", "english"); }
    public String getDefaultLevel() { return config.getString("levels.default", "a0"); }
}
