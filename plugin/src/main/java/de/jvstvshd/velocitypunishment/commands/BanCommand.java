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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

import static de.jvstvshd.velocitypunishment.internal.Util.copyComponent;

public class BanCommand implements SimpleCommand {

    private final VelocityPunishmentPlugin plugin;

    public BanCommand(VelocityPunishmentPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (invocation.arguments().length < 1) {
            source.sendMessage(plugin.getMessageProvider().provide("command.ban.usage", source, true).color(NamedTextColor.RED));
            return;
        }
        var playerResolver = plugin.getPlayerResolver();
        var punishmentManager = plugin.getPunishmentManager();
        var parser = new PunishmentHelper();
        playerResolver.getOrQueryPlayerUuid(invocation.arguments()[0], plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(invocation, source, uuid, throwable, plugin)) return;
            TextComponent component = parser.parseComponent(1, invocation, Component.text("ban").color(NamedTextColor.DARK_RED));
            punishmentManager.createPermanentBan(uuid, component).punish().whenCompleteAsync((ban, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                } else {
                    String uuidString = uuid.toString().toLowerCase();
                    source.sendMessage(plugin.getMessageProvider().provide("command.ban.success", source, true, copyComponent(invocation.arguments()[0], plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                            copyComponent(uuidString, plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                            component).color(NamedTextColor.GREEN));
                    source.sendMessage(plugin.getMessageProvider().provide("commands.general.punishment.id", source, true, copyComponent(ban.getPunishmentUuid().toString().toLowerCase(), plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW)));
                }
            });
        }, plugin.getService());
    }


    @Override
    public List<String> suggest(Invocation invocation) {
        var proxyServer = plugin.getServer();
        return Util.getPlayerNames(invocation, proxyServer);
    }


    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.ban");
    }
}
