package de.jvstvshd.velocitypunishment.punishment.impl;

import com.velocitypowered.api.command.CommandSource;
import de.jvstvshd.velocitypunishment.internal.Util;
import de.jvstvshd.velocitypunishment.message.MessageProvider;
import de.jvstvshd.velocitypunishment.punishment.*;
import de.jvstvshd.velocitypunishment.punishment.util.PlayerResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DefaultMute extends AbstractTemporalPunishment implements Mute {

    public DefaultMute(UUID playerUuid, Component reason, DataSource dataSource, PlayerResolver playerResolver, PunishmentManager punishmentManager, ExecutorService service, PunishmentDuration duration, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, playerResolver, punishmentManager, service, duration, messageProvider);
    }

    public DefaultMute(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver, PunishmentDuration duration, MessageProvider messageProvider) {
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
                statement.setTimestamp(4, getDuration().timestampExpiration());
                statement.setString(5, convertReason(getReason()));
                statement.setString(6, Util.trimUuid(getPunishmentUuid()));
                statement.executeUpdate();
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
                return this;
            }
        }, getService());
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
            return getMessageProvider().provide("punishment.mute.temp.full-reason", source, true, Component.text(getDuration().getRemainingDuration()).color(NamedTextColor.YELLOW), getReason(), until);
        }
    }
}
