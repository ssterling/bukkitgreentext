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

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

/**
 * @author    Seth Price <ssterling AT firemail DOT cc>
 * @version   2.1
 * @since     1.0
 */
public class BgtCommandExecutor implements CommandExecutor
{
	private final BukkitGreentext plugin;

	public BgtCommandExecutor(BukkitGreentext plugin)
	{
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		/* Why is all this better than a `switch' statement or something, you may ask?
		 * It probably isn't, but it at least saves my sanity by doing a lot of the
		 * processing all up here instead of copy-pasted several times throughout
		 * a more computationally consuming cascade of `if-then' clauses. */

		/* NOTE: the reason a lot of the following variables are initialised to `null',
		 * `false', empty strings, etc. is because Maven screams unless they're
		 * explicitly set with a value, even if they're never used. */

		/* Determine whether command sender is a player; if so, convert to Player type */
		Player player = null;
		boolean is_player = false;
		if (sender instanceof Player) {
			player = (Player) sender;
			is_player = true;
		}

		/* Determine whether sender sent too many arguments; if so, print usage */
		if (args.length > 2) {
			return false;
		}

		/* Determine whether first argument (if present) is `on' or `off'; also,
		 * set a string to `enabled' or `disabled' appropriately */
		boolean arg_enabled = false;
		String enabled_disabled = "";
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("on")) {
				arg_enabled = true;
				enabled_disabled = "enabled";
			} else if (args[0].equalsIgnoreCase("off")) {
				/* `arg_enabled' is already false */
				enabled_disabled = "disabled";
			} else {
				/* Incorrect syntax; print usage */
				return false;
			}
		}

		/* Determine whether second argument (if present) is either `global'
		 * or a valid player name (hopefully no one named `global' is online) */
		boolean is_global = false;
		Player target_player = null;
		if (args.length == 2) {
			if (args[1].equalsIgnoreCase("global")) {
				is_global = true;
			} else {
				/* `is_global' is already false */
				target_player = plugin.getServer().getPlayer(args[1]);

				/* Player is either offline or invalid; exit */
				if (target_player == null) {
					sender.sendMessage("Player " + args[1] + " is nonexistent or offline.");
					return true;
				}
			}
		}

		/*-------- actual command processing begins here --------*/

		/* If a player sends `/greentext' with no arguments, toggle greentext on/off */
		if (args.length == 0 && is_player) {
			if (plugin.playerIsEnabled(player)) {
				sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " disabled.");
				plugin.playerSetEnabled(player, false);
			} else {
				sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " enabled.");
				plugin.playerSetEnabled(player, true);
			}
			return true;
		}
		/* If a player sends `/greentext <on|off>' with no target, set the status
		 * for that player to the value specified in the command */
		else if (args.length == 1 && is_player) {
			/* If player doesn't have permission, exit */
			if (!(player.hasPermission("greentext.toggle"))) {
				sender.sendMessage(ChatColor.RED + "Insufficient permission to toggle greentext.");
				return true;
			}

			/* Player already has status set to value; ignore */
			if (plugin.playerIsEnabled(player) == arg_enabled) {
				sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " already " + enabled_disabled + ".");
				return true;
			}

			/* Method `playerSetEnabled()' already logs to the console,
			 * so only pretty-print to players */
			if (is_player) {
				sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " " + enabled_disabled + ".");
			}
			plugin.playerSetEnabled(player, arg_enabled);
			return true;
		}
		/* If either player or console sends `/greentext <on|off> player', set the status
		 * for that player to the value specified in the command */
		else if (args.length == 2 && !(is_global)) {
			/* If sender is player and they doesn't have permission, exit */
			if (is_player && !(player.hasPermission("greentext.toggle.others"))) {
				sender.sendMessage(ChatColor.RED + "Insufficient permission to toggle greentext for others.");
				return true;
			}

			/* Player already has status set to value; ignore */
			if (plugin.playerIsEnabled(target_player) == arg_enabled) {
				/* Again, to keep consistency with the uncoloured console messages */
				if (is_player) {
					sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " already " + enabled_disabled + " for player " + target_player.getName() + ".");
				} else {
					sender.sendMessage("Greentext already " + enabled_disabled + " for player " + target_player.getName());
				}
				return true;
			}

			/* Method `globalSetEnabled()' already logs to the console,
			 * so only pretty-print to players */
			if (is_player) {
				sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " " + enabled_disabled + " for player " + target_player.getName() + ".");
			}
			plugin.playerSetEnabled(target_player, arg_enabled);
			return true;
		}
		/* If either player or console sends `/greentext <on|off> global', or console sends
		 * `/greentext <on|off>', set the global status to the value specified in the command */
		else if ((args.length == 1 && !(is_player)) || is_global) {
			/* If sender is player and they doesn't have permission, exit */
			if (is_player && !(player.hasPermission("greentext.toggle.others"))) {
				sender.sendMessage(ChatColor.RED + "Insufficient permission to toggle greentext for others.");
				return true;
			}

			/* Player already has status set to value; ignore */
			if (plugin.globalIsEnabled() == arg_enabled) {
				/* Yet again, to keep consistency with the uncoloured console messages */
				if (is_player) {
					sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " already " + enabled_disabled + " globally.");
				} else {
					sender.sendMessage("Greentext already " + enabled_disabled + " globally");
				}
				return true;
			}

			/* Method `globalSetEnabled()' already logs to the console,
			 * so only pretty-print to players */
			if (is_player) {
				sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " " + enabled_disabled + " globally.");
			}
			plugin.globalSetEnabled(arg_enabled);
			return true;
		}

		/* If the command hasn't returned by now, there's been some sort of error;
		 * just print usage and exit */
		return false;
	}
}
