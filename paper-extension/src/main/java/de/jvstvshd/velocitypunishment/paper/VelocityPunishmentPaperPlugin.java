/*
 * This file is part of Velocity Punishment, which is licensed under the MIT license.
 *
 * Copyright (c) 2022 JvstvsHD
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.jvstvshd.velocitypunishment.paper;

import de.jvstvshd.velocitypunishment.common.plugin.MuteData;
import de.jvstvshd.velocitypunishment.paper.listeners.ChatListener;
import de.jvstvshd.velocitypunishment.paper.listeners.MessagingChannelListener;
import de.jvstvshd.velocitypunishment.paper.listeners.MuteInformation;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class VelocityPunishmentPaperPlugin extends JavaPlugin {

    private final List<MuteInformation> cachedMutes = new ArrayList<>();

    @Override
    public void onEnable() {
        getLogger().info("VelocityPunishmentPaperPlugin has been enabled!");
        getServer().getMessenger().registerIncomingPluginChannel(this, MuteData.MUTE_DATA_CHANNEL_IDENTIFIER, new MessagingChannelListener(this));
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("VelocityPunishmentPaperPlugin has been disabled!");
    }

    public List<MuteInformation> cachedMutes() {
        return cachedMutes;
    }
}