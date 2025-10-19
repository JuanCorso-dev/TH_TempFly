package com.github.djkingcraftero89.TH_TempFly.command;

import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import com.github.djkingcraftero89.TH_TempFly.restriction.FlightRestrictionManager;
import com.github.djkingcraftero89.TH_TempFly.util.MessageManager;
import com.github.djkingcraftero89.TH_TempFly.util.TimeParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {
	private final FlyManager flyManager;
	private final MessageManager messageManager;
	private final FlightRestrictionManager restrictionManager;

	public FlyCommand(FlyManager flyManager, MessageManager messageManager, FlightRestrictionManager restrictionManager) {
		this.flyManager = flyManager;
		this.messageManager = messageManager;
		this.restrictionManager = restrictionManager;
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

		// Check flight restrictions
		FlightRestrictionManager.RestrictionResult restriction = restrictionManager.checkFlightAllowed(player);
		if (restriction.isBlocked()) {
			if (restriction.getType() == FlightRestrictionManager.RestrictionType.WORLD) {
				player.sendMessage(messageManager.getMessage("fly.restrictions.world-blocked", "world", restriction.getRestrictionName()));
			} else if (restriction.getType() == FlightRestrictionManager.RestrictionType.REGION) {
				player.sendMessage(messageManager.getMessage("fly.restrictions.region-blocked", "region", restriction.getRestrictionName()));
			}
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
