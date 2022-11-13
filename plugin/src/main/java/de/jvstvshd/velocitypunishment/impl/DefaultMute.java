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

package de.jvstvshd.velocitypunishment.impl;

import com.velocitypowered.api.command.CommandSource;
import de.jvstvshd.velocitypunishment.api.duration.PunishmentDuration;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.Mute;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.PunishmentType;
import de.jvstvshd.velocitypunishment.api.punishment.StandardPunishmentType;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import de.jvstvshd.velocitypunishment.common.plugin.MuteData;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DefaultMute extends AbstractTemporalPunishment implements Mute {

    public DefaultMute(UUID playerUuid, Component reason, DataSource dataSource, PlayerResolver playerResolver, DefaultPunishmentManager punishmentManager, ExecutorService service, PunishmentDuration duration, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, playerResolver, punishmentManager, service, duration, messageProvider);
    }

    public DefaultMute(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, DefaultPunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver, PunishmentDuration duration, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, service, punishmentManager, punishmentUuid, playerResolver, duration, messageProvider);
    }

    @Override
    public boolean isOngoing() {
        return getDuration().expiration().isAfter(LocalDateTime.now());
    }

    @Override
    public CompletableFuture<Punishment> punish() {
        checkValidity();
        getDuration().absolute();
        return executeAsync(() -> {
            try (Connection connection = getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(APPLY_PUNISHMENT)) {
                statement.setString(1, Util.trimUuid(getPlayerUuid()));
                statement.setString(2, getPlayerResolver().getOrQueryPlayerName(getPlayerUuid(),
                        Executors.newSingleThreadExecutor()).get(5, TimeUnit.SECONDS).toLowerCase());
                statement.setString(3, getType().getName());
                statement.setTimestamp(4, getDuration().expirationAsTimestamp());
                statement.setString(5, convertReason(getReason()));
                statement.setString(6, Util.trimUuid(getPunishmentUuid()));
                statement.executeUpdate();

                queueMute(MuteData.ADD);
                return this;
            }
        }, getService());
    }

    @Override
    public CompletableFuture<Punishment> cancel() {
        return executeAsync(() -> {
            try (Connection connection = getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(APPLY_ANNUL)) {
                statement.setString(1, Util.trimUuid(getPunishmentUuid()));
                statement.executeUpdate();
                queueMute(MuteData.REMOVE);
                return this;
            }
        }, getService());
    }

    @Override
    public PunishmentType getType() {
        return isPermanent() ? StandardPunishmentType.PERMANENT_MUTE : StandardPunishmentType.MUTE;
    }

    @Override
    public boolean isPermanent() {
        checkValidity();
        return getDuration().isPermanent();
    }

    @Override
    public Component createFullReason(CommandSource source) {
        if (!isValid()) {
            return Component.text("INVALID").decorate(TextDecoration.BOLD).color(NamedTextColor.DARK_RED);
        }
        if (isPermanent()) {
            return getMessageProvider().provide("punishment.mute.permanent.full-reason", source, true, getReason());
        } else {
            var until = Component.text(getDuration().expiration().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .color(NamedTextColor.YELLOW);
            return getMessageProvider().provide("punishment.mute.temp.full-reason", source, true, Component.text(getDuration().remainingDuration()).color(NamedTextColor.YELLOW), getReason(), until);
        }
    }
}
