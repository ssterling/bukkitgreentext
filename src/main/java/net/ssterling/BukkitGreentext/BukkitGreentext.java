/*
 * BukkitGreentext - add 4chan-style quoting to Minecraft server chat
 *
 * Copyright 2018, 2019 Seth Price
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
package net.ssterling.BukkitGreentext;

import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.PluginLogger;
import org.bstats.bukkit.Metrics;

/**
 * @author	Seth Price <ssterling AT firemail DOT cc>
 * @version	2.2
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
	 * Whether debug messages are enabled.
	 */
	private static boolean is_debug;

	/**
	 * Whether to check for updates on startup.
	 */
	private static boolean check_for_updates;

	/**
	 * Spigot project ID (for update checker).
	 */
	private static final int PROJECT_ID = 55295;

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
		metrics = new Metrics(this);

		/* Save the default configuration file if not present; else, use the file */
		getLogger().config("Loading configuration...");
		try {
			this.saveDefaultConfig();
			config = this.getConfig();
		} catch (Throwable ex) {
			getLogger().warning("Failed to load/save configuration file.  Using default configuration.");
			ex.printStackTrace();
		}

		/* Before anything else, check whether we want the debug messages */
		is_debug = config.getBoolean("debug");
		if (is_debug) {
			getLogger().info("Entering debug mode (set in config.yml)");
			/* TODO: change logger level */
		}

		/* ...actually, go back and write the `debug' key to the config file
		 * if it doesn't exist (was added in version 2.0) */
		if (!(config.isSet("debug"))) {
			getLogger().config("Updating configuration file with new `debug' key...");
			/* If it didn't exist, `is_debug' holds the default value
			 * since `saveDefaultConfig()' was called */
			config.set("debug", is_debug);
			try {
				this.saveConfig();
			} catch (Throwable ex) {
				getLogger().warning("Failed to update configuration file.");
				ex.printStackTrace();
			}
		}

		/* Likewise, add `check-for-updates' if nonexistent (added in version 2.2) */
		check_for_updates = config.getBoolean("check-for-updates");
		if (!(config.isSet("check-for-updates"))) {
			getLogger().config("Updating configuration file with new `check-for-updates' key...");
			/* Like above, `check_for_updates' holds the default value */
			config.set("check-for-updates", check_for_updates);
			try {
				this.saveConfig();
			} catch (Throwable ex) {
				getLogger().warning("Failed to update configuration file.");
				ex.printStackTrace();
			}
		}

		/* Finally, separate orangetext and greentext exceptions if not yet done */
		if (config.isSet("exceptions") &&
		    !(config.isSet("greentext-exceptions") || config.isSet("orangetext-exceptions"))) {
			getLogger().config("Updating configuration file to have separate `greentext-exception' and `orangetext-exception' lists (cloning contents of `exceptions)");
			/* Clone the values of `exceptions' to the specific lists and remove `exceptions' */
			List<String> exceptions_tmp = config.getStringList("exceptions");
			config.set("greentext-exceptions", exceptions_tmp);
			config.set("orangetext-exceptions", exceptions_tmp);
			config.set("exceptions", null);
			try {
				this.saveConfig();
			} catch (Throwable ex) {
				getLogger().warning("Failed to update configuration file.");
				ex.printStackTrace();
			}
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

		getLogger().info("Successfully initialised " + pdf.getName() + " v" + pdf.getVersion());

		/* Check for updates on the Spigot resource page
		 * (XXX this will work even if current version is newer, i.e. unreleased SNAPSHOT;
		 * this is to keep things simple as 99.8% of people won't be building something
		 * like this from the repo) */
		if (check_for_updates) {
			getLogger().fine("Checking for updates...");
			String current_version = pdf.getVersion();
			try {
				URL update_url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + PROJECT_ID);
				URLConnection update_conn = update_url.openConnection();
				String new_version = new BufferedReader(new InputStreamReader(update_conn.getInputStream())).readLine();
				if (!(new_version.equals(current_version))) {
					getLogger().info("New version " + new_version + " found (currently on " + current_version + "; download at: <https://www.spigotmc.org/resources/" + PROJECT_ID + "/>");
				} else {
					getLogger().info("No new version found");
				}
			} catch (Throwable ex) {
				getLogger().warning("Failed to check for updates");
				ex.printStackTrace();
			}
		}

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

		getLogger().finest("ChatEvent passed to `eventMakeGreentext()'; attempting to make message green: `" + message + "'");
		try {
			e.setMessage(ChatColor.GREEN + message);
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

		getLogger().finest("ChatEvent passed to `eventMakeOrangetext()'; attempting to make message gold (orange): `" + message + "'");
		try {
			e.setMessage(ChatColor.GOLD + message);
		} catch (Throwable ex) {
			getLogger().warning("Failed to make ChatEvent orangetext: message `" + message + "'");
			ex.printStackTrace();
		}
	}
}
