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
 * @version   2.0
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
		/* Convert CommandSender to Player type if sender is indeed a player */
		Player player;
		if (sender instanceof Player) {
			player = (Player) sender;
		} else {
			/* Maven won't compile unless I explicitly initialise this */
			player = null;
		}

		/* For weird processing later */
		boolean arg_enabled;
		String enabled_disabled;

		/* No arguments = same as `/greentext toggle' */
		if (args.length == 0) {
			/* Console can't use greentext hence cannot toggle it; exit */
			if (!(sender instanceof Player)) {
				sender.sendMessage("Console can only toggle greentext for players.");
				return true;
			}

			if (plugin.playerIsEnabled(player)) {
				sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " disabled.");
				plugin.playerSetEnabled(player, false);
			} else {
				sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + " enabled.");
				plugin.playerSetEnabled(player, true);
			}
			return true;
		} else if (args.length > 2) {
			/* Too many arguments */
			return false;
		}

		/* This (as opposed to separate clauses for `on' and `off') involves some weird processing,
		 * as you can see here, but it saves me from re-writing a whole lot of code in the end. */
		if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off")) {

			/* The spaces in `enabled_disabled' are necessary; see the concatenation below. */
			if (args[0].equalsIgnoreCase("on")) {
				arg_enabled = true;
				enabled_disabled = " enabled";
			} else {
				arg_enabled = false;
				enabled_disabled = " disabled";
			}

			if (args.length == 2) {
				if (!(sender instanceof Player) || player.hasPermission("greentext.toggle.others")) {
					/* Sorry for the guy whose IGN is `global' */
					if (args[1].equalsIgnoreCase("global")) {
						if (sender instanceof Player) {
							sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + enabled_disabled + " globally.");
						}
						plugin.globalSetEnabled(arg_enabled);
						return true;
					} else {
						Player targetPlayer = plugin.getServer().getPlayer(args[1]);
						if (targetPlayer != null) {
							if (sender instanceof Player) {
								sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + enabled_disabled + " for player " + targetPlayer.getName() + ".");
							}
							plugin.playerSetEnabled(targetPlayer, arg_enabled);
							return true;
						} else {
							sender.sendMessage("Player " + args[1] + " is nonexistent or offline.");
							return true;
						}
					}
				} else {
					sender.sendMessage(ChatColor.RED + "Insufficient permission to toggle greentext for others.");
					return true;
				}
			} else if (args.length == 1) {
				/* Console can't use greentext hence cannot toggle it; exit */
				if (!(sender instanceof Player)) {
					sender.sendMessage("Console can only toggle greentext for players.");
					return true;
				}

				if (player.hasPermission("greentext.toggle")) {
					if (sender instanceof Player) {
						sender.sendMessage(ChatColor.GREEN + ">greentext" + ChatColor.RESET + enabled_disabled + ".");
					}
					plugin.playerSetEnabled(player, arg_enabled);
					return true;
				} else {
					sender.sendMessage(ChatColor.RED + "Insufficient permission to toggle greentext.");
					return true;
				}
			} else {
				/* Too many arguments */
				return false;
			}
		}

		/* Argument is invalid */
		return false;
	}
}
