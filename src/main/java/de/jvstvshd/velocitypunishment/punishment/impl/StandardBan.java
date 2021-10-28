package de.jvstvshd.velocitypunishment.punishment.impl;

import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.punishment.*;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StandardBan extends AbstractTemporalPunishment implements Ban {

    public StandardBan(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service,
                       PunishmentManager punishmentManager, PunishmentDuration duration) {
        super(playerUuid, reason, dataSource, service, punishmentManager, duration);
    }

    public StandardBan(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service,
                       PunishmentManager punishmentManager, PunishmentDuration duration, UUID punishmentUuid) {
        super(playerUuid, reason, dataSource, service, punishmentManager, duration, punishmentUuid);
    }

    @Override
    public boolean isOngoing() {
        return getDuration().expiration().isAfter(LocalDateTime.now());
    }

    @Override
    public CompletableFuture<Boolean> punish() {
        return executeAsync(() -> {
            tryKick();
            try (Connection connection = getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(APPLY_PUNISHMENT)) {
                statement.setString(1, Util.trimUuid(getPlayerUuid()));
                statement.setString(2, getPunishmentManager().getPlayerName(getPlayerUuid(),
                        Executors.newSingleThreadExecutor()).get(5, TimeUnit.SECONDS).toLowerCase());
                statement.setString(3, getType().name());
                statement.setTimestamp(4, getDuration().timestampExpiration());
                statement.setString(5, convertReason(getReason()));
                statement.setString(6, Util.trimUuid(getPunishmentUuid()));
                return statement.executeUpdate() > 0;
            }
        }, getService());
    }

    @Override
    public CompletableFuture<Boolean> annul() {
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
        return executeAsync(() -> {
            tryKick();
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
    public StandardPunishmentType getType() {
        return getDuration().isPermanent() ? StandardPunishmentType.PERMANENT_BAN : StandardPunishmentType.BAN;
    }

    private void tryKick() {
        Optional<Player> optionalPlayer = getPunishmentManager().getServer().getPlayer(getPlayerUuid());
        if (optionalPlayer.isEmpty())
            return;
        optionalPlayer.get().disconnect(createFullReason());
    }

    @Override
    public Component createFullReason() {
        if (isPermanent()) {
            return Component.text().append(Component.text("You have been permanently banned from this server.\n\n")
                                    .color(NamedTextColor.DARK_RED),
                            Component.text("Reason: \n").color(NamedTextColor.RED),
                            getReason())
                    .build();
        } else {
            return Component.text().append(Component.text("You are banned for ").color(NamedTextColor.DARK_RED),
                    Component.text(getDuration().getRemainingDuration()).color(NamedTextColor.YELLOW),

                    Component.text(".\n\n").color(NamedTextColor.DARK_RED),
                    Component.text("Reason: \n").color(NamedTextColor.RED),
                    getReason(),
                    Component.text("\n\nEnd of punishment: ").color(NamedTextColor.RED),
                    Component.text(getDuration().expiration().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .color(NamedTextColor.YELLOW)).build();
        }
    }

    @Override
    public boolean isPermanent() {
        return getDuration().isPermanent();
    }
}
