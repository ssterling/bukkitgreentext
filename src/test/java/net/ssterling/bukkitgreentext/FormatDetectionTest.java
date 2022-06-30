/*
 * BukkitGreentext - add 4chan-style quoting to Minecraft server chat
 *
 * Copyright 2018, 2022 Seth Price
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.bukkit.Bukkit;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

/**
 * Check that all greentext detection and orangetext detection
 * functions as expected.
 *
 * @author	Seth Price <sprice623 AT aol DOT com>
 * @version	3.0.1
 * @since	3.0.1
 */
class FormatDetectionTest
{
	private ServerMock server;
	private BukkitGreentext plugin;
	
	private static class MessageUnit
	{
		/** The message to check. */
		public final String message;

		/**
		 * Whether the message should be detected as greentext
		 * during normal operation.
		 */
		public final boolean is_greentext;

		/**
		 * Whether the message should be detected as orangetext
		 * during normal operation.
		 */
		public final boolean is_orangetext;

		public MessageUnit(final String message, final boolean is_greentext, final boolean is_orangetext)
		{
			this.message = message;
			this.is_greentext = is_greentext;
			this.is_orangetext = is_orangetext;
		}
	}

	private final MessageUnit[] messages = {
		new MessageUnit(">abc123", true, false),
		new MessageUnit(">abc123<", true, true),
		new MessageUnit(" >abc123", false, false),
		new MessageUnit("abc123< ", false, false),
		new MessageUnit("abc123<", false, true),
		new MessageUnit(" >abc123< ", false, false),
		new MessageUnit("abc123", false, false),
		new MessageUnit("><", true, true),
		new MessageUnit(">", false, false),
		new MessageUnit("> ", true, false),
		new MessageUnit("<", false, false),
		new MessageUnit(" <", false, true),

		/* Be aware test will use default config */
		new MessageUnit(">:C", false, false),
		new MessageUnit(">_<", false, false),
		new MessageUnit("C:<", false, false)
	};

	@BeforeEach
	public void setUp()
	{
		server = MockBukkit.mock();
		plugin = MockBukkit.load(BukkitGreentext.class);
	}

	@AfterEach
	public void tearDown()
	{
		Bukkit.getScheduler().cancelTasks(plugin);
		MockBukkit.unmock();
	}

	@Test
	void testFormatDetection()
	{
		for (MessageUnit unit : messages) {
			assertEquals(unit.is_greentext, plugin.isValidGreentext(unit.message));
			assertEquals(unit.is_orangetext, plugin.isValidOrangetext(unit.message));
		}
	}
}
