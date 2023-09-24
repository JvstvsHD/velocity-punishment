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
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.PunishmentType;
import de.jvstvshd.velocitypunishment.api.punishment.StandardPunishmentType;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;

public class PunishmentRemovalCommand {

    public static BrigadierCommand unmuteCommand(VelocityPunishmentPlugin plugin) {
        var node = Util.permissibleCommand("unmute", "velocitypunishment.command.unmute")
                .then(Util.punishmentRemoveArgument(plugin).executes(context -> execute(context, plugin, "unmute", StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE)));
        return new BrigadierCommand(node);
    }

    public static BrigadierCommand unbanCommand(VelocityPunishmentPlugin plugin) {
        var node = Util.permissibleCommand("unban", "velocitypunishment.command.unban")
                .then(Util.punishmentRemoveArgument(plugin).executes(context -> execute(context, plugin, "unban", StandardPunishmentType.BAN, StandardPunishmentType.PERMANENT_BAN)));
        return new BrigadierCommand(node);
    }

    public static int execute(CommandContext<CommandSource> context, VelocityPunishmentPlugin plugin, String commandName, PunishmentType... types) {
        var source = context.getSource();
        PunishmentHelper.getPlayerUuid(context, plugin).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
            plugin.getPunishmentManager().getPunishments(uuid, plugin.getService(), types).whenComplete((punishments, t) -> {
                if (t != null) {
                    plugin.getLogger().error("An error occurred while getting punishments for player " + uuid, t);
                    source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                    return;
                }
                if (punishments.isEmpty()) {
                    source.sendMessage(plugin.getMessageProvider().provide("command.punishment.not-banned", source, true).color(NamedTextColor.RED));
                    return;
                }
                if (punishments.size() > 1) {
                    source.sendMessage(plugin.getMessageProvider().provide("command." + commandName + ".multiple-bans", source, true).color(NamedTextColor.YELLOW));
                    for (Punishment punishment : punishments) {
                        source.sendMessage(buildComponent(PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider(), source), punishment));
                    }
                } else {
                    Punishment punishment = punishments.get(0);
                    try {
                        punishment.cancel().whenCompleteAsync((unused, th) -> {
                            if (th != null) {
                                plugin.getLogger().error("An error occurred while removing punishment " + punishment.getPunishmentUuid() + " for player " + uuid, th);
                                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                                return;
                            }
                            source.sendMessage(plugin.getMessageProvider().provide("command." + commandName + ".success").color(NamedTextColor.GREEN));
                        }, plugin.getService());
                    } catch (PunishmentException e) {
                        plugin.getLogger().error("An error occurred while removing punishment " + punishment.getPunishmentUuid() + " for player " + uuid, e);
                        Util.sendErrorMessage(context, e);
                    }
                }
            });
        }, plugin.getService());
        return Command.SINGLE_SUCCESS;
    }

    private static Component buildComponent(Component dataComponent, Punishment punishment) {
        return dataComponent.clickEvent(ClickEvent.runCommand("/punishment " + punishment.getPunishmentUuid()
                        .toString().toLowerCase(Locale.ROOT) + " remove"))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(Component
                        .text("Click to remove punishment").color(NamedTextColor.GREEN)));
    }
}
