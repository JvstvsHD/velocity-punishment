package de.jvstvshd.velocitypunishment.punishment.impl;

import de.jvstvshd.velocitypunishment.punishment.PunishmentDuration;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.punishment.TemporalPunishment;
import de.jvstvshd.velocitypunishment.util.PlayerResolver;
import net.kyori.adventure.text.Component;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public abstract class AbstractTemporalPunishment extends AbstractPunishment implements TemporalPunishment {

    private final PunishmentDuration duration;

    public AbstractTemporalPunishment(UUID playerUuid, Component reason, DataSource dataSource, PlayerResolver playerResolver, PunishmentManager punishmentManager, ExecutorService service, PunishmentDuration duration) {
        super(playerUuid, reason, dataSource, playerResolver, punishmentManager, service);
        this.duration = duration;
    }

    public AbstractTemporalPunishment(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver, PunishmentDuration duration) {
        super(playerUuid, reason, dataSource, service, punishmentManager, punishmentUuid, playerResolver);
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
}
