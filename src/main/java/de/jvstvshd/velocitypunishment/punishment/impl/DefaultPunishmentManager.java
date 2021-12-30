package de.jvstvshd.velocitypunishment.punishment.impl;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariDataSource;
import de.jvstvshd.velocitypunishment.punishment.*;
import de.jvstvshd.velocitypunishment.util.PlayerResolver;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static de.jvstvshd.velocitypunishment.util.Util.executeAsync;

public class DefaultPunishmentManager implements PunishmentManager {

    private final ProxyServer proxyServer;
    private final HikariDataSource dataSource;
    private final ExecutorService service = Executors.newCachedThreadPool();
    private final PlayerResolver playerResolver;

    private static final String QUERY_PUNISHMENT_WITH_ID = "SELECT uuid, name, type, expiration, reason FROM velocity_punishment WHERE punishment_id = ?";
    private static final String SELECT_PUNISHMENT_WITH_TYPE = "SELECT expiration, reason, punishment_id FROM velocity_punishment WHERE uuid = ? AND type = ?";

    public DefaultPunishmentManager(ProxyServer proxyServer, HikariDataSource dataSource, PlayerResolver playerResolver) {
        this.proxyServer = proxyServer;
        this.dataSource = dataSource;
        this.playerResolver = playerResolver;
    }

    @Override
    public Ban createBan(UUID player, Component reason, PunishmentDuration duration) {
        return new DefaultBan(player, reason, dataSource, playerResolver, this, service, duration);
    }

    @Override
    public Mute createMute(UUID player, Component reason, PunishmentDuration duration) {
        return new DefaultMute(player, reason, dataSource, playerResolver, this, service, duration);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<List<Punishment>> getPunishments(UUID player, Executor service, PunishmentType... types) {
        return executeAsync(() -> {
            List<StandardPunishmentType> typeList = types.length == 0 ? Arrays.stream(StandardPunishmentType.values()).toList() : getTypes(types);
            List<Punishment> punishments = new ArrayList<>();
            for (StandardPunishmentType standardPunishmentType : typeList) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(SELECT_PUNISHMENT_WITH_TYPE)) {
                    statement.setString(1, Util.trimUuid(player));
                    statement.setString(2, standardPunishmentType.getName());
                    punishments.addAll(getPunishments(statement.executeQuery(), standardPunishmentType, player));
                }
            }
            return ImmutableList.copyOf(punishments);
        }, service);
    }

    private List<Punishment> getPunishments(ResultSet resultSet, StandardPunishmentType type, UUID uuid) {
        try {
            List<Punishment> punishments = new ArrayList<>();
            while (resultSet.next()) {
                final Timestamp timestamp = resultSet.getTimestamp(1);
                final PunishmentDuration duration = PunishmentDuration.fromTimestamp(timestamp);
                final Component reason = LegacyComponentSerializer.legacySection().deserialize(resultSet.getString(2));
                final UUID punishmentUuid = Util.parseUuid(resultSet.getString(3));
                Punishment punishment;
                switch (type) {
                    case BAN, PERMANENT_BAN -> punishment = new DefaultBan(uuid, reason, dataSource, service, this, punishmentUuid, playerResolver, duration);
                    case MUTE, PERMANENT_MUTE -> punishment = new DefaultMute(uuid, reason, dataSource, service, this, punishmentUuid, playerResolver, duration);
                    case KICK -> punishment = new DefaultKick(uuid, reason, dataSource, service, this, punishmentUuid, playerResolver);
                    default -> throw new UnsupportedOperationException("unhandled punishment type: " + type.getName());
                }
                punishments.add(punishment);
            }
            return punishments;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ImmutableList.of();
    }

    @SuppressWarnings("unchecked")
    private <T extends Punishment> T getPunishment(ResultSet resultSet, StandardPunishmentType type, UUID punishmentUuid, int uuidIndex,
                                                   int timestampIndex, int reasonIndex) throws SQLException {
        final UUID uuid = Util.parseUuid(resultSet.getString(uuidIndex));
        PunishmentDuration duration = null;
        if (timestampIndex != -1) {
            final Timestamp timestamp = resultSet.getTimestamp(timestampIndex);
            duration = PunishmentDuration.fromTimestamp(timestamp);
        }
        final Component reason = LegacyComponentSerializer.legacySection().deserialize(resultSet.getString(reasonIndex));
        return (T) switch (type) {
            case BAN, PERMANENT_BAN -> new DefaultBan(uuid, reason, dataSource, service, this, punishmentUuid, playerResolver, duration);
            case MUTE, PERMANENT_MUTE -> new DefaultMute(uuid, reason, dataSource, service, this, punishmentUuid, playerResolver, duration);
            case KICK -> new DefaultKick(uuid, reason, dataSource, service, this, punishmentUuid, playerResolver);
        };
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends Punishment> T getPunishment(ResultSet resultSet, UUID punishmentId, int uuidIndex, int timestampIndex, int reasonIndex,
                                                   int typeIndex) throws SQLException {
        return getPunishment(resultSet, StandardPunishmentType.valueOf(resultSet.getString(typeIndex).toUpperCase(Locale.ROOT)),
                punishmentId, uuidIndex, timestampIndex, reasonIndex);
    }


    private List<StandardPunishmentType> getTypes(PunishmentType... types) {
        ArrayList<StandardPunishmentType> vTypes = new ArrayList<>();
        for (PunishmentType punishmentType : types) {
            if (punishmentType instanceof StandardPunishmentType vType)
                vTypes.add(vType);
            else
                throw new IllegalArgumentException("Invalid punishment type: " + punishmentType + ", class: " + punishmentType.getClass());
        }
        return ImmutableList.copyOf(vTypes);
    }

    @Override
    public ProxyServer getServer() {
        return proxyServer;
    }

    @Override
    public <T extends Punishment> CompletableFuture<Optional<T>> getPunishment(UUID punishmentId, Executor service) {
        return executeAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(QUERY_PUNISHMENT_WITH_ID)) {
                statement.setString(1, Util.trimUuid(punishmentId));
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(getPunishment(rs, punishmentId, 1, 4, 5, 3));
                } else {
                    return Optional.empty();
                }
            }
        }, service);
    }

    @Override
    public CompletableFuture<Boolean> isBanned(UUID playerUuid, Executor service) {
        return executeAsync(() -> !getPunishments(playerUuid, service, StandardPunishmentType.BAN, StandardPunishmentType.PERMANENT_BAN).get().isEmpty(), service);
    }
}
