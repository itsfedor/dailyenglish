package com.nous.dailyenglish;

import com.nous.dailyenglish.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class DailyEnglish extends JavaPlugin {
    private ConfigManager configManager;
    private TaskPool taskPool;
    private PlayerTaskData playerData;
    private EconomyManager economyManager;
    private TaskManager taskManager;
    private QuizManager quizManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        String apiKey = configManager.getGroqApiKey();
        if (apiKey.isEmpty() || apiKey.equals("YOUR_GROQ_API_KEY_HERE")) {
            getLogger().warning("Groq API key not set! Describe tasks will fail.");
        }

        this.economyManager = new EconomyManager();
        if (!economyManager.isReady()) {
            getLogger().severe("Vault not found! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Economy: " + economyManager.getName());

        this.playerData = new PlayerTaskData(this);
        this.taskPool = new TaskPool(this, getLogger());
        this.taskManager = new TaskManager(this, taskPool, playerData, economyManager, configManager);
        this.quizManager = new QuizManager(this, taskManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new CraftListener(taskManager), this);
        getServer().getPluginManager().registerEvents(new PlantListener(taskManager), this);
        getServer().getPluginManager().registerEvents(new FishListener(taskManager), this);
        getServer().getPluginManager().registerEvents(new RideListener(taskManager), this);
        getServer().getPluginManager().registerEvents(new TameListener(taskManager), this);
        getServer().getPluginManager().registerEvents(new DescribeListener(taskManager, configManager), this);
        getServer().getPluginManager().registerEvents(quizManager, this);

        // Register commands
        getCommand("tasks").setExecutor(new BookGUI(taskManager, configManager, quizManager));
        getCommand("dailyenglish").setExecutor(new BookGUI(taskManager, configManager, quizManager));

        getLogger().info("DailyEnglish v1.0.0 enabled! " + configManager.getRefreshMinutes() + "min refresh, "
            + configManager.getSlots() + " slots, rewards EASY=$" + String.format("%.0f", configManager.getRewardEasy())
            + " MEDIUM=$" + String.format("%.0f", configManager.getRewardMedium())
            + " HARD=$" + String.format("%.0f", configManager.getRewardHard()));
    }

    @Override
    public void onDisable() {
        getLogger().info("DailyEnglish disabled!");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public TaskManager getTaskManager() { return taskManager; }
}
