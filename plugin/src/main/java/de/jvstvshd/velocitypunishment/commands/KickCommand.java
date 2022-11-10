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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;

public class KickCommand {

    public static BrigadierCommand kickCommand(VelocityPunishmentPlugin plugin) {
        var node = Util.permissibleCommand("kick", "velocitypunishment.command.kick")
                .then(Util.playerArgument(plugin.getServer()).executes(context -> execute(context, plugin))
                        .then(Util.reasonArgument.executes(context -> execute(context, plugin))));
        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> context, VelocityPunishmentPlugin plugin) {
        var source = context.getSource();
        var playerArgument = context.getArgument("player", String.class);
        Optional<Player> playerOptional = plugin.getServer().getPlayer(playerArgument);
        if (playerOptional.isEmpty()) {
            try {
                playerOptional = plugin.getServer().getPlayer(Util.parseUuid(playerArgument));
            } catch (Exception e) {
                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                return 0;
            }
        }
        if (playerOptional.isEmpty()) {
            source.sendMessage(plugin.getMessageProvider().provide("commands.general.not-found", source, true, Component.text(playerArgument).color(NamedTextColor.YELLOW)));
            return 0;
        }
        var player = playerOptional.get();
        Component reason = PunishmentHelper.parseReason(context);
        player.disconnect(reason);
        source.sendMessage(plugin.getMessageProvider().provide("command.kick.success", source, true, Component.text(player.getUsername()).color(NamedTextColor.YELLOW), reason));
        return Command.SINGLE_SUCCESS;
    }
}
