package com.github.djkingcraftero89.TH_TempFly.command;

import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import com.github.djkingcraftero89.TH_TempFly.storage.DataStore;
import com.github.djkingcraftero89.TH_TempFly.storage.DatabaseMigrator;
import com.github.djkingcraftero89.TH_TempFly.util.MessageManager;
import com.github.djkingcraftero89.TH_TempFly.util.TimeParser;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TempFlyCommand implements CommandExecutor, TabCompleter {
	private final Plugin plugin;
	private final FlyManager flyManager;
	private final DataStore dataStore;
	private final MessageManager messageManager;
	private static boolean debugMode = false;

	public TempFlyCommand(Plugin plugin, FlyManager flyManager, DataStore dataStore, MessageManager messageManager) {
		this.plugin = plugin;
		this.flyManager = flyManager;
		this.dataStore = dataStore;
		this.messageManager = messageManager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		String cmd = command.getName().toLowerCase();
		// Admin command: /atempfly -> only "debug"
		if ("atempfly".equals(cmd)) {
			if (args.length == 0) {
				sender.sendMessage("Usage: /atempfly debug <true|false>");
				return true;
			}
			String sub = args[0].toLowerCase();
			if ("debug".equals(sub)) {
				if (!sender.hasPermission("thtempfly.admin")) {
					sender.sendMessage(messageManager.getMessage("commands.no-permission"));
					return true;
				}
				return handleDebug(sender, args);
			}
			sender.sendMessage("Invalid subcommand. Use: /atempfly debug <true|false>");
			return true;
		}

		// Normal command: /tempfly (without "debug")
		if (args.length == 0) {
			sender.sendMessage(messageManager.getMessage("commands.tempfly.usage"));
			sender.sendMessage(messageManager.getMessage("commands.tempfly.time-formats"));
			return true;
		}
		String sub = args[0].toLowerCase();
		switch (sub) {
			case "reload":
				if (!sender.hasPermission("thtempfly.admin")) {
					sender.sendMessage(messageManager.getMessage("commands.no-permission"));
					return true;
				}
				plugin.reloadConfig();
				messageManager.reload();
				
				// Reload flight restrictions
				if (plugin instanceof com.github.djkingcraftero89.TH_TempFly.TH_TempFly) {
					com.github.djkingcraftero89.TH_TempFly.TH_TempFly tempFlyPlugin = (com.github.djkingcraftero89.TH_TempFly.TH_TempFly) plugin;
					if (tempFlyPlugin.getRestrictionManager() != null) {
						List<String> blockedWorlds = plugin.getConfig().getStringList("fly.restrictions.blocked-worlds");
						List<String> blockedRegions = plugin.getConfig().getStringList("fly.restrictions.blocked-regions");
						tempFlyPlugin.getRestrictionManager().reload(blockedWorlds, blockedRegions);
					}
				}
				
				sender.sendMessage(messageManager.getMessage("commands.tempfly.reload.success"));
				return true;
			case "check":
				UUID checkId = resolveTarget(sender, args, 1);
				if (checkId == null) return true;
				long rem = flyManager.getRemaining(checkId);
				String formattedTime = TimeParser.formatSeconds(rem);
				sender.sendMessage(messageManager.getMessage("commands.tempfly.check.remaining", "time", formattedTime));
				return true;
			case "give":
				return handleSet(sender, args, true);
			case "add":
				return handleAdd(sender, args, true);
			case "remove":
				return handleAdd(sender, args, false);
			case "version":
				if (plugin instanceof com.github.djkingcraftero89.TH_TempFly.TH_TempFly) {
					com.github.djkingcraftero89.TH_TempFly.TH_TempFly tempFlyPlugin = (com.github.djkingcraftero89.TH_TempFly.TH_TempFly) plugin;
					if (tempFlyPlugin.getUpdateChecker() != null) {
						sender.sendMessage(tempFlyPlugin.getUpdateChecker().getVersionInfo());
					} else {
						sender.sendMessage("§eTH_TempFly §7v" + plugin.getDescription().getVersion());
					}
				}
				return true;
			case "migrate":
				if (!sender.hasPermission("thtempfly.admin")) {
					sender.sendMessage(messageManager.getMessage("commands.no-permission"));
					return true;
				}
				return handleMigrate(sender);
			default:
				sender.sendMessage(messageManager.getMessage("commands.tempfly.unknown-subcommand"));
				return true;
		}
	}

	private boolean handleSet(CommandSender sender, String[] args, boolean apply) {
		if (!sender.hasPermission("thtempfly.admin")) {
			sender.sendMessage(messageManager.getMessage("commands.no-permission"));
			return true;
		}
		if (args.length < 3) {
			sender.sendMessage(messageManager.getMessage("commands.tempfly.give.usage"));
			sender.sendMessage(messageManager.getMessage("commands.tempfly.give.example"));
			return true;
		}
		UUID id = resolveTarget(sender, args, 1);
		if (id == null) return true;
		long seconds = parseTime(sender, args[2]);
		if (seconds <= 0) {
			sender.sendMessage(messageManager.getMessage("commands.invalid-time"));
			return true;
		}
		flyManager.setRemaining(id, seconds, apply);
		String formattedTime = TimeParser.formatSeconds(seconds);
		sender.sendMessage(messageManager.getMessage("commands.tempfly.give.success", "time", formattedTime));
		return true;
	}

	private boolean handleAdd(CommandSender sender, String[] args, boolean add) {
		if (!sender.hasPermission("thtempfly.admin")) {
			sender.sendMessage(messageManager.getMessage("commands.no-permission"));
			return true;
		}
		if (args.length < 3) {
			String usageKey = add ? "commands.tempfly.add.usage" : "commands.tempfly.remove.usage";
			String exampleKey = add ? "commands.tempfly.add.example" : "commands.tempfly.remove.example";
			sender.sendMessage(messageManager.getMessage(usageKey));
			sender.sendMessage(messageManager.getMessage(exampleKey));
			return true;
		}
		UUID id = resolveTarget(sender, args, 1);
		if (id == null) return true;
		long seconds = parseTime(sender, args[2]);
		if (seconds <= 0) {
			sender.sendMessage(messageManager.getMessage("commands.invalid-time"));
			return true;
		}
		if (add) flyManager.addSeconds(id, seconds, true);
		else flyManager.addSeconds(id, -seconds, true);
		String formattedTime = TimeParser.formatSeconds(seconds);
		String change = (add ? "+" : "-") + formattedTime;
		String successKey = add ? "commands.tempfly.add.success" : "commands.tempfly.remove.success";
		sender.sendMessage(messageManager.getMessage(successKey, "change", change));
		return true;
	}

	private long parseTime(CommandSender sender, String raw) {
		return TimeParser.parseTimeToSeconds(raw);
	}

	private UUID resolveTarget(CommandSender sender, String[] args, int idx) {
		if (args.length <= idx) {
			sender.sendMessage(messageManager.getMessage("commands.specify-player"));
			return null;
		}
		String name = args[idx];
		Player p = Bukkit.getPlayerExact(name);
		if (p != null) return p.getUniqueId();
		@SuppressWarnings("deprecation") OfflinePlayer off = Bukkit.getOfflinePlayer(name);
		if (off != null && off.getUniqueId() != null) return off.getUniqueId();
		sender.sendMessage(messageManager.getMessage("commands.player-not-found", "player", name));
		return null;
	}

	private boolean handleDebug(CommandSender sender, String[] args) {
		if (args.length < 2) {
			String status = debugMode ? "enabled" : "disabled";
			sender.sendMessage("Debug mode is currently " + status + ".");
			sender.sendMessage("Usage: /atempfly debug <true|false>");
			return true;
		}
		
		String value = args[1].toLowerCase();
		if ("true".equals(value) || "on".equals(value) || "1".equals(value)) {
			debugMode = true;
			sender.sendMessage("Debug mode enabled. Detailed logs will be shown in console.");
		} else if ("false".equals(value) || "off".equals(value) || "0".equals(value)) {
			debugMode = false;
			sender.sendMessage("Debug mode disabled.");
		} else {
			sender.sendMessage("Invalid value. Use: true, false, on, off, 1, or 0");
			return true;
		}
		return true;
	}

	public static boolean isDebugMode() {
		return debugMode;
	}

	private boolean handleMigrate(CommandSender sender) {
		if (!(plugin instanceof com.github.djkingcraftero89.TH_TempFly.TH_TempFly)) {
			sender.sendMessage(messageManager.getMessage("commands.tempfly.migrate.error.not-plugin"));
			return true;
		}
		
		if (!(plugin instanceof org.bukkit.plugin.java.JavaPlugin)) {
			sender.sendMessage(messageManager.getMessage("commands.tempfly.migrate.error.not-plugin"));
			return true;
		}
		
		com.github.djkingcraftero89.TH_TempFly.TH_TempFly tempFlyPlugin = (com.github.djkingcraftero89.TH_TempFly.TH_TempFly) plugin;
		org.bukkit.plugin.java.JavaPlugin javaPlugin = (org.bukkit.plugin.java.JavaPlugin) plugin;
		org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
		
		// Check if currently using MySQL
		boolean isMysql = "MYSQL".equalsIgnoreCase(cfg.getString("storage.type", "SQLITE"));
		if (!isMysql) {
			sender.sendMessage(messageManager.getMessage("commands.tempfly.migrate.error.not-mysql"));
			return true;
		}
		
		// Check if SQLite file exists
		String sqliteFile = cfg.getString("storage.sqlite.file", "data.db");
		java.io.File dbFile = new java.io.File(plugin.getDataFolder(), sqliteFile);
		if (!dbFile.exists()) {
			sender.sendMessage(messageManager.getMessage("commands.tempfly.migrate.error.no-sqlite", "file", dbFile.getAbsolutePath()));
			return true;
		}
		
		sender.sendMessage(messageManager.getMessage("commands.tempfly.migrate.starting"));
		
		// Run migration asynchronously
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					// Get MySQL datasource from plugin
					HikariDataSource mysqlDataSource = tempFlyPlugin.getMySQLDataSource();
					if (mysqlDataSource == null || mysqlDataSource.isClosed()) {
						sender.sendMessage(messageManager.getMessage("commands.tempfly.migrate.error.no-datasource"));
						return;
					}
					
					DatabaseMigrator migrator = new DatabaseMigrator(javaPlugin);
					DatabaseMigrator.MigrationResult result = migrator.migrateFromSQLiteToMySQL(mysqlDataSource);
					
					// Send results on main thread
					new BukkitRunnable() {
						@Override
						public void run() {
							if (result.hasError()) {
								sender.sendMessage(messageManager.getMessage("commands.tempfly.migrate.error.general", "error", result.getError()));
							} else {
								Map<String, String> placeholders = new HashMap<>();
								placeholders.put("total", String.valueOf(result.getTotalRecords()));
								placeholders.put("migrated", String.valueOf(result.getMigrated()));
								placeholders.put("skipped", String.valueOf(result.getSkipped()));
								sender.sendMessage(messageManager.getMessage("commands.tempfly.migrate.success", placeholders));
							}
						}
					}.runTask(plugin);
					
				} catch (Exception e) {
					String errorMsg = e.getMessage();
					new BukkitRunnable() {
						@Override
						public void run() {
							sender.sendMessage(messageManager.getMessage("commands.tempfly.migrate.error.general", "error", errorMsg != null ? errorMsg : e.getClass().getSimpleName()));
							plugin.getLogger().severe("Error during migration: " + e.getMessage());
							e.printStackTrace();
						}
					}.runTask(plugin);
				}
			}
		}.runTaskAsynchronously(plugin);
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		String cmd = command.getName().toLowerCase();
		// Tab completion for /atempfly (only debug)
		if ("atempfly".equals(cmd)) {
			if (args.length == 1) {
				return java.util.Collections.singletonList("debug");
			}
			if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
				String input = args[1].toLowerCase();
				List<String> opts = Arrays.asList("true", "false", "on", "off", "1", "0");
				List<String> matches = new ArrayList<>();
				for (String o : opts) if (o.startsWith(input)) matches.add(o);
				return matches;
			}
			return new ArrayList<>();
		}

		// Tab completion for /tempfly (without debug)
		if (args.length == 1) {
			List<String> subcommands = new ArrayList<>();
			subcommands.add("check");
			subcommands.add("give");
			subcommands.add("add");
			subcommands.add("remove");
			subcommands.add("reload");
			subcommands.add("version");
			subcommands.add("migrate");
			
			String input = args[0].toLowerCase();
			List<String> matches = new ArrayList<>();
			for (String sub : subcommands) {
				if (sub.startsWith(input)) {
					matches.add(sub);
				}
			}
			return matches;
		}
		
		if (args.length == 2) {
			String subcommand = args[0].toLowerCase();
			switch (subcommand) {
				case "check":
				case "give":
				case "add":
				case "remove":
					// Autocomplete player names
					List<String> playerNames = new ArrayList<>();
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
							playerNames.add(player.getName());
						}
					}
					return playerNames;
			}
		}
		
		if (args.length == 3) {
			String subcommand = args[0].toLowerCase();
			if ("give".equals(subcommand) || "add".equals(subcommand) || "remove".equals(subcommand)) {
				// Suggest common time formats
				List<String> timeFormats = Arrays.asList("1m", "5m", "10m", "30m", "1h", "2h", "5h", "1d");
				String input = args[2].toLowerCase();
				List<String> matches = new ArrayList<>();
				for (String format : timeFormats) {
					if (format.startsWith(input)) {
						matches.add(format);
					}
				}
				return matches;
			}
		}
		
		return new ArrayList<>();
	}
}
