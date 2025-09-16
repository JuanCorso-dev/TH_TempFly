package com.github.djkingcraftero89.TH_TempFly.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final Plugin plugin;
    private FileConfiguration messagesConfig;
    private final Map<String, String> messageCache = new HashMap<>();

    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        // Si no existe el archivo, copiarlo desde resources
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        messageCache.clear();
        
        // Precargar todos los mensajes en cache
        loadAllMessages();
    }

    private void loadAllMessages() {
        // Cargar mensajes de comandos
        loadCommandMessages();
        loadFlyMessages();
        loadEventMessages();
        loadPlaceholderMessages();
        loadConfigMessages();
    }

    private void loadCommandMessages() {
        String[] commandKeys = {
            "no-permission", "player-only", "player-not-found", "specify-player", "invalid-time"
        };
        
        for (String key : commandKeys) {
            cacheMessage("commands." + key);
        }
        
        // Mensajes específicos de tempfly
        String[] tempflyKeys = {
            "usage", "time-formats", "unknown-subcommand", "reload.success",
            "check.remaining", "check.no-time", "give.usage", "give.example", "give.success",
            "add.usage", "add.example", "add.success", "remove.usage", "remove.example", "remove.success"
        };
        
        for (String key : tempflyKeys) {
            cacheMessage("commands.tempfly." + key);
        }
    }

    private void loadFlyMessages() {
        String[] flyKeys = {
            "no-permission", "no-time", "contact-admin", "disabled", "enabled", "infinite-enabled"
        };
        
        for (String key : flyKeys) {
            cacheMessage("fly." + key);
        }
    }

    private void loadEventMessages() {
        String[] eventKeys = {
            "join.welcome-back", "join.infinite-fly", "quit.fly-disabled", "time-expired"
        };
        
        for (String key : eventKeys) {
            cacheMessage("events." + key);
        }
    }

    private void loadPlaceholderMessages() {
        String[] placeholderKeys = {
            "no-time", "infinite", "flying", "can-fly", "no-permission"
        };
        
        for (String key : placeholderKeys) {
            cacheMessage("placeholders." + key);
        }
    }

    private void loadConfigMessages() {
        String[] configKeys = {
            "database.initializing", "database.initialized", "database.error",
            "plugin.starting", "plugin.config-loaded", "plugin.commands-registered",
            "plugin.listeners-registered", "plugin.placeholderapi-registered",
            "plugin.placeholderapi-not-found", "plugin.enabled", "plugin.disabled"
        };
        
        for (String key : configKeys) {
            cacheMessage("config." + key);
        }
    }

    private void cacheMessage(String key) {
        String message = messagesConfig.getString(key);
        if (message != null) {
            messageCache.put(key, message);
        }
    }

    public String getMessage(String key) {
        String message = messageCache.get(key);
        if (message == null) {
            message = messagesConfig.getString(key, "&cMensaje no encontrado: " + key);
            messageCache.put(key, message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }

    public String getMessage(String key, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        return getMessage(key, placeholders);
    }

    public void reload() {
        loadMessages();
    }
}
