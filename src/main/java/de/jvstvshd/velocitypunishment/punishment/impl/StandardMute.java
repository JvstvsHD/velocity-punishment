package de.jvstvshd.velocitypunishment.punishment.impl;

import de.jvstvshd.velocitypunishment.punishment.*;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StandardMute extends AbstractTemporalPunishment implements Mute {

    public StandardMute(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager, PunishmentDuration duration) {
        super(playerUuid, reason, dataSource, service, punishmentManager, duration);
    }

    public StandardMute(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager, PunishmentDuration duration, UUID punishmentUuid) {
        super(playerUuid, reason, dataSource, service, punishmentManager, duration, punishmentUuid);
    }

    @Override
    public boolean isOngoing() {
        return getDuration().expiration().isAfter(LocalDateTime.now());
    }

    @Override
    public CompletableFuture<Boolean> punish() {
        checkValidity();
        return executeAsync(() -> {
            try (Connection connection = getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(APPLY_PUNISHMENT)) {
                statement.setString(1, Util.trimUuid(getPlayerUuid()));
                statement.setString(2, getPunishmentManager().getPlayerName(getPlayerUuid(),
                        Executors.newSingleThreadExecutor()).get(5, TimeUnit.SECONDS).toLowerCase());
                statement.setString(3, getType().getName());
                statement.setTimestamp(4, getDuration().timestampExpiration());
                statement.setString(5, convertReason(getReason()));
                statement.setString(6, Util.trimUuid(getPunishmentUuid()));
                return statement.executeUpdate() > 0;
            }
        }, getService());
    }

    @Override
    public CompletableFuture<Boolean> annul() {
        checkValidity();
        return executeAsync(() -> {
            try (Connection connection = getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(APPLY_ANNUL)) {
                statement.setString(1, Util.trimUuid(getPunishmentUuid()));
                return statement.executeUpdate() > 0;
            }
        }, getService());
    }

    @Override
    public CompletableFuture<Punishment> change(PunishmentDuration newDuration, Component newReason) {
        checkValidity();
        return executeAsync(() -> {
            try (Connection connection = getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(APPLY_CHANGE)) {
                statement.setString(1, convertReason(newReason));
                statement.setTimestamp(2, Timestamp.valueOf(newDuration.expiration()));
                statement.setBoolean(3, newDuration.isPermanent());
                statement.setString(4, Util.trimUuid(getPunishmentUuid()));
            }
            return new StandardBan(getPlayerUuid(), newReason, getDataSource(), getService(), getPunishmentManager(), newDuration);
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

    private void updateMutedPlayer(UUID uuid) {
        checkValidity();
    }
}
