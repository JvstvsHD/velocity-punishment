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
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.PunishmentException;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import static de.jvstvshd.velocitypunishment.internal.Util.copyComponent;

public class BanCommand {

    public static BrigadierCommand banCommand(VelocityPunishmentPlugin plugin) {
        var node = Util.permissibleCommand("ban", "velocitypunishment.command.ban")
                .then(Util.playerArgument(plugin.getServer()).executes(context -> execute(context, plugin))
                        .then(Util.reasonArgument.executes(context -> execute(context, plugin))));
        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> context, VelocityPunishmentPlugin plugin) {
        var source = context.getSource();
        var player = context.getArgument("player", String.class);
        var reason = PunishmentHelper.parseReason(context);
        var playerResolver = plugin.getPlayerResolver();
        var punishmentManager = plugin.getPunishmentManager();
        playerResolver.getOrQueryPlayerUuid(player, plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
            try {
                punishmentManager.createPermanentBan(uuid, reason).punish().whenComplete((ban, t) -> {
                    if (t != null) {
                        plugin.getLogger().error("An error occurred while creating a ban for player " + player + " (" + uuid + ")", t);
                        source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                    } else {
                        String uuidString = uuid.toString().toLowerCase();
                        source.sendMessage(plugin.getMessageProvider().provide("command.ban.success", source, true, copyComponent(player, plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                                copyComponent(uuidString, plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                                reason).color(NamedTextColor.GREEN));
                        source.sendMessage(plugin.getMessageProvider().provide("commands.general.punishment.id", source, true, copyComponent(ban.getPunishmentUuid().toString().toLowerCase(), plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW)));
                    }
                });
            } catch (PunishmentException e) {
                plugin.getLogger().error("An error occurred while creating a ban for player " + player + " (" + uuid + ")", e);
                Util.sendErrorMessage(context, e);
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}
