package com.github.djkingcraftero89.TH_TempFly.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * One step that brings a user's YAML file from version {@code N - 1} to version {@code N}.
 *
 * <p>Most releases do not need one: keys added to the bundled file are copied into outdated
 * files automatically, and user values are kept. A migration is only needed when an existing
 * key changes in a way a plain merge cannot express, for example a rename, a move to another
 * section, a value transformation, or a key that changes from a plain value to a section.
 *
 * <p>To add one:
 * <ol>
 *   <li>Change the bundled resource to its new layout and raise its {@code config-version}
 *       to {@code N}.</li>
 *   <li>Register the step for version {@code N} where the updater for that file is created,
 *       for example after renaming {@code fly.tick-interval-ticks} to {@code fly.tick-interval}:
 * <pre>{@code
 * new ConfigUpdater(this, "config.yml")
 *         .addMigration(2, config -> {
 *             if (config.contains("fly.tick-interval-ticks")) {
 *                 config.set("fly.tick-interval", config.get("fly.tick-interval-ticks"));
 *                 config.set("fly.tick-interval-ticks", null);
 *             }
 *         })
 *         .update();
 * }</pre>
 *   </li>
 * </ol>
 *
 * <p>Without the step above, the old key would be kept as an unknown user key and the new key
 * would be added with its default value, so the user's setting would silently stop applying.
 */
@FunctionalInterface
public interface ConfigMigration {

	/**
	 * Rewrites the user's configuration in place, before it is merged into the bundled template.
	 *
	 * @param config the user's configuration as loaded from disk
	 */
	void migrate(ConfigurationSection config);
}
