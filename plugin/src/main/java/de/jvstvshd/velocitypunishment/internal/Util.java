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

package de.jvstvshd.velocitypunishment.internal;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.TemporalPunishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class Util {

    public static final RequiredArgumentBuilder<CommandSource, String> reasonArgument = RequiredArgumentBuilder.argument("reason", StringArgumentType.greedyString());
    public static RequiredArgumentBuilder<CommandSource, String> durationArgument = RequiredArgumentBuilder.argument("duration", StringArgumentType.word());

    public static List<String> getPlayerNames(Collection<Player> players) {
        return players.stream().map(Player::getUsername).toList();
    }

    public static RequiredArgumentBuilder<CommandSource, String> playerArgument(ProxyServer server) {
        return RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word()).suggests((context, builder) -> {
            Collection<Player> players = server.getAllPlayers();
            for (Player player : players) {
                builder.suggest(player.getUsername());
            }
            return builder.buildFuture();
        });
    }

    public static RequiredArgumentBuilder<CommandSource, String> punishmentRemoveArgument(VelocityPunishmentPlugin plugin) {
        return RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word()).suggests((context, builder) -> Util.executeAsync(() -> {
            var input = builder.getRemainingLowerCase();
            if (input.isBlank() || input.length() <= 2) return builder.build();
            try (Connection connection = plugin.getDataSource().getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT name FROM velocity_punishment WHERE name LIKE ?");
                statement.setString(1, input + "%");
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    builder.suggest(name);
                }
                plugin.getServer().getAllPlayers().stream().map(Player::getUsername).forEach(builder::suggest);
            }
            return builder.build();
        }, plugin.getService()));
    }

    public static LiteralArgumentBuilder<CommandSource> permissibleCommand(String name, String permission) {
        return LiteralArgumentBuilder.<CommandSource>literal(name).requires(source -> source.hasPermission(permission));
    }

    public static UUID parseUuid(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return UUID.fromString(uuidString.replaceAll(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"));
        }
    }

    public static TextComponent copyComponent(String text, MessageProvider provider, CommandSource source) {
        return Component.text(text).clickEvent(ClickEvent.suggestCommand(text))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(provider.provide("commands.general.copy", source).color(NamedTextColor.GREEN)));
    }

    public static <T> CompletableFuture<T> executeAsync(Callable<T> task, Executor service) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        service.execute(() -> {
            try {
                cf.complete(task.call());
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    public static <T extends TemporalPunishment> T getLongestPunishment(List<T> list) {
        if (list.isEmpty())
            return null;
        List<T> sorted = sortPunishments(list);
        return sorted.get(sorted.size() - 1);
    }

    public static <T extends TemporalPunishment> List<T> sortPunishments(List<T> list) {
        return list.stream().sorted(Comparator.comparing(TemporalPunishment::getDuration)).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Punishment> List<T> convert(List<? super T> list) {
        List<T> out = new ArrayList<>();
        for (Object o : list) {
            out.add((T) o);
        }
        return out;
    }

    public static String trimUuid(UUID origin) {
        return origin.toString().toLowerCase().replace("-", "");
    }

    public static boolean sendErrorMessageIfErrorOccurred(SimpleCommand.Invocation invocation, CommandSource source, UUID uuid, Throwable throwable, VelocityPunishmentPlugin plugin) {
        if (throwable != null) {
            source.sendMessage(plugin.getMessageProvider().internalError(source, true));
            throwable.printStackTrace();
            return true;
        }
        if (uuid == null) {
            source.sendMessage(Component.translatable().args(Component.text(invocation.arguments()[0]).color(NamedTextColor.YELLOW)).key("commands.general.not-found").color(NamedTextColor.RED));
            return true;
        }
        return false;
    }

    public static boolean sendErrorMessageIfErrorOccurred(CommandContext<CommandSource> context, UUID uuid, Throwable throwable, VelocityPunishmentPlugin plugin) {
        var source = context.getSource();
        var player = context.getArgument("player", String.class);
        if (throwable != null) {
            source.sendMessage(plugin.getMessageProvider().internalError(source, true));
            throwable.printStackTrace();
            return true;
        }
        if (uuid == null) {
            source.sendMessage(Component.translatable().args(Component.text(player).color(NamedTextColor.YELLOW)).key("commands.general.not-found").color(NamedTextColor.RED));
            return true;
        }
        return false;
    }

    public static List<String> getPlayerNames(SimpleCommand.Invocation invocation, ProxyServer proxyServer) {
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
}
