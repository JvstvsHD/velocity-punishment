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
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import de.chojo.sadu.wrapper.QueryBuilder;
import de.chojo.sadu.wrapper.QueryBuilderConfig;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class WhitelistCommand {

    public static final List<String> options = ImmutableList.of("add", "remove");


    public static BrigadierCommand whitelistCommand(VelocityPunishmentPlugin plugin) {
        var node = Util.permissibleCommand("whitelist", "velocitypunishment.command.whitelist")
                .then(Util.playerArgument(plugin.getServer()).executes(context -> execute(context, plugin))
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("option", StringArgumentType.word()).executes(context -> execute(context, plugin))
                                .suggests((context, builder) -> {
                                    for (String option : options) {
                                        builder.suggest(option);
                                    }
                                    return builder.buildFuture();
                                })));
        return new BrigadierCommand(node);
    }

    private static int execute(CommandContext<CommandSource> context, VelocityPunishmentPlugin plugin) {
        var source = context.getSource();
        if (!plugin.whitelistActive()) {
            source.sendMessage(Component.text("Whitelist is not active. You may activate it by setting 'whitelistActivated' in 'plugins/velocity-punishment/config.json' to true.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        var player = context.getArgument("player", String.class);
        if (context.getArguments().containsKey("option")) {
            var option = context.getArgument("option", String.class).toLowerCase();
            switch (option) {
                case "add", "remove" ->
                        plugin.getPlayerResolver().getOrQueryPlayerUuid(player, plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
                            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
                            var builder = QueryBuilder.builder(plugin.getDataSource())
                                    .configure(QueryBuilderConfig.defaultConfig())
                                    .query(option.equalsIgnoreCase("remove") ? "DELETE FROM velocity_punishment_whitelist WHERE uuid = ?;" :
                                            "INSERT INTO velocity_punishment_whitelist (uuid) VALUES (?);")
                                    .parameter(paramBuilder -> paramBuilder.setUuidAsString(uuid));
                            if (option.equalsIgnoreCase("remove")) {
                                builder.delete().send();
                                plugin.getServer().getPlayer(uuid).ifPresent(pl -> pl.disconnect(Component.text("You have been blacklisted.").color(NamedTextColor.DARK_RED)));
                            } else {
                                builder.insert().send();
                            }
                        }, plugin.getService());
                default ->
                        source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.usage", source, true));
            }
            return Command.SINGLE_SUCCESS;
        }
        plugin.getPlayerResolver().getOrQueryPlayerUuid(player, plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
            if (Util.sendErrorMessageIfErrorOccurred(context, uuid, throwable, plugin)) return;
            QueryBuilder.builder(plugin.getDataSource(), Boolean.class)
                    .configure(QueryBuilderConfig.defaultConfig())
                    .query("SELECT uuid FROM velocity_punishment_whitelist WHERE uuid = ?;")
                    .parameter(paramBuilder -> paramBuilder.setUuidAsString(uuid))
                    .readRow(rs -> true).first().thenAcceptAsync(aBoolean -> {
                        var whitelisted = aBoolean.isPresent() ? plugin.getMessageProvider().provide("whitelist.status.whitelisted", source) :
                                plugin.getMessageProvider().provide("whitelist.status.disallowed", source);
                        source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.status", source, true, Component.text(player).color(NamedTextColor.YELLOW), whitelisted.color(NamedTextColor.YELLOW)));
                    });
        }, plugin.getService());
        return Command.SINGLE_SUCCESS;
    }
}