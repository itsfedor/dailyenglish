package com.nous.dailyenglish;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class TaskPool {
    private final Map<String, List<Task>> poolByLevel = new HashMap<>();
    private final Logger logger;

    public TaskPool(DailyEnglish plugin, Logger logger) {
        this.logger = logger;
        String[] levels = {"a0", "a1", "a2", "b1", "b2", "c1", "c2", "d1"};
        for (String level : levels) {
            loadLevel(plugin, level);
        }
    }

    private void loadLevel(DailyEnglish plugin, String level) {
        String path = "tasks/tasks_" + level + ".yml";
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> list = yaml.getMapList("tasks");
        List<Task> tasks = new ArrayList<>();
        for (Map<?, ?> map : list) {
            Task t = new Task();
            t.id = String.valueOf(map.get("id"));
            t.type = TaskType.valueOf(String.valueOf(map.get("type")).toUpperCase());
            t.difficulty = Difficulty.valueOf(String.valueOf(map.get("difficulty")).toUpperCase());
            t.level = level;
            t.instruction = String.valueOf(map.get("instruction"));
            t.target = map.containsKey("target") ? String.valueOf(map.get("target")) : "";
            t.amount = map.containsKey("amount") ? ((Number)map.get("amount")).intValue() : 1;
            t.minWords = map.containsKey("min_words") ? ((Number)map.get("min_words")).intValue() : 8;
            t.topic = map.containsKey("topic") ? String.valueOf(map.get("topic")) : "";
            t.quizCorrect = map.containsKey("correct") ? ((Number)map.get("correct")).intValue() : 0;
            if (map.containsKey("options")) {
                @SuppressWarnings("unchecked")
                List<String> opts = (List<String>) map.get("options");
                t.quizOptions = opts;
            }
            tasks.add(t);
        }
        poolByLevel.put(level, tasks);
        logger.info("Loaded " + tasks.size() + " tasks for level " + level.toUpperCase());
    }

    public List<Task> getTasksForLevel(String level) {
        return poolByLevel.getOrDefault(level.toLowerCase(), poolByLevel.get("a0"));
    }

    public Task getRandomTask(String level) {
        List<Task> pool = getTasksForLevel(level);
        if (pool.isEmpty()) return null;
        return pool.get(new Random().nextInt(pool.size()));
    }

    public Task getRandomTaskExcluding(String level, Set<String> excludeIds) {
        List<Task> pool = getTasksForLevel(level);
        List<Task> candidates = new ArrayList<>();
        for (Task t : pool) {
            if (!excludeIds.contains(t.id)) candidates.add(t);
        }
        if (candidates.isEmpty()) candidates = pool;
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    public Task getById(String id) {
        for (List<Task> pool : poolByLevel.values()) {
            for (Task t : pool) {
                if (t.id.equals(id)) return t;
            }
        }
        return null;
    }
}
