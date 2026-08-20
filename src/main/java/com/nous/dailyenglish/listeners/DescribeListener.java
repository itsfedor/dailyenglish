package com.nous.dailyenglish.listeners;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nous.dailyenglish.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class DescribeListener implements Listener {
    private final TaskManager manager;
    private final ConfigManager config;
    private final Logger logger;
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    // Player has clicked [Click to write] — waiting for their chat message
    private final Map<UUID, String> pendingDescribe = new HashMap<>();

    public DescribeListener(TaskManager manager, ConfigManager config) {
        this.manager = manager;
        this.config = config;
        this.logger = Bukkit.getPluginManager().getPlugin("DailyEnglish").getLogger();
    }

    /** Handle /dailyenglish describe <taskId> — activates describe mode for that task */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (!msg.startsWith("/dailyenglish describe ")) return;

        event.setCancelled(true);
        String[] parts = msg.split(" ");
        if (parts.length < 3) return;
        String taskId = parts[2];

        Player player = event.getPlayer();
        manager.checkRefresh(player);

        int slot = manager.findSlotForTask(player.getUniqueId(), taskId);
        if (slot < 0) return;

        Task task = manager.getTaskAtSlot(player, slot);
        if (task == null || task.type != TaskType.DESCRIBE) return;
        if (manager.isSlotCompleted(player, slot)) return;

        // Activate describe mode
        pendingDescribe.put(player.getUniqueId(), taskId);

        player.sendMessage(Component.text(" "));
        player.sendMessage(Component.text("✏️ " + task.instruction, NamedTextColor.AQUA));
        player.sendMessage(Component.text("   Write at least " + task.minWords + " words in chat!", NamedTextColor.GRAY));
        player.sendMessage(Component.text("   Topic: " + task.topic, NamedTextColor.GRAY));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.2f);
    }

    /** Catch next chat message from player in describe mode */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String taskId = pendingDescribe.get(player.getUniqueId());
        if (taskId == null) return; // Not in describe mode

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (message.startsWith("/")) return;

        // Cancel event to prevent message from going to public chat
        event.setCancelled(true);

        int slot = manager.findSlotForTask(player.getUniqueId(), taskId);
        Task task = manager.getTaskAtSlot(player, slot);
        if (task == null || task.type != TaskType.DESCRIBE) {
            pendingDescribe.remove(player.getUniqueId());
            return;
        }

        String[] words = message.trim().split("\\s+");
        if (words.length < task.minWords) {
            player.sendMessage(Component.text(
                "✗ Too short! Write at least " + task.minWords + " words. You wrote " + words.length + ".",
                NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }

        // Echo the message back to the player (since we cancelled the event)
        player.sendMessage(Component.text(" ")
            .append(Component.text("[" + player.getName() + "] ", NamedTextColor.GRAY))
            .append(Component.text(message, NamedTextColor.WHITE)));

        pendingDescribe.remove(player.getUniqueId());

        // Show checking indicator
        player.sendMessage(Component.text("⏳ Checking your response...", NamedTextColor.GRAY));

        // Check with Groq async
        final String finalMessage = message;
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("DailyEnglish"), () -> {
            String result = checkWithGroq(task, finalMessage);
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("DailyEnglish"), () -> {
                if ("YES".equals(result)) {
                    manager.completeDescribeTask(player, slot, task);
                } else if ("ERROR".equals(result)) {
                    // API error — give benefit of doubt, accept the answer
                    logger.warning("Groq API error for " + player.getName() + " — auto-accepting describe task");
                    manager.completeDescribeTask(player, slot, task);
                } else {
                    // NO or other — reject
                    player.sendMessage(Component.text(
                        "✗ Your answer doesn't seem to describe the topic enough. Try adding more detail!",
                        NamedTextColor.RED));
                    player.sendMessage(Component.text(
                        "   Topic: " + task.topic + " | Wrote " + words.length + " words",
                        NamedTextColor.GRAY));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                }
            });
        });
    }

    /** Returns "YES", "NO", or "ERROR" */
    private String checkWithGroq(Task task, String message) {
        try {
            String apiKey = config.getGroqApiKey();
            if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_")) {
                logger.severe("Groq API key not set in DailyEnglish config! Falling back to keyword check.");
                // Fallback: simple keyword match
                String msgLower = message.toLowerCase();
                String topicLower = task.topic != null ? task.topic.toLowerCase() : "";
                if (!topicLower.isEmpty() && msgLower.contains(topicLower)) {
                    return "YES";
                }
                return "NO";
            }

            // Use a more lenient scoring prompt
            String prompt = "Task: Does this text explain or describe '" + task.topic 
                + "'? It should be in English and at least " + task.minWords + " words.\n"
                + "Rate: YES (good description/explanation) or NO (off-topic or gibberish).\n\n"
                + "Text: \"" + message + "\"\n\nReply with ONLY one word: YES or NO.";

            JsonObject sys = new JsonObject(); 
            sys.addProperty("role", "system");
            sys.addProperty("content", "You evaluate English writing tasks for ESL students. Be lenient — if the text is on-topic and makes sense, say YES. Only say NO if it's completely off-topic or nonsense. Reply with ONLY one word: YES or NO.");

            JsonObject usr = new JsonObject(); 
            usr.addProperty("role", "user");
            usr.addProperty("content", prompt);

            JsonObject body = new JsonObject();
            body.addProperty("model", config.getGroqModel());
            body.addProperty("temperature", 0.0);
            body.addProperty("max_tokens", 5);
            body.add("messages", GSON.toJsonTree(new JsonObject[]{sys, usr}));

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(config.getGroqTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (resp.statusCode() != 200) {
                logger.warning("Groq API returned " + resp.statusCode() + ": " + resp.body().substring(0, Math.min(200, resp.body().length())));
                return "ERROR";
            }

            JsonObject root = GSON.fromJson(resp.body(), JsonObject.class);
            String content = root.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString().trim().toUpperCase();
            
            logger.info("Describe check for '" + task.topic + "': " + content + " (player message: " + message.substring(0, Math.min(80, message.length())) + "...)");
            
            return content.contains("YES") ? "YES" : "NO";

        } catch (Exception e) {
            logger.warning("Groq API exception: " + e.getMessage());
            return "ERROR";
        }
    }
}
