package com.nous.dailyenglish.listeners;

import com.nous.dailyenglish.DailyEnglish;
import com.nous.dailyenglish.TaskManager;
import com.nous.dailyenglish.TaskType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.logging.Logger;

public class FishListener implements Listener {
    private final TaskManager manager;
    private final Logger logger;

    public FishListener(TaskManager manager) {
        this.manager = manager;
        this.logger = Bukkit.getLogger();
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        PlayerFishEvent.State state = event.getState();
        Player player = event.getPlayer();

        if (state != PlayerFishEvent.State.CAUGHT_FISH) return;

        logger.info("[FishListener] " + player.getName() + " caught: " + event.getCaught());

        if (event.getCaught() == null) {
            logger.warning("[FishListener] getCaught() is null for " + player.getName());
            return;
        }

        if (!(event.getCaught() instanceof Item)) {
            logger.warning("[FishListener] getCaught() is not Item: " + event.getCaught().getClass().getName());
            return;
        }

        Item item = (Item) event.getCaught();
        if (item.getItemStack() == null) {
            logger.warning("[FishListener] ItemStack is null for " + player.getName());
            return;
        }

        String type = item.getItemStack().getType().name();
        logger.info("[FishListener] " + player.getName() + " caught item: " + type);

        boolean completed = manager.tryCompleteTask(player, TaskType.FISH, type, 1);
        if (!completed) {
            logger.info("[FishListener] " + player.getName() + " caught " + type + " but no matching FISH task active");
        }
    }
}
