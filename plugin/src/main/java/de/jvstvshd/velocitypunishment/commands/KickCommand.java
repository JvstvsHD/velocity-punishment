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

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class KickCommand implements SimpleCommand {

    private final VelocityPunishmentPlugin plugin;

    public KickCommand(VelocityPunishmentPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        if (invocation.arguments().length < 1) {
            source.sendMessage(plugin.getMessageProvider().provide("command.kick.usage", source, true).color(NamedTextColor.RED));
            return;
        }
        var playerArgument = invocation.arguments()[0];
        Optional<Player> playerOptional = plugin.getServer().getPlayer(playerArgument);
        if (playerOptional.isEmpty()) {
            try {
                playerOptional = plugin.getServer().getPlayer(Util.parseUuid(playerArgument));
            } catch (Exception e) {
                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                return;
            }
        }
        if (playerOptional.isEmpty()) {
            source.sendMessage(plugin.getMessageProvider().provide("commands.general.not-found", source, true, Component.text(playerArgument).color(NamedTextColor.YELLOW)));
            return;
        }
        var player = playerOptional.get();
        Component reason = new PunishmentHelper().parseComponent(1, invocation, Component.text("Kick").color(NamedTextColor.DARK_RED));
        player.disconnect(reason);
        invocation.source().sendMessage(plugin.getMessageProvider().provide("command.kick.success", source, true, Component.text(player.getUsername()).color(NamedTextColor.YELLOW), reason));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.kick");
    }
}
