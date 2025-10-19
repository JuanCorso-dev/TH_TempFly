package com.github.djkingcraftero89.TH_TempFly.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private final Plugin plugin;
    private final String currentVersion;
    private final String githubRepo;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(Plugin plugin, String githubOwner, String githubRepo) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.githubRepo = "https://github.com/" + githubOwner + "/" + githubRepo;
    }

    /**
     * Check for updates asynchronously
     * @return CompletableFuture with update availability
     */
    public CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/JuanCorso-dev/TH_TempFly/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse JSON response manually to avoid external dependencies
                    String json = response.toString();
                    int tagIndex = json.indexOf("\"tag_name\"");
                    if (tagIndex != -1) {
                        int start = json.indexOf("\"", tagIndex + 11) + 1;
                        int end = json.indexOf("\"", start);
                        latestVersion = json.substring(start, end).replace("v", "");
                        
                        updateAvailable = isNewerVersion(latestVersion, currentVersion);
                        return updateAvailable;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not check for updates: " + e.getMessage());
            }
            return false;
        });
    }

    /**
     * Compare two version strings
     * @param latest Latest version from GitHub
     * @param current Current plugin version
     * @return true if latest is newer than current
     */
    private boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        
        int maxLength = Math.max(latestParts.length, currentParts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
            
            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }
        
        return false;
    }

    /**
     * Parse version part to integer, ignoring non-numeric suffixes
     */
    private int parseVersionPart(String part) {
        try {
            // Remove any non-numeric suffix (e.g., "1-SNAPSHOT" -> "1")
            return Integer.parseInt(part.replaceAll("[^0-9].*", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Log update information to console
     */
    public void notifyConsole(MessageManager messageManager) {
        if (updateAvailable) {
            plugin.getLogger().info("========================================");
            plugin.getLogger().info("A new version is available!");
            plugin.getLogger().info("Current: v" + currentVersion + " | Latest: v" + latestVersion);
            plugin.getLogger().info("Download: " + githubRepo + "/releases/latest");
            plugin.getLogger().info("========================================");
        }
    }

    /**
     * Notify player about available update
     */
    public void notifyPlayer(Player player, MessageManager messageManager) {
        if (updateAvailable && player.hasPermission("thtempfly.admin")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(messageManager.getMessage("update.available", 
                    "current", currentVersion,
                    "latest", latestVersion));
                player.sendMessage(messageManager.getMessage("update.download-url", 
                    "url", githubRepo + "/releases/latest"));
            }, 60L); // Wait 3 seconds after join
        }
    }

    /**
     * Send version info to command sender
     */
    public String getVersionInfo() {
        if (updateAvailable) {
            return "§eTH_TempFly §7v" + currentVersion + " §8| §cUpdate available: §av" + latestVersion;
        } else {
            return "§eTH_TempFly §7v" + currentVersion + " §8| §aYou are up to date!";
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }
}

