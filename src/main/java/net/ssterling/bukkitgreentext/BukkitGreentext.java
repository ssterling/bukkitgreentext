/*
 * BukkitGreentext - add 4chan-style quoting to Minecraft server chat
 *
 * Copyright 2018, 2021 Seth Price
 * All rights reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package net.ssterling.bukkitgreentext;

import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.logging.Level;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.scheduler.BukkitScheduler;
import net.md_5.bungee.api.ChatColor;
import org.bstats.bukkit.Metrics;
import net.ssterling.updatechecker.UpdateChecker;

/**
 * @author	Seth Price <ssterling AT firemail DOT cc>
 * @version	3.0
 * @since	1.0
 */
public class BukkitGreentext extends JavaPlugin
{
	/**
	 * The configuration file as a variable.
	 *
	 * @see org.bukkit.configuration.file.FileConfiguration
	 */
	public static FileConfiguration config;

	/**
	 * A HashMap of whether Greentext features are enabled for a given player.
	 */
	private static HashMap<UUID, Boolean> enabled_for_player;

	/**
	 * Whether Greentext is enabled for any player.
	 */
	private static boolean enabled_by_default;

	/**
	 * Spigot project ID (for update checker).
	 */
	private static final String PROJECT_ID = "55295";

	/**
	 * bStats plugin ID (for metrics).
	 */
	private static final int BSTATS_ID = 2814;

	/**
	 * The file pointer for the persistent hashmap.
	 */
	private static File persistent_hashmap_file;

	/**
	 * The persistent hashmap file as a Bukkit configuration file.
	 *
	 * @see org.bukkit.configuration.file.FileConfiguration
	 */
	private static FileConfiguration persistent_hashmap;

	private static PluginManager pm;
	private static PluginDescriptionFile pdf;
	private static Metrics metrics;

