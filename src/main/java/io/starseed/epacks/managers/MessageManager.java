package io.starseed.epacks.managers;

import io.starseed.epacks.Epacks;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final Epacks plugin;
    private final Map<String, String> messages;

    public MessageManager(Epacks plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        loadMessages();
    }

    public void reloadMessages() {
        messages.clear();
        loadMessages();
    }

    private void loadMessages() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key));
            }
        }
    }

    public String getMessage(String key, String... placeholders) {
        String message = messages.getOrDefault(key, "Message not found: " + key);
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }



}
