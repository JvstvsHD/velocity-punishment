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

package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.punishment.PunishmentDuration;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import de.jvstvshd.velocitypunishment.listener.ChatListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.internal.Util.copyComponent;

public class TempmuteCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final ExecutorService service;
    private final VelocityPunishmentPlugin plugin;
    private final ChatListener chatListener;

    public TempmuteCommand(VelocityPunishmentPlugin plugin, ChatListener chatListener) {
        this.proxyServer = plugin.getServer();
        this.service = plugin.getService();
        this.plugin = plugin;
        this.chatListener = chatListener;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (invocation.arguments().length < 2) {
            source.sendMessage(plugin.getMessageProvider().provide("command.tempmute.usage", source, true).color(NamedTextColor.RED));
            return;
        }
        PunishmentHelper parser = new PunishmentHelper();
        plugin.getPlayerResolver().getOrQueryPlayerUuid(invocation.arguments()[0], service).whenCompleteAsync((uuid, throwable) -> {
            if (throwable != null) {
                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                throwable.printStackTrace();
                return;
            }
            if (uuid == null) {
                source.sendMessage(plugin.getMessageProvider().provide("commands.general.not-found", source, true, Component.text(invocation.arguments()[0]).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
                return;
            }
            Optional<PunishmentDuration> optDuration = parser.parseDuration(1, invocation, plugin.getMessageProvider());
            if (optDuration.isEmpty()) {
                return;
            }
            PunishmentDuration duration = optDuration.get();
            TextComponent component = parser.parseComponent(2, invocation, Component.text("mute").color(NamedTextColor.DARK_RED));
            plugin.getPunishmentManager().createMute(uuid, component, duration).punish().whenComplete((mute, t) -> {
                if (t != null) {
                    invocation.source().sendMessage(plugin.getMessageProvider().internalError(source, true));
                    t.printStackTrace();
                    return;
                }
                String until = duration.expiration().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                String uuidString = uuid.toString().toLowerCase();
                source.sendMessage(plugin.getMessageProvider().provide("command.tempmute.success", source, true,
                        copyComponent(invocation.arguments()[0], plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                        copyComponent(uuidString, plugin.getMessageProvider(), source).color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        component,
                        Component.text(until).color(NamedTextColor.GREEN)));
                source.sendMessage(plugin.getMessageProvider().provide("commands.general.punishment.id", source, true, copyComponent(mute.getPunishmentUuid().toString().toLowerCase(), plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW)));
                try {
                    chatListener.update(uuid);
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }, service);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            return Util.getPlayerNames(proxyServer.getAllPlayers());
        }
        if (args.length == 1) {
            return Util.getPlayerNames(proxyServer.getAllPlayers())
                    .stream().filter(s -> s.toLowerCase().startsWith(args[0])).collect(Collectors.toList());
        }
        return ImmutableList.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.tempmute");
    }
}
