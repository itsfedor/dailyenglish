package com.nous.dailyenglish;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BookGUI implements CommandExecutor {
    private final TaskManager manager;
    private final ConfigManager config;
    private final QuizManager quizManager;

    public BookGUI(TaskManager manager, ConfigManager config, QuizManager quizManager) {
        this.manager = manager;
        this.config = config;
        this.quizManager = quizManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        manager.checkRefresh(player);

        int slots = config.getSlots();
        long now = System.currentTimeMillis();
        long refreshMs = config.getRefreshMinutes() * 60L * 1000L;
        long lastRefresh = manager.getLastRefresh(player);
        long nextRefresh = lastRefresh + refreshMs;
        long remaining = Math.max(0, nextRefresh - now);
        long remainingMin = remaining / 60000;

        // Header
        player.sendMessage(Component.text(" "));
        player.sendMessage(Component.text("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", NamedTextColor.DARK_GRAY));
        player.sendMessage(
            Component.text("📖 Daily English Tasks", NamedTextColor.GOLD, TextDecoration.BOLD)
        );
        player.sendMessage(
            Component.text("Next refresh in " + remainingMin + " min — Level: " 
                + manager.getPlayerLevel(player).toUpperCase(), NamedTextColor.GRAY)
        );
        player.sendMessage(Component.text("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", NamedTextColor.DARK_GRAY));

        for (int i = 0; i < slots; i++) {
            Task task = manager.getTaskAtSlot(player, i);
            boolean completed = manager.isSlotCompleted(player, i);

            if (task == null) {
                player.sendMessage(Component.text((i+1) + ". ○ Empty slot", NamedTextColor.DARK_GRAY));
                continue;
            }

            String diffLabel = getDiffLabel(task.difficulty);
            Component line;

            if (completed) {
                line = Component.text("§a✔ " + (i+1) + ". §7§m" + task.instruction + "§r " + diffLabel);
            } else if (task.type == TaskType.QUIZ) {
                line = Component.text("§6● " + (i+1) + ". §e" + task.instruction + " " + diffLabel
                    + " §6[Click to answer]", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/dailyenglish quiz " + task.id));
            } else if (task.type == TaskType.DESCRIBE) {
                line = Component.text("§6● " + (i+1) + ". §b" + task.instruction + " " + diffLabel
                    + " §b[Click to write]", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/dailyenglish describe " + task.id));
            } else {
                line = Component.text("§6● " + (i+1) + ". §f" + task.instruction + " " + diffLabel);
            }

            player.sendMessage(line);
        }

        player.sendMessage(Component.text("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("💡 Complete tasks by playing! Rewards: Easy=$5 Medium=$10 Hard=$15", NamedTextColor.GRAY));
        player.sendMessage(Component.text("   Click on quiz/describe tasks above to start them!", NamedTextColor.GRAY));

        return true;
    }

    private String getDiffLabel(Difficulty d) {
        switch (d) {
            case EASY: return "§a[$5]";
            case MEDIUM: return "§e[$10]";
            case HARD: return "§c[$15]";
            default: return "";
        }
    }
}