	@Override
	public void onEnable()
	{
		pm = getServer().getPluginManager();
		pdf = this.getDescription();
		metrics = new Metrics(this, BSTATS_ID);

		/* Save the default configuration file if not present; else, use the file */
		getLogger().config("Loading configuration...");
		try {
			this.saveDefaultConfig();
			config = this.getConfig();
		} catch (Throwable ex) {
			getLogger().warning("Failed to load/save configuration file.  Using default configuration.");
			ex.printStackTrace();
		}

		getLogger().config("Checking for discrepancies between default config and user config...");
		try {
			/* Load default `config.yml' from inside JAR */
			getLogger().finest("Loading default `config.yml'...");
			InputStream default_config_inputstream = getResource("config.yml");
			InputStreamReader default_config_reader = new InputStreamReader(default_config_inputstream);
			FileConfiguration default_config = YamlConfiguration.loadConfiguration(default_config_reader);

			/* Add new config keys (i.e. added to the program since
			 * the config file was last generated/updated) */
			for (String key : default_config.getConfigurationSection("").getKeys(false)) {
				if (!(config.isSet(key))) {
					getLogger().config("Adding new key `" + key + "' to config file");
					config.set(key, default_config.get(key));
				}
			}

			/* Remove old config keys that no longer exist */
			for (String key : config.getConfigurationSection("").getKeys(false)) {
				if (!(default_config.isSet(key))) {
					getLogger().config("Removing old key `" + key + "' from config file");
					config.set(key, null);
				}
			}

			getLogger().fine("Saving new config to `config.yml'...");
			saveConfig();
		} catch (Throwable ex) {
			getLogger().warning("Failed to update configuration file.");
			ex.printStackTrace();
		}

		getLogger().finest("Initialising player hashmap...");
		enabled_for_player = new HashMap<UUID, Boolean>();

		getLogger().info("Loading persistent hashmap from disk...");
		try {
			persistent_hashmap_file = new File(getDataFolder(), "playermap.yml");

			/* Create the hashmap file if it doesn't exist */
			if (!(persistent_hashmap_file.exists())) {
				getLogger().fine("`playermap.yml' doesn't exist; creating");
				saveResource("playermap.yml", false);
			}

			persistent_hashmap = new YamlConfiguration();
			persistent_hashmap.load(persistent_hashmap_file);

			/* Assign all the values to the in-memory HashMap */
			for (String uuid : persistent_hashmap.getKeys(true)) {
				/* TODO: overload method with UUID instead of Player object? */
				enabled_for_player.put(UUID.fromString(uuid), persistent_hashmap.getBoolean(uuid));
			}
		} catch (Throwable ex) {
			getLogger().warning("Failed to load persistent hashmap from disk.  All users will default to `" + String.valueOf(enabled_by_default) + "' (value of `enabled_by_default').");
			ex.printStackTrace();
		}

		getLogger().finest("Registering chat listener...");
		try {
			pm.registerEvents(new BgtChatListener(this), this);
		} catch (Throwable ex) {
			/* There's no use in having the plugin if it can't listen to chat */
			getLogger().warning("Failed to register chat listener; shutting down");
			ex.printStackTrace();
			pm.disablePlugin(this);
		}

		getLogger().finest("Registering command executor...");
		try {
			getCommand("greentext").setExecutor(new BgtCommandExecutor(this));
		} catch (Throwable ex) {
			/* Not having commands still provides limited functionality, however */
			getLogger().warning("Failed to register command executor; proceed with caution");
			ex.printStackTrace();
		}

		/* Read from config whether greentext must be manually enabled per-player */
		enabled_by_default = config.getBoolean("enabled-by-default");

		if (config.getBoolean("use-hex-colors") && VersionUtil.compareVersions(getServer().getVersion(), "1.16")) {
			getLogger().warning("1.16-style hex colors are enabled in config, but server is still running " + getServer().getVersion() + "; reverting to old-style formatting codes");
			config.set("use-hex-colors", false); /* Intentionally does not save to file */
		}

		/* Asynchronously check for updates on the Spigot resource page */
		if (config.getBoolean("check-for-updates")) {
			String current_version = pdf.getVersion();
			UpdateChecker update_checker = new UpdateChecker(PROJECT_ID, current_version);

			getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				@Override
				public void run()
				{
					getLogger().fine("Checking for updates...");
					try {
						String new_version = update_checker.check();
						if (new_version != null) {
							getLogger().info("New version " + new_version + " found (currently on " + current_version + "; download at: <https://www.spigotmc.org/resources/" + PROJECT_ID + "/>");
						} else {
							getLogger().info("No new version found");
						}
					} catch (Throwable ex) {
						/* Don't spam a warning + stack trace on something so trivial
						 * (especially if server isn't connected to the Internet) */
						getLogger().info("Failed to check for updates");
					}
				}
			}, 200L, 1728000L /* wait 10 seconds, then repeat every 24 hours */);
		}

		getLogger().info("Successfully initialised " + pdf.getName() + " v" + pdf.getVersion());
	}

	@Override
	public void onDisable()
	{
		getLogger().info("Saving persistent hashmap to disk...");
		
		for (UUID uuid : enabled_for_player.keySet()) {
			persistent_hashmap.set(uuid.toString(), enabled_for_player.get(uuid));
		}

		try {
			persistent_hashmap.save(persistent_hashmap_file);
		} catch (Throwable ex) {
			getLogger().warning("Failed to save persistent hashmap to disk.");
			ex.printStackTrace();
		}
	}

	/**
	 * Sets whether Greentext features are enabled for a given player.
	 *
	 * @param player	Player object for whom features shall be enabled or disabled.
	 * @param value		Enable (true) or disable (false) features.
	 * @since 1.2
	 */
	public void playerSetEnabled(final Player player, final boolean value)
	{
		String enabled_disabled;
		if (value) {
			enabled_disabled = "enabled";
		} else {
			enabled_disabled = "disabled";
		}
		getLogger().info("Set greentext " + enabled_disabled + " for player " + player.getName());
		enabled_for_player.put(player.getUniqueId(), value);
	}

	/**
	 * Gets whether Greentext features are enabled for a given player.
	 *
	 * @param player	Player object for whom enable/disable status shall be queried.
	 * @return true if enabled, false if disabled or not found
	 * @since 1.2
	 */
	public boolean playerIsEnabled(final Player player)
	{
		if (enabled_for_player.containsKey(player.getUniqueId())) {
			return enabled_for_player.get(player.getUniqueId());
		} else {
			/* Use global setting if not explicitly defined for player */
			return enabled_by_default;
		}
	}

	/**
	 * Sets whether Greentext features are enabled globally.
	 *
	 * @param value     Enable (true) or disable (false) features.
	 * @since 1.2
	 */
	public void globalSetEnabled(final boolean value)
	{
		String enabled_disabled;
		if (value) {
			enabled_disabled = "enabled";
		} else {
			enabled_disabled = "disabled";
		}
		getLogger().info("Set greentext " + enabled_disabled + " for all players");
		enabled_by_default = value;
	}

	/**
	 * Gets whether Greentext features are enabled globally.
	 *
	 * @return true if enabled, false if disabled
	 * @since 1.2
	 */
	public boolean globalIsEnabled()
	{
		return enabled_by_default;
	}

	/**
	 * Checks whether a given string is valid Greentext.
	 *
	 * @param message	string to check for validity
	 * @return true if valid Greentext, false otherwise
	 * @since 1.4
	 */
	public boolean isValidGreentext(final String message)
	{
		getLogger().finest("Message passed to `isValidGreentext()'; checking whether it starts with `>': `" + message + "'");
		if (message.startsWith(">")) {
			getLogger().fine("Message begins with `>'; checking whether it's greentext: `" + message + "'");

			/* Make sure the message isn't just a `>' */
			if (message.length() == 1) {
				getLogger().fine("Aforementioned message contains only a `>'; ignoring");
				return false;
			}

			/* Cycles through exceptions in `config.yml'.
			 * For example, if `:' is listed as an exception,
			 * any message starting with `>:' isn't greenified,
			 * such as in emoticons, e.g. `>:('. */
			for (String exception : config.getStringList("greentext-exceptions")) {
				if (message.startsWith(">" + exception)) {
					getLogger().fine("Aforementioned message begins with exception `>" + exception + "'; ignoring");
					return false;
				}
			}
		} else {
			return false;
		}
		getLogger().fine("Message appears to be greentext");
		return true;
	}

	/**
	 * Checks whether a given string is valid Orangetext.
	 *
	 * @param message	string to check for validity
	 * @return true if valid Orangetext, false otherwise
	 * @since 1.4
	 */
	public boolean isValidOrangetext(final String message)
	{
		getLogger().finest("Message passed to `isValidOrangetext()'; checking whether it ends with `<': `" + message + "'");
		if (message.endsWith("<")) {
			getLogger().fine("Message ends with `<'; checking whether it's orangetext: `" + message + "'");
			/* Similarly, make sure the message isn't just a `<' */
			if (message.length() == 1) {
				getLogger().fine("Aforementioned message contains only a `<'; ignoring");
				return false;
			}

			/* Same concept as in isValidGreentext() */
			for (String exception : config.getStringList("orangetext-exceptions")) {
				if (message.endsWith(exception + "<")) {
					getLogger().fine("Aforementioned message ends with exception `" + exception + "<'; ignoring");
					return false;
				}
			}
		} else {
			return false;
		}
		getLogger().fine("Message appears to be orangetext");
		return true;
	}

	/**
	 * Converts the message of a given event to Greentext.
	 *
	 * @param e         ChatEvent in which the message shall be replaced
	 * @since 1.2
	 */
	public void eventMakeGreentext(AsyncPlayerChatEvent e)
	{
		/* Maven screams at me if I don't initialise this */
		String message = "";
		try {
			message = e.getMessage();
		} catch (Throwable ex) {
			getLogger().warning("Failed to get message from ChatEvent");
			ex.printStackTrace();
			return;
		}

		final String color_code = config.getBoolean("use-hex-colors")
			? ChatColor.of("#789922").toString()
			: ChatColor.GREEN.toString();

		getLogger().finest("ChatEvent passed to `eventMakeGreentext()'; attempting to make message green: `" + message + "'");
		try {
			e.setMessage(color_code + message);
		} catch (Throwable ex) {
			getLogger().warning("Failed to make ChatEvent greentext: message `" + message + "'");
			ex.printStackTrace();
		}
	}

	/**
	 * Converts the message of a given event to Orangetext.
	 *
	 * @param e         ChatEvent in which the message shall be replaced
	 * @since 1.4
	 */
	public void eventMakeOrangetext(AsyncPlayerChatEvent e)
	{
		/* Maven screams at me if I don't initialise this */
		String message = "";
		try {
			message = e.getMessage();
		} catch (Throwable ex) {
			getLogger().warning("Failed to get message from ChatEvent");
			ex.printStackTrace();
			return;
		}

		final String color_code = config.getBoolean("use-hex-colors")
			? ChatColor.of("#ff682d").toString()
			: ChatColor.GOLD.toString();

		getLogger().finest("ChatEvent passed to `eventMakeOrangetext()'; attempting to make message gold (orange): `" + message + "'");
		try {
			e.setMessage(color_code + message);
		} catch (Throwable ex) {
			getLogger().warning("Failed to make ChatEvent orangetext: message `" + message + "'");
			ex.printStackTrace();
		}
	}
}
