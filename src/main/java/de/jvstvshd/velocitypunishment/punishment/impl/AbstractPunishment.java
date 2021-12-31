package de.jvstvshd.velocitypunishment.punishment.impl;

import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.punishment.util.PlayerResolver;
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
    private final PunishmentManager punishmentManager;
    private final PlayerResolver playerResolver;

    protected final static String APPLY_PUNISHMENT = "INSERT INTO velocity_punishment" +
            " (uuid, name, type, expiration, reason, punishment_id) VALUES (?, ?, ?, ?, ?, ?)";
    protected final static String APPLY_ANNUL = "DELETE FROM velocity_punishment WHERE punishment_id = ?";
    @SuppressWarnings("GrazieInspection")
    protected final static String APPLY_CHANGE = "UPDATE velocity_punishment SET reason = ?, expiration = ?, permanent = ? WHERE punishment_id = ?";
    private boolean validity;

    public AbstractPunishment(UUID playerUuid, Component reason, DataSource dataSource, PlayerResolver playerResolver, PunishmentManager punishmentManager, ExecutorService service) {
        this(playerUuid, reason, dataSource, service, punishmentManager, UUID.randomUUID(), playerResolver);
    }

    public AbstractPunishment(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver) {
        this.reason = reason;
        this.dataSource = dataSource;
        this.service = service;
        this.playerUuid = playerUuid;
        this.punishmentManager = punishmentManager;
        this.punishmentUuid = punishmentUuid;
        this.playerResolver = playerResolver;
        this.validity = true;
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

    public PunishmentManager getPunishmentManager() {
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

    protected void setValidity(boolean validity) {
        this.validity = validity;
    }

    protected void checkValidity() {
        if (!isValid()) {
            throw new IllegalStateException("punishment is invalid");
        }
    }

    public PlayerResolver getPlayerResolver() {
        return playerResolver;
    }
}
