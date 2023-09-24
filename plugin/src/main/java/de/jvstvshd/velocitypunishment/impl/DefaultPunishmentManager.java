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

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariDataSource;
import de.chojo.sadu.base.QueryFactory;
import de.chojo.sadu.wrapper.util.Row;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.duration.PunishmentDuration;
import de.jvstvshd.velocitypunishment.api.punishment.*;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static de.jvstvshd.velocitypunishment.internal.Util.executeAsync;

public class DefaultPunishmentManager extends QueryFactory implements PunishmentManager {

    protected static final String QUERY_PUNISHMENT_WITH_ID = "SELECT uuid, name, type, expiration, reason FROM velocity_punishment WHERE punishment_id = ?";
    protected static final String SELECT_PUNISHMENT_WITH_TYPE = "SELECT expiration, reason, punishment_id FROM velocity_punishment WHERE uuid = ? AND type = ?";

    private final ProxyServer proxyServer;
    private final HikariDataSource dataSource;
    private final ExecutorService service = Executors.newCachedThreadPool();
    private final VelocityPunishmentPlugin plugin;

    public DefaultPunishmentManager(ProxyServer proxyServer, HikariDataSource dataSource, VelocityPunishmentPlugin plugin) {
        super(dataSource);
        this.proxyServer = proxyServer;
        this.dataSource = dataSource;
        this.plugin = plugin;
    }

    @Override
    public Ban createBan(UUID player, Component reason, PunishmentDuration duration) {
        return new DefaultBan(player, reason, dataSource, plugin.getPlayerResolver(), this, service, duration, plugin.getMessageProvider());
    }

    @Override
    public Mute createMute(UUID player, Component reason, PunishmentDuration duration) {
        return new DefaultMute(player, reason, dataSource, plugin.getPlayerResolver(), this, service, duration, plugin.getMessageProvider());
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<List<Punishment>> getPunishments(UUID player, Executor service, PunishmentType... types) {
        return executeAsync(() -> {
            List<StandardPunishmentType> typeList = types.length == 0 ? Arrays.stream(StandardPunishmentType.values()).toList() : getTypes(types);
            List<Punishment> punishments = new ArrayList<>();
            for (StandardPunishmentType standardPunishmentType : typeList) {
                var list = builder(Punishment.class)
                        .query(SELECT_PUNISHMENT_WITH_TYPE)
                        .parameter(paramBuilder -> paramBuilder.setString(player.toString()).setString(standardPunishmentType.getName()))
                        .readRow(row -> getPunishments(row, standardPunishmentType, player)).all().get();
                punishments.addAll(list);
            }
            return ImmutableList.copyOf(punishments);
        }, service);
    }

    private Punishment getPunishments(Row row, StandardPunishmentType type, UUID uuid) throws SQLException {
        final Timestamp timestamp = row.getTimestamp(1);
        final PunishmentDuration duration = PunishmentDuration.fromTimestamp(timestamp);
        final Component reason = LegacyComponentSerializer.legacySection().deserialize(row.getString(2));
        final UUID punishmentUuid = Util.parseUuid(row.getString(3));
        Punishment punishment;
        switch (type) {
            case BAN, PERMANENT_BAN ->
                    punishment = new DefaultBan(uuid, reason, dataSource, service, this, punishmentUuid, plugin.getPlayerResolver(), duration, plugin.getMessageProvider());
            case MUTE, PERMANENT_MUTE ->
                    punishment = new DefaultMute(uuid, reason, dataSource, service, this, punishmentUuid, plugin.getPlayerResolver(), duration, plugin.getMessageProvider());
            case KICK ->
                    punishment = new DefaultKick(uuid, reason, dataSource, service, this, punishmentUuid, plugin.getPlayerResolver(), plugin.getMessageProvider());
            default -> throw new UnsupportedOperationException("unhandled punishment type: " + type.getName());
        }
        return punishment;
    }

    protected List<StandardPunishmentType> getTypes(PunishmentType... types) {
        ArrayList<StandardPunishmentType> vTypes = new ArrayList<>();
        for (PunishmentType punishmentType : types) {
            if (punishmentType instanceof StandardPunishmentType vType)
                vTypes.add(vType);
            else
                throw new IllegalArgumentException("Invalid punishment type: " + punishmentType + ", class: " + punishmentType.getClass());
        }
        return Collections.unmodifiableList(vTypes);
    }

    @SuppressWarnings("unchecked")
    private <T extends Punishment> T getPunishment(Row row, StandardPunishmentType type, UUID punishmentUuid, int uuidIndex,
                                                   int timestampIndex, int reasonIndex) throws SQLException {
        final UUID uuid = Util.parseUuid(row.getString(uuidIndex));
        PunishmentDuration duration = null;
        if (timestampIndex != -1) {
            final Timestamp timestamp = row.getTimestamp(timestampIndex);
            duration = PunishmentDuration.fromTimestamp(timestamp);
        }
        final Component reason = LegacyComponentSerializer.legacySection().deserialize(row.getString(reasonIndex));
        return (T) switch (type) {
            case BAN, PERMANENT_BAN ->
                    new DefaultBan(uuid, reason, dataSource, service, this, punishmentUuid, plugin.getPlayerResolver(), duration, plugin.getMessageProvider());
            case MUTE, PERMANENT_MUTE ->
                    new DefaultMute(uuid, reason, dataSource, service, this, punishmentUuid, plugin.getPlayerResolver(), duration, plugin.getMessageProvider());
            case KICK ->
                    new DefaultKick(uuid, reason, dataSource, service, this, punishmentUuid, plugin.getPlayerResolver(), plugin.getMessageProvider());
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Optional<Punishment>> getPunishment(UUID punishmentId, Executor service) {
        return builder(Punishment.class)
                .query(QUERY_PUNISHMENT_WITH_ID)
                .parameter(paramBuilder -> paramBuilder.setString(punishmentId.toString()))
                .readRow(row -> getPunishment(row, punishmentId, 1, 4, 5, 3))
                .first();
    }

    @SuppressWarnings("SameParameterValue")
    protected <T extends Punishment> T getPunishment(Row row, UUID punishmentId, int uuidIndex, int timestampIndex, int reasonIndex,
                                                     int typeIndex) throws SQLException {
        return getPunishment(row, StandardPunishmentType.valueOf(row.getString(typeIndex).toUpperCase(Locale.ROOT)),
                punishmentId, uuidIndex, timestampIndex, reasonIndex);
    }

    @Override
    public ProxyServer getServer() {
        return proxyServer;
    }

    public VelocityPunishmentPlugin plugin() {
        return plugin;
    }

    @Override
    public CompletableFuture<Boolean> isBanned(UUID playerUuid, Executor executor) {
        return executeAsync(() -> !getPunishments(playerUuid, executor, StandardPunishmentType.BAN, StandardPunishmentType.PERMANENT_BAN).get().isEmpty(), executor);
    }
}