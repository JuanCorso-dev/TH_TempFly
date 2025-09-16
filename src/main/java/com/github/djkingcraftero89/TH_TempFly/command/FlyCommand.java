package com.github.djkingcraftero89.TH_TempFly.command;

import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import com.github.djkingcraftero89.TH_TempFly.util.MessageManager;
import com.github.djkingcraftero89.TH_TempFly.util.TimeParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {
	private final FlyManager flyManager;
	private final MessageManager messageManager;

	public FlyCommand(FlyManager flyManager, MessageManager messageManager) {
		this.flyManager = flyManager;
		this.messageManager = messageManager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(messageManager.getMessage("commands.player-only"));
			return true;
		}

		Player player = (Player) sender;
		
		if (!player.hasPermission("thtempfly.fly.use")) {
			player.sendMessage(messageManager.getMessage("fly.no-permission"));
			return true;
		}

		long remainingTime = flyManager.getRemaining(player.getUniqueId());
		
		// Check for infinite fly permission
		if (player.hasPermission("thtempfly.fly.infinite")) {
			// Toggle flight for infinite fly users
			if (player.getAllowFlight()) {
				player.setFlying(false);
				player.setAllowFlight(false);
				player.sendMessage(messageManager.getMessage("fly.disabled"));
			} else {
				player.setAllowFlight(true);
				player.setFlying(true);
				player.sendMessage(messageManager.getMessage("fly.infinite-enabled"));
			}
			return true;
		}
		
		if (remainingTime <= 0) {
			player.sendMessage(messageManager.getMessage("fly.no-time"));
			player.sendMessage(messageManager.getMessage("fly.contact-admin"));
			return true;
		}

		// Toggle flight for regular users
		if (player.getAllowFlight()) {
			player.setFlying(false);
			player.setAllowFlight(false);
			player.sendMessage(messageManager.getMessage("fly.disabled"));
		} else {
			player.setAllowFlight(true);
			player.setFlying(true);
			String formattedTime = TimeParser.formatSeconds(remainingTime);
			player.sendMessage(messageManager.getMessage("fly.enabled", "time", formattedTime));
		}

		return true;
	}
}
