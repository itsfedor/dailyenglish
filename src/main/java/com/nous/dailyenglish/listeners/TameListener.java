package com.nous.dailyenglish.listeners;

import com.nous.dailyenglish.TaskManager;
import com.nous.dailyenglish.TaskType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

public class TameListener implements Listener {
    private final TaskManager manager;

    public TameListener(TaskManager manager) { this.manager = manager; }

    @EventHandler(ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) return;
        Player player = (Player) event.getOwner();
        String type = event.getEntity().getType().name();
        manager.tryCompleteTask(player, TaskType.TAME, type, 1);
    }
}
