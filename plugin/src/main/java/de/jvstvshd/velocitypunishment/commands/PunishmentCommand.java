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
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PunishmentCommand {

    private final static List<String> PUNISHMENT_OPTIONS = ImmutableList.of("cancel", "remove", "info", "change");

    public static BrigadierCommand punishmentCommand(VelocityPunishmentPlugin plugin) {
        var node = Util.permissibleCommand("punishment", "velocitypunishment.command.punishment")
                .then(LiteralArgumentBuilder.<CommandSource>literal("playerinfo")
                        .then(Util.punishmentRemoveArgument(plugin).executes(context -> executePlayerInfo(context, plugin))))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("punishment ID", StringArgumentType.word())
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("option", StringArgumentType.word()).suggests((context, builder) -> {
                            PUNISHMENT_OPTIONS.forEach(builder::suggest);
                            return builder.buildFuture();
                        }).executes(context -> execute(context, plugin))));
        return new BrigadierCommand(node);
    }

    private static int executePlayerInfo(CommandContext<CommandSource> context, VelocityPunishmentPlugin plugin) {
        CommandSource source = context.getSource();
        var punishmentManager = plugin.getPunishmentManager();
        PunishmentHelper.getPlayerUuid(context, plugin).whenCompleteAsync((uuid, throwable) -> {
            if (throwable != null) {
                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                throwable.printStackTrace();
                return;
            }
            if (uuid == null) {
                source.sendMessage(plugin.getMessageProvider().provide("command.punishment.not-banned", source, true).color(NamedTextColor.RED));
                return;
            }
            punishmentManager.getPunishments(uuid, plugin.getService()).whenComplete((punishments, t) -> {
                if (t != null) {
                    source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                    t.printStackTrace();
                    return;
                }
                source.sendMessage(plugin.getMessageProvider().provide("command.punishment.punishments", source, true, Component.text(punishments.size())).color(NamedTextColor.AQUA));
                for (Punishment punishment : punishments) {
                    Component component = PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider(), source)
                            .clickEvent(ClickEvent.suggestCommand(punishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT)))
                            .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(plugin.getMessageProvider().provide("commands.general.copy")
                                    .color(NamedTextColor.GREEN)));
                    source.sendMessage(component);
                }
            });
        }, plugin.getService());
        return Command.SINGLE_SUCCESS;
    }

    private static int execute(CommandContext<CommandSource> context, VelocityPunishmentPlugin plugin) {
        var source = context.getSource();
        var uuidString = context.getArgument("Punishment UUID", String.class);
        UUID uuid;
        try {
            uuid = Util.parseUuid(uuidString);
        } catch (IllegalArgumentException e) {
            source.sendMessage(plugin.getMessageProvider().provide("command.punishment.uuid-parse-error", source, true, Component.text(uuidString).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
            return 0;
        }
        String option = context.getArgument("option", String.class);
        if (!PUNISHMENT_OPTIONS.contains(option)) {
            source.sendMessage(plugin.getMessageProvider().provide("command.punishment.unknown-option", source, true, Component.text(option).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
            return 0;
        }
        plugin.getPunishmentManager().getPunishment(uuid, plugin.getService()).whenCompleteAsync((optional, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                return;
            }
            Punishment punishment;
            if (optional.isEmpty()) {
                source.sendMessage(plugin.getMessageProvider().provide("command.punishment.unknown-punishment-id", source, true,
                        Component.text(uuid.toString().toLowerCase(Locale.ROOT)).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
                return;
            }
            punishment = optional.get();
            switch (option) {
                case "cancel", "remove" -> punishment.cancel().whenCompleteAsync((unused, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                        source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                        return;
                    }
                    source.sendMessage(plugin.getMessageProvider().provide("punishment.remove", source, true).color(NamedTextColor.GREEN));
                });
                case "info" ->
                        source.sendMessage(PunishmentHelper.buildPunishmentData(punishment, plugin.getMessageProvider(), source));
                case "change" -> source.sendMessage(Component.text("Soon (TM)"));
            }
        });
        return Command.SINGLE_SUCCESS;
    }
}