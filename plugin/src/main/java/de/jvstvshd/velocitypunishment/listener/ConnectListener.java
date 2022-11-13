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

package de.jvstvshd.velocitypunishment.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.punishment.Ban;
import de.jvstvshd.velocitypunishment.api.punishment.Mute;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.StandardPunishmentType;
import de.jvstvshd.velocitypunishment.common.plugin.MuteData;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class ConnectListener {
    private final VelocityPunishmentPlugin plugin;
    private final ExecutorService service;
    private final ProxyServer proxyServer;
    private final ChatListener chatListener;

    public ConnectListener(VelocityPunishmentPlugin plugin,
                           ExecutorService service, ProxyServer proxyServer, ChatListener chatListener) {
        this.plugin = plugin;
        this.service = service;
        this.proxyServer = proxyServer;
        this.chatListener = chatListener;
    }

    @Subscribe
    public void onConnect(LoginEvent event) throws Exception {
        if (plugin.whitelistActive()) {
            plugin.getLogger().info("Whitelist is activated.");
            try (var connection = plugin.getDataSource().getConnection();
                 var statement = connection.prepareStatement("SELECT * FROM velocity_punishment_whitelist WHERE uuid = ?;")) {
                statement.setString(1, Util.trimUuid(event.getPlayer().getUniqueId()));
                if (!statement.executeQuery().next()) {
                    event.setResult(ResultedEvent.ComponentResult.denied(Component.text("WHITELIST").color(NamedTextColor.DARK_RED)));
                    return;
                }
            }
        }
        List<Punishment> punishments;
        try {
            punishments = plugin.getPunishmentManager().getPunishments(event.getPlayer().getUniqueId(), service, StandardPunishmentType.BAN,
                    StandardPunishmentType.PERMANENT_BAN, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            event.setResult(ResultedEvent.ComponentResult.denied(plugin.getMessageProvider().internalError(event.getPlayer(), true)));
            return;
        }
        List<Ban> bans = new ArrayList<>();
        List<Mute> mutes = new ArrayList<>();
        for (Punishment punishment : punishments) {
            if (punishment instanceof Ban velocityBan)
                bans.add(velocityBan);
            if (punishment instanceof Mute mute)
                mutes.add(mute);
        }
        for (Mute mute : mutes) {
            try {
                plugin.communicator().queueMute(mute, event.getPlayer(), MuteData.ADD);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Mute longestMute = Util.getLongestPunishment(mutes);
        if (longestMute != null) {
            longestMute.getDuration().absolute();
            chatListener.getMutes().put(event.getPlayer().getUniqueId(), new ChatListener.MuteContainer(true).setMute(longestMute));
        }
        if (bans.isEmpty()) {
            return;
        }
        final Ban ban = Util.getLongestPunishment(bans);
        if (ban == null)
            return;
        if (ban.isOngoing()) {
            Component deny = ban.createFullReason(event.getPlayer());
            event.setResult(ResultedEvent.ComponentResult.denied(deny));
        } else {
            ban.cancel().whenCompleteAsync((unused, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    return;
                }
                proxyServer.getConsoleCommandSource().sendMessage(Component.text()
                        .append(Component.text("Ban ").color(NamedTextColor.GREEN),
                                Component.text("'" + ban.getPunishmentUuid().toString().toLowerCase() + "'")
                                        .color(NamedTextColor.YELLOW),
                                Component.text("was cancelled.").color(NamedTextColor.GREEN)));
            }, service);
        }
    }

    public VelocityPunishmentPlugin plugin() {
        return plugin;
    }

    public ExecutorService service() {
        return service;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ConnectListener) obj;
        return Objects.equals(this.plugin, that.plugin) &&
                Objects.equals(this.service, that.service) &&
                Objects.equals(this.proxyServer, that.proxyServer) &&
                Objects.equals(this.chatListener, that.chatListener);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plugin, service, proxyServer, chatListener);
    }

    @Override
    public String toString() {
        return "ConnectListener[" +
                "plugin=" + plugin + ", " +
                "service=" + service + ", " +
                "proxyServer=" + proxyServer + ", " +
                "chatListener=" + chatListener + ']';
    }

}
