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
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class AbstractPunishment implements Punishment {

    private final Component reason;
    private final DataSource dataSource;
    private final ExecutorService service;
    private final UUID playerUuid;
    private final UUID punishmentUuid;
    private final DefaultPunishmentManager punishmentManager;
    private final PlayerResolver playerResolver;
    private final MessageProvider messageProvider;

    protected final static String APPLY_PUNISHMENT = "INSERT INTO velocity_punishment" +
            " (uuid, name, type, expiration, reason, punishment_id) VALUES (?, ?, ?, ?, ?, ?)";
    protected final static String APPLY_ANNUL = "DELETE FROM velocity_punishment WHERE punishment_id = ?";
    protected final static String APPLY_CHANGE = "UPDATE velocity_punishment SET reason = ?, expiration = ?, permanent = ? WHERE punishment_id = ?";
    private boolean validity;

    public AbstractPunishment(UUID playerUuid, Component reason, DataSource dataSource, PlayerResolver playerResolver, DefaultPunishmentManager punishmentManager, ExecutorService service, MessageProvider messageProvider) {
        this(playerUuid, reason, dataSource, service, punishmentManager, UUID.randomUUID(), playerResolver, messageProvider);
    }

    public AbstractPunishment(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, DefaultPunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver, MessageProvider messageProvider) {
        this.reason = reason;
        this.dataSource = dataSource;
        this.service = service;
        this.playerUuid = playerUuid;
        this.punishmentManager = punishmentManager;
        this.punishmentUuid = punishmentUuid;
        this.playerResolver = playerResolver;
        this.validity = true;
        this.messageProvider = messageProvider;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Component getReason() {
        return reason;
    }

    public ExecutorService getService() {
        return service;
    }

    protected <T> CompletableFuture<T> executeAsync(Callable<T> task, ExecutorService executorService) {
        var future = new CompletableFuture<T>();
        executorService.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public DefaultPunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public UUID getPunishmentUuid() {
        return punishmentUuid;
    }

    protected String convertReason(Component component) {
        return LegacyComponentSerializer.legacy(LegacyComponentSerializer.SECTION_CHAR).serialize(component);
    }

    public boolean isValid() {
        return validity;
    }

    @Override
    public String toString() {
        return "AbstractPunishment{" +
                "reason=" + reason +
                ", dataSource=" + dataSource +
                ", service=" + service +
                ", playerUuid=" + playerUuid +
                ", punishmentUuid=" + punishmentUuid +
                ", punishmentManager=" + punishmentManager +
                '}';
    }

    protected void checkValidity() {
        if (!isValid()) {
            throw new IllegalStateException("punishment is invalid");
        }
    }

    public PlayerResolver getPlayerResolver() {
        return playerResolver;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    @Override
    public UUID getUuid() {
        return playerUuid;
    }
}
