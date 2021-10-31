package de.jvstvshd.velocitypunishment.punishment.impl;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariDataSource;
import de.jvstvshd.velocitypunishment.punishment.*;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.util.Util.executeAsync;

public class StandardPunishmentManager implements PunishmentManager {

    private final ProxyServer proxyServer;
    private final HikariDataSource dataSource;
    private final ExecutorService service = Executors.newCachedThreadPool();

    private static final String QUERY_PUNISHMENT_WITH_ID = "SELECT uuid, name, type, expiration, reason FROM velocity_punishment WHERE punishment_id = ?";
    private static final String SELECT_PUNISHMENT_WITH_TYPE = "SELECT expiration, reason, punishment_id FROM velocity_punishment WHERE uuid = ? AND type = ?";

    public StandardPunishmentManager(ProxyServer proxyServer, HikariDataSource dataSource) {
        this.proxyServer = proxyServer;
        this.dataSource = dataSource;
    }

    @Override
    public Ban createBan(UUID player, Component reason, PunishmentDuration duration) {
        return new StandardBan(player, reason, dataSource, service, this, duration);
    }

    @Override
    public Mute createMute(UUID player, Component reason, PunishmentDuration duration) {
        return new StandardMute(player, reason, dataSource, service, this, duration);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<List<Punishment>> getPunishments(UUID player, ExecutorService service, PunishmentType... types) {
        return executeAsync(() -> {
            List<StandardPunishmentType> typeList = types.length == 0 ? Arrays.stream(StandardPunishmentType.values()).collect(Collectors.toList()) : getTypes(types);
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
                final UUID punishmentUuid = Util.parse(resultSet.getString(3));
                Punishment punishment;
                switch (type) {
                    case BAN, PERMANENT_BAN -> punishment = new StandardBan(uuid, reason, dataSource, service, this, duration, punishmentUuid);
                    case MUTE, PERMANENT_MUTE -> punishment = new StandardMute(uuid, reason, dataSource, service, this, duration, punishmentUuid);
                    case KICK -> punishment = new StandardKick(uuid, reason, dataSource, service, this, punishmentUuid);
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

    private Punishment getPunishment(ResultSet resultSet, StandardPunishmentType type, int uuidIndex, int timestampIndex,
                                     int reasonIndex, int punishmentIdIndex) throws SQLException {
        final UUID punishmentUuid = Util.parse(resultSet.getString(punishmentIdIndex));
        return getPunishment(resultSet, type, punishmentUuid, uuidIndex, timestampIndex, reasonIndex);
    }

    @SuppressWarnings("unchecked")
    private <T extends Punishment> T getPunishment(ResultSet resultSet, StandardPunishmentType type, UUID punishmentUuid, int uuidIndex,
                                                   int timestampIndex, int reasonIndex) throws SQLException {
        final UUID uuid = Util.parse(resultSet.getString(uuidIndex));
        PunishmentDuration duration = null;
        if (timestampIndex != -1) {
            final Timestamp timestamp = resultSet.getTimestamp(timestampIndex);
            duration = PunishmentDuration.fromTimestamp(timestamp);
        }
        final Component reason = LegacyComponentSerializer.legacySection().deserialize(resultSet.getString(reasonIndex));
        return (T) switch (type) {
            case BAN, PERMANENT_BAN -> new StandardBan(uuid, reason, dataSource, service, this, duration, punishmentUuid);
            case MUTE, PERMANENT_MUTE -> new StandardMute(uuid, reason, dataSource, service, this, duration, punishmentUuid);
            case KICK -> new StandardKick(uuid, reason, dataSource, service, this, punishmentUuid);
        };
    }

    private Punishment getPunishment(ResultSet resultSet, int uuidIndex, int timestampIndex, int reasonIndex,
                                     int punishmentIdIndex, int typeIndex) throws SQLException {
        return getPunishment(resultSet, StandardPunishmentType.valueOf(resultSet.getString(typeIndex).toUpperCase(Locale.ROOT)),
                uuidIndex, timestampIndex, reasonIndex, punishmentIdIndex);
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
    public CompletableFuture<String> getPlayerName(UUID uuid, ExecutorService service) {
        if (proxyServer == null)
            return getPlayerNameMojangApi(uuid, service);
        Optional<Player> optPlayer = proxyServer.getPlayer(uuid);
        if (optPlayer.isEmpty()) {
            return getPlayerNameMojangApi(uuid, service);
        }
        return CompletableFuture.completedFuture(optPlayer.get().getUsername());
    }

    @Override
    public CompletableFuture<String> getPlayerNameMojangApi(UUID uuid, ExecutorService service) {
        CompletableFuture<String> cf = new CompletableFuture<>();
        service.execute(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString())).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonElement jsonElement = JsonParser.parseString(response.body());
                cf.complete(jsonElement.getAsJsonObject().get("name").getAsString());
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    @Override
    public CompletableFuture<UUID> getPlayerUuid(String name, ExecutorService service) {
        if (proxyServer == null)
            return getPlayerUuidMojangApi(name, service);
        Optional<Player> optPlayer = proxyServer.getPlayer(name);
        if (optPlayer.isEmpty()) {
            return getPlayerUuidMojangApi(name, service);
        }
        return CompletableFuture.completedFuture(optPlayer.get().getUniqueId());
    }

    @Override
    public CompletableFuture<UUID> getPlayerUuidMojangApi(String name, ExecutorService service) {
        CompletableFuture<UUID> cf = new CompletableFuture<>();
        service.execute(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name)).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonElement jsonElement = JsonParser.parseString(response.body());
                if (jsonElement == null || jsonElement.isJsonNull()) {
                    cf.complete(null);
                    return;
                }
                cf.complete(Util.parse(jsonElement.getAsJsonObject().get("id").getAsString()));
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    @Override
    public <T extends Punishment> CompletableFuture<Optional<T>> getPunishment(UUID punishmentId, ExecutorService service) {
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
    public CompletableFuture<Boolean> isBanned(UUID playerUuid, ExecutorService service) {
        return executeAsync(() -> !getPunishments(playerUuid, service, StandardPunishmentType.BAN, StandardPunishmentType.PERMANENT_BAN).get().isEmpty(), service);
    }
}
