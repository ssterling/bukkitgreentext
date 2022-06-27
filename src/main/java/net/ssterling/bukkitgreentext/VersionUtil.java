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
package net.ssterling.bukkitgreentext;

import org.bukkit.Bukkit;

/**
 * @author    Seth Price <sprice623 AT aol DOT com>
 * @version   3.0
 * @since     3.0
 */
public final class VersionUtil
{
	/**
	 * Determine whether a given major version is newer than the other specified.
	 *
	 * @param server_version	Version to compare.
	 * @param compare_to		Version to compare to.
	 * @return true if server_version is newer than or the same as compare_to, false otherwise
	 * @since 3.0
	 */
	public static boolean compareVersions(final String server_version, final String compare_to)
	{
		final int server_major = Integer.parseInt(server_version.split("\\.")[1].replaceAll("[\\(\\)]", ""));
		final int compare_major = Integer.parseInt(compare_to.split("\\.")[1].replaceAll("[\\(\\)]", ""));

		return compare_major >= server_major ? true : false;
	}
}
