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

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * @author    Seth Price <ssterling AT firemail DOT cc>
 * @version   2.0
 * @since     1.0
 */
public class BgtChatListener implements Listener
{
	private final BukkitGreentext plugin;

	public BgtChatListener(BukkitGreentext plugin)
	{
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e)
	{
		Player player = e.getPlayer();
		
		/* Decrease CPU/IO load by initialising these only once */
		boolean check_perms = plugin.config.getBoolean("check-for-permission");

		/* I apologise for the weird nesting. */
		if (plugin.playerIsEnabled(player)) {
			if (((check_perms && player.hasPermission("greentext.chat.green")) || ! check_perms)
			    && plugin.isValidGreentext(e.getMessage())) {
				plugin.eventMakeGreentext(e);
			} else if (plugin.config.getBoolean("allow-orangetext")
			           && ((check_perms && player.hasPermission("greentext.chat.orange")) || ! check_perms)
			           && plugin.isValidOrangetext(e.getMessage())) {
				plugin.eventMakeOrangetext(e);
			}
		}
	}
}
