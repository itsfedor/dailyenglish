package com.nous.dailyenglish.listeners;

import com.nous.dailyenglish.TaskManager;
import com.nous.dailyenglish.TaskType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlantListener implements Listener {
    private final TaskManager manager;

    public PlantListener(TaskManager manager) { this.manager = manager; }

    @EventHandler(ignoreCancelled = true)
    public void onPlant(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material mat = event.getBlockPlaced().getType();
        // Only count plantable items: saplings, seeds, flowers, crops
        if (!isPlantable(mat)) return;
        manager.tryCompleteTask(player, TaskType.PLANT, mat.name(), 1);
    }

    private boolean isPlantable(Material mat) {
        String name = mat.name();
        return name.contains("SAPLING") || name.contains("SEEDS")
            || name.contains("WHEAT") || name.contains("CARROT")
            || name.contains("POTATO") || name.contains("BEETROOT")
            || name.contains("MELON") || name.contains("PUMPKIN")
            || name.contains("BAMBOO") || name.contains("SUGAR_CANE")
            || name.contains("CACTUS") || name.contains("VINE")
            || name.contains("FLOWER") || name.contains("TULIP")
            || name.contains("DAISY") || name.contains("ORCHID")
            || name.contains("DANDELION") || name.contains("POPPY")
            || name.contains("ALLIUM") || name.contains("BLUET")
            || name.contains("ROSE") || name.contains("LILAC")
            || name.contains("PEONY") || name.contains("SUNFLOWER")
            || name.contains("MUSHROOM") || name.contains("FUNGUS")
            || name.contains("KELP") || name.contains("SEA_PICKLE")
            || name.contains("SWEET_BERRIES") || name.contains("GLOW_BERRIES")
            || name.contains("COCOA") || name.contains("NETHER_WART")
            || name.contains("CHORUS") || name.equals("GRASS")
            || name.equals("FERN") || name.equals("DEAD_BUSH");
    }
}
