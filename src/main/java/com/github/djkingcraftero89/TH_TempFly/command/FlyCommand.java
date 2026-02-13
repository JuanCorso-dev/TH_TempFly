package com.github.djkingcraftero89.TH_TempFly.command;

import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import com.github.djkingcraftero89.TH_TempFly.integration.VulcanIntegration;
import com.github.djkingcraftero89.TH_TempFly.restriction.FlightRestrictionManager;
import com.github.djkingcraftero89.TH_TempFly.util.MessageManager;
import com.github.djkingcraftero89.TH_TempFly.util.TimeParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FlyCommand implements CommandExecutor, TabCompleter {
	private final FlyManager flyManager;
	private final MessageManager messageManager;
	private final FlightRestrictionManager restrictionManager;
	private final VulcanIntegration vulcanIntegration;

	public FlyCommand(FlyManager flyManager, MessageManager messageManager, FlightRestrictionManager restrictionManager,
			VulcanIntegration vulcanIntegration) {
		this.flyManager = flyManager;
		this.messageManager = messageManager;
		this.restrictionManager = restrictionManager;
		this.vulcanIntegration = vulcanIntegration;
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

		// Parse subcommand (default to "toggle" if no args)
		String subcommand = args.length > 0 ? args[0].toLowerCase() : "toggle";

		// Handle subcommands
		switch (subcommand) {
			case "toggle":
				return handleToggle(player);
			case "on":
				return handleEnable(player);
			case "off":
				return handleDisable(player);
			case "check":
				return handleCheck(player);
			default:
				player.sendMessage(messageManager.getMessage("fly.usage"));
				return true;
		}
	}

	private boolean handleToggle(Player player) {
		// Check flight restrictions
		FlightRestrictionManager.RestrictionResult restriction = restrictionManager.checkFlightAllowed(player);
		if (restriction.isBlocked()) {
			sendRestrictionMessage(player, restriction);
			return true;
		}

		long remainingTime = flyManager.getRemaining(player.getUniqueId());

		// Check for infinite fly permission
		if (player.hasPermission("thtempfly.fly.infinite")) {
			toggleInfiniteFly(player);
			return true;
		}

		if (remainingTime <= 0) {
			player.sendMessage(messageManager.getMessage("fly.no-time"));
			player.sendMessage(messageManager.getMessage("fly.contact-admin"));
			return true;
		}

		// Toggle flight for regular users
		toggleRegularFly(player, remainingTime);
		return true;
	}

	private boolean handleEnable(Player player) {
		// Check flight restrictions
		FlightRestrictionManager.RestrictionResult restriction = restrictionManager.checkFlightAllowed(player);
		if (restriction.isBlocked()) {
			sendRestrictionMessage(player, restriction);
			return true;
		}

		long remainingTime = flyManager.getRemaining(player.getUniqueId());

		// Check for infinite fly permission
		if (player.hasPermission("thtempfly.fly.infinite")) {
			if (!player.getAllowFlight()) {
				player.setAllowFlight(true);
				player.setFlying(true);

				if (vulcanIntegration != null) {
					vulcanIntegration.enableFlyExemption(player);
				}

				player.sendMessage(messageManager.getMessage("fly.infinite-enabled"));
			} else {
				player.sendMessage(messageManager.getMessage("fly.infinite-enabled"));
			}
			return true;
		}

		if (remainingTime <= 0) {
			player.sendMessage(messageManager.getMessage("fly.no-time"));
			player.sendMessage(messageManager.getMessage("fly.contact-admin"));
			return true;
		}

		if (!player.getAllowFlight()) {
			player.setAllowFlight(true);
			player.setFlying(true);

			if (vulcanIntegration != null) {
				vulcanIntegration.enableFlyExemption(player);
			}

			String formattedTime = TimeParser.formatSeconds(remainingTime);
			player.sendMessage(messageManager.getMessage("fly.enabled", "time", formattedTime));
		} else {
			String formattedTime = TimeParser.formatSeconds(remainingTime);
			player.sendMessage(messageManager.getMessage("fly.enabled", "time", formattedTime));
		}

		return true;
	}

	private boolean handleDisable(Player player) {
		long remainingTime = flyManager.getRemaining(player.getUniqueId());

		// Check for infinite fly permission
		if (player.hasPermission("thtempfly.fly.infinite")) {
			if (player.getAllowFlight()) {
				player.setFlying(false);
				player.setAllowFlight(false);

				if (vulcanIntegration != null) {
					vulcanIntegration.disableFlyExemption(player);
				}

				player.sendMessage(messageManager.getMessage("fly.disabled", "time", "∞"));
			} else {
				player.sendMessage(messageManager.getMessage("fly.disabled", "time", "∞"));
			}
			return true;
		}

		if (player.getAllowFlight()) {
			player.setFlying(false);
			player.setAllowFlight(false);

			if (vulcanIntegration != null) {
				vulcanIntegration.disableFlyExemption(player);
			}

			String formattedTime = TimeParser.formatSeconds(remainingTime);
			player.sendMessage(messageManager.getMessage("fly.disabled", "time", formattedTime));
		} else {
			String formattedTime = TimeParser.formatSeconds(remainingTime);
			player.sendMessage(messageManager.getMessage("fly.disabled", "time", formattedTime));
		}

		return true;
	}

	private boolean handleCheck(Player player) {
		long remainingTime = flyManager.getRemaining(player.getUniqueId());

		// Check for infinite fly permission
		if (player.hasPermission("thtempfly.fly.infinite")) {
			player.sendMessage(messageManager.getMessage("fly.status.infinite"));
			return true;
		}

		if (remainingTime <= 0) {
			player.sendMessage(messageManager.getMessage("fly.no-time"));
			player.sendMessage(messageManager.getMessage("fly.contact-admin"));
			return true;
		}

		String formattedTime = TimeParser.formatSeconds(remainingTime);
		if (player.getAllowFlight()) {
			player.sendMessage(messageManager.getMessage("fly.status.flying", "time", formattedTime));
		} else {
			player.sendMessage(messageManager.getMessage("fly.status.not-flying", "time", formattedTime));
		}

		return true;
	}

	private void toggleInfiniteFly(Player player) {
		if (player.getAllowFlight()) {
			player.setFlying(false);
			player.setAllowFlight(false);

			if (vulcanIntegration != null) {
				vulcanIntegration.disableFlyExemption(player);
			}

			player.sendMessage(messageManager.getMessage("fly.disabled", "time", "∞"));
		} else {
			player.setAllowFlight(true);
			player.setFlying(true);

			if (vulcanIntegration != null) {
				vulcanIntegration.enableFlyExemption(player);
			}

			player.sendMessage(messageManager.getMessage("fly.infinite-enabled"));
		}
	}

	private void toggleRegularFly(Player player, long remainingTime) {
		if (player.getAllowFlight()) {
			player.setFlying(false);
			player.setAllowFlight(false);

			if (vulcanIntegration != null) {
				vulcanIntegration.disableFlyExemption(player);
			}

			String formattedTime = TimeParser.formatSeconds(remainingTime);
			player.sendMessage(messageManager.getMessage("fly.disabled", "time", formattedTime));
		} else {
			player.setAllowFlight(true);
			player.setFlying(true);

			if (vulcanIntegration != null) {
				vulcanIntegration.enableFlyExemption(player);
			}

			String formattedTime = TimeParser.formatSeconds(remainingTime);
			player.sendMessage(messageManager.getMessage("fly.enabled", "time", formattedTime));
		}
	}

	private void sendRestrictionMessage(Player player, FlightRestrictionManager.RestrictionResult restriction) {
		if (restriction.getType() == FlightRestrictionManager.RestrictionType.WORLD) {
			player.sendMessage(messageManager.getMessage("fly.restrictions.world-blocked", "world",
					restriction.getRestrictionName()));
		} else if (restriction.getType() == FlightRestrictionManager.RestrictionType.REGION) {
			player.sendMessage(messageManager.getMessage("fly.restrictions.region-blocked", "region",
					restriction.getRestrictionName()));
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> completions = new ArrayList<>();

		// Only suggest for first argument
		if (args.length == 1) {
			List<String> subcommands = Arrays.asList("toggle", "on", "off", "check");
			String input = args[0].toLowerCase();

			// Filter subcommands based on input
			completions = subcommands.stream()
					.filter(sub -> sub.startsWith(input))
					.collect(Collectors.toList());
		}

		return completions;
	}
}
