package com.github.djkingcraftero89.TH_TempFly.command;

import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import com.github.djkingcraftero89.TH_TempFly.storage.DataStore;
import com.github.djkingcraftero89.TH_TempFly.util.MessageManager;
import com.github.djkingcraftero89.TH_TempFly.util.TimeParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class TempFlyCommand implements CommandExecutor {
	private final Plugin plugin;
	private final FlyManager flyManager;
	private final DataStore dataStore;
	private final MessageManager messageManager;

	public TempFlyCommand(Plugin plugin, FlyManager flyManager, DataStore dataStore, MessageManager messageManager) {
		this.plugin = plugin;
		this.flyManager = flyManager;
		this.dataStore = dataStore;
		this.messageManager = messageManager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
}
