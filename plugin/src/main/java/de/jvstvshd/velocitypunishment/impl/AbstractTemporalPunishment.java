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

import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.PunishmentDuration;
import de.jvstvshd.velocitypunishment.api.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.api.punishment.TemporalPunishment;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class AbstractTemporalPunishment extends AbstractPunishment implements TemporalPunishment {

    private final PunishmentDuration duration;

    public AbstractTemporalPunishment(UUID playerUuid, Component reason, DataSource dataSource, PlayerResolver playerResolver, PunishmentManager punishmentManager, ExecutorService service, PunishmentDuration duration, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, playerResolver, punishmentManager, service, messageProvider);
        this.duration = duration;
    }

    public AbstractTemporalPunishment(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver, PunishmentDuration duration, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, service, punishmentManager, punishmentUuid, playerResolver, messageProvider);
        this.duration = duration;
    }

    public PunishmentDuration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "AbstractTemporalPunishment{" +
                "duration=" + duration +
                "} " + super.toString();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && isOngoing();
    }

    @Override
    protected void checkValidity() {
        if (!isValid()) {
            throw new IllegalStateException("punishment is invalid (probably isOngoing returned false)");
        }
    }

    @Override
    public CompletableFuture<Punishment> change(PunishmentDuration newDuration, Component newReason) {
        return executeAsync(() -> {
            try (Connection connection = getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(APPLY_CHANGE)) {
                statement.setString(1, convertReason(newReason));
                statement.setTimestamp(2, Timestamp.valueOf(newDuration.expiration()));
                statement.setBoolean(3, newDuration.isPermanent());
                statement.setString(4, Util.trimUuid(getPunishmentUuid()));
                statement.executeUpdate();
            }
            return new DefaultMute(getPlayerUuid(), newReason, getDataSource(), getPlayerResolver(), getPunishmentManager(), getService(), newDuration, getMessageProvider());
        }, getService());
    }
}
