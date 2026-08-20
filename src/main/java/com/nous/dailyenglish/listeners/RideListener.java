package com.nous.dailyenglish.listeners;

import com.nous.dailyenglish.TaskManager;
import com.nous.dailyenglish.TaskType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class RideListener implements Listener {
    private final TaskManager manager;

    public RideListener(TaskManager manager) { this.manager = manager; }

    @EventHandler(ignoreCancelled = true)
    public void onRide(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) return;
        Player player = (Player) event.getEntered();
        Vehicle vehicle = event.getVehicle();
        String type;

        if (vehicle instanceof Boat) {
            type = ((Boat) vehicle).getBoatType().name() + "_BOAT";
        } else if (vehicle instanceof Minecart) {
            type = "MINECART";
        } else if (vehicle instanceof Horse) {
            type = "HORSE";
        } else if (vehicle instanceof Pig) {
            type = "PIG";
        } else if (vehicle instanceof Strider) {
            type = "STRIDER";
        } else {
            type = vehicle.getType().name();
        }

        manager.tryCompleteTask(player, TaskType.RIDE, type, 1);
    }
}
