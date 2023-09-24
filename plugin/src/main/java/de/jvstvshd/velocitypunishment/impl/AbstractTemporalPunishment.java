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

import de.jvstvshd.velocitypunishment.api.PunishmentException;
import de.jvstvshd.velocitypunishment.api.duration.PunishmentDuration;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.StandardPunishmentType;
import de.jvstvshd.velocitypunishment.api.punishment.TemporalPunishment;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import de.jvstvshd.velocitypunishment.common.plugin.MuteData;
import net.kyori.adventure.text.Component;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractTemporalPunishment extends AbstractPunishment implements TemporalPunishment {

    private final PunishmentDuration duration;

    public AbstractTemporalPunishment(UUID playerUuid, Component reason, DataSource dataSource, PlayerResolver playerResolver, DefaultPunishmentManager punishmentManager, ExecutorService service, PunishmentDuration duration, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, playerResolver, punishmentManager, service, messageProvider);
        this.duration = duration;
    }

    public AbstractTemporalPunishment(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, DefaultPunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver, PunishmentDuration duration, MessageProvider messageProvider) {
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
    public CompletableFuture<Punishment> change(PunishmentDuration newDuration, Component newReason) throws PunishmentException {
        if (!getType().isBan() && !getType().isMute()) {
            throw new IllegalStateException("only bans and mutes can be changed");
        }
        return builder()
                .query(APPLY_CHANGE)
                .parameter(paramBuilder -> paramBuilder.setString(convertReason(newReason))
                        .setTimestamp(newDuration.expirationAsTimestamp())
                ).update()
                .send()
                .thenApply(result -> {
                    if (getType().isBan()) {
                        return new DefaultBan(getPlayerUuid(), newReason, getDataSource(), getPlayerResolver(), getPunishmentManager(), getService(), newDuration, getMessageProvider());
                    } else if (getType().isMute()) {
                        var newMute = new DefaultMute(getPlayerUuid(), newReason, getDataSource(), getPlayerResolver(), getPunishmentManager(), getService(), newDuration, getMessageProvider());
                        newMute.queueMute(MuteData.UPDATE);
                        return newMute;
                    } else {
                        throw new IllegalStateException("punishment type is not a ban or mute");
                    }
                });
    }

    @Override
    public CompletableFuture<Punishment> cancel() throws PunishmentException {
        return builder()
                .query(APPLY_CANCELLATION)
                .parameter(paramBuilder -> paramBuilder.setUuidAsString(getPunishmentUuid()))
                .delete()
                .send().thenApply(updateResult -> this);
    }

    @Override
    public abstract StandardPunishmentType getType();

    @Override
    public CompletableFuture<Punishment> punish() throws PunishmentException {
        checkValidity();
        var duration = getDuration().absolute();
        return executeAsync(() -> {
            builder()
                    .query(APPLY_PUNISHMENT)
                    .parameter(paramBuilder -> paramBuilder.setUuidAsString(getPlayerUuid())
                            .setString(getPlayerResolver().getOrQueryPlayerName(getPlayerUuid(),
                                    Executors.newSingleThreadExecutor()).join().toLowerCase())
                            .setString(getType().getName())
                            .setTimestamp(duration.expirationAsTimestamp())
                            .setString(convertReason(getReason()))
                            .setUuidAsString(getPunishmentUuid())
                    ).insert()
                    .sendSync();
            return this;
        }, getService());
    }
}
