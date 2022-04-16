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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.PunishmentDuration;
import de.jvstvshd.velocitypunishment.api.punishment.TemporalPunishment;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class PunishmentHelper {

    public Component buildPunishmentData(Punishment punishment, MessageProvider provider, CommandSource source) {
        return Component.text()
                .append(provider.provide("helper.type", source, true).color(NamedTextColor.AQUA),
                        Component.text(punishment.getType().getName()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        provider.provide("helper.reason", source, true).color(NamedTextColor.AQUA),
                        punishment.getReason(),
                        Component.newline(),
                        punishment instanceof TemporalPunishment temporalPunishment ?
                                buildPunishmentDataTemporal(temporalPunishment, provider, source) : Component.text(""),
                        Component.newline()
                )
                .build();
    }

    public Component buildPunishmentDataTemporal(TemporalPunishment punishment, MessageProvider provider, CommandSource source) {
        return punishment.isPermanent() ? Component.text("permanent").color(NamedTextColor.RED) : Component.text()
                .append(provider.provide("helper.temporal.duration", source, true).color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().getRemainingDuration()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        provider.provide("helper.temporal.end", source, true).color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().getEnd()).color(NamedTextColor.YELLOW))
                /*Component.newline(),
                Component.text("initial duration: ").color(NamedTextColor.AQUA),
                Component.text(punishment.getDuration().getInitialDuration()).color(NamedTextColor.YELLOW))*/
                .build();
    }

    public Optional<PunishmentDuration> parseDuration(int argumentIndex, SimpleCommand.Invocation invocation, MessageProvider provider) {
        try {
            return Optional.ofNullable(PunishmentDuration.parse(invocation.arguments()[argumentIndex]));
        } catch (IllegalArgumentException e) {
            invocation.source().sendMessage(Component.text().append(Component.text("Cannot parse duration: ").color(NamedTextColor.RED),
                    Component.text(e.getMessage()).color(NamedTextColor.YELLOW)));
            return Optional.empty();
        } catch (Exception e) {
            invocation.source().sendMessage(provider.internalError(invocation.source(), true));
            throw new RuntimeException(e);
        }
    }

    public TextComponent parseComponent(int startIndex, SimpleCommand.Invocation invocation, TextComponent def) {
        if (invocation.arguments().length == startIndex) {
            return def;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < invocation.arguments().length; i++) {
            builder.append(invocation.arguments()[i]).append(" ");
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(builder.toString());
    }

    public TextComponent parseComponent(int startIndex, CommandContext<CommandSource> context, TextComponent def) {
        if (context.getArguments().size() <= startIndex) {
            return def;
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(StringArgumentType.getString(context, "reason"));
    }

    public CompletableFuture<UUID> getPlayerUuid(int argumentIndex, ExecutorService service, PlayerResolver playerResolver, SimpleCommand.Invocation invocation) {
        String argument = invocation.arguments()[argumentIndex];
        if (argument.length() <= 16) {
            return playerResolver.getOrQueryPlayerUuid(argument, service);
        } else if (argument.length() <= 36) {
            try {
                return CompletableFuture.completedFuture(Util.parseUuid(argument));
            } catch (IllegalArgumentException e) {
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
}
