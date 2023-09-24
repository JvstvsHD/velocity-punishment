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
import de.jvstvshd.velocitypunishment.api.duration.PunishmentDuration;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static de.jvstvshd.velocitypunishment.internal.Util.copyComponent;

/**
 * @see VelocityPunishmentPlugin#MUTES_DISABLED
 */
public class TempmuteCommand {

    public static BrigadierCommand tempmuteCommand(VelocityPunishmentPlugin plugin) {
        var node = Util.permissibleCommand("tempmute", "velocitypunishment.command.tempmute")
                .then(Util.playerArgument(plugin.getServer())
                        .then(Util.durationArgument.executes(context -> execute(context, plugin))
                                .then(Util.reasonArgument.executes(context -> execute(context, plugin)))));
        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> context, VelocityPunishmentPlugin plugin) {
        CommandSource source = context.getSource();
        var player = context.getArgument("player", String.class);
        plugin.getPlayerResolver().getOrQueryPlayerUuid(player, plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
            Optional<PunishmentDuration> optDuration = PunishmentHelper.parseDuration(context, plugin.getMessageProvider());
            if (optDuration.isEmpty()) {
                return;
            }
            PunishmentDuration duration = optDuration.get();
            TextComponent reason = PunishmentHelper.parseReason(context);
            try {
                plugin.getPunishmentManager().createMute(uuid, reason, duration).punish().whenComplete((ban, t) -> {
                    if (t != null) {
                        source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                        plugin.getLogger().error("An error occurred while creating a ban for player " + player + " (" + uuid + ")", t);
                        return;
                    }
                    String until = duration.expiration().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                    String uuidString = uuid.toString().toLowerCase();
                    source.sendMessage(plugin.getMessageProvider().provide("command.tempmute.success", source, true,
                            copyComponent(player, plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                            copyComponent(uuidString, plugin.getMessageProvider(), source).color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                            reason,
                            Component.text(until).color(NamedTextColor.GREEN)).color(NamedTextColor.GREEN));
                    source.sendMessage(plugin.getMessageProvider().provide("commands.general.punishment.id", source, true, Component.text(ban.getPunishmentUuid().toString().toLowerCase()).color(NamedTextColor.YELLOW)));
                });
            } catch (PunishmentException e) {
                plugin.getLogger().error("An error occurred while creating a ban for player " + player + " (" + uuid + ")", e);
                Util.sendErrorMessage(context, e);
            }
        }, plugin.getService());
        return Command.SINGLE_SUCCESS;
    }
}
