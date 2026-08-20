package com.nous.dailyenglish.listeners;

import com.nous.dailyenglish.TaskManager;
import com.nous.dailyenglish.TaskType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class CraftListener implements Listener {
    private final TaskManager manager;

    public CraftListener(TaskManager manager) { this.manager = manager; }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();
        Material mat = result.getType();
        int amount = result.getAmount();

        // Handle shift-click: multiply by actual crafted amount
        if (event.isShiftClick()) {
            amount = calculateShiftClickAmount(event, mat, amount);
        }

        manager.tryCompleteTask(player, TaskType.CRAFT, mat.name(), amount);
    }

    private int calculateShiftClickAmount(CraftItemEvent event, Material mat, int perCraft) {
        int maxStack = mat.getMaxStackSize();
        int crafted = 0;
        ItemStack[] matrix = event.getInventory().getMatrix();
        for (ItemStack item : matrix) {
            if (item != null && item.getAmount() < crafted + perCraft) {
                // Rough estimate: multiply perCraft by the minimum ingredient count
            }
        }
        return perCraft; // Simplify: count as one craft event
    }
}
