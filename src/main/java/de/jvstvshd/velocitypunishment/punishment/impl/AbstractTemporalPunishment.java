package de.jvstvshd.velocitypunishment.punishment.impl;

import de.jvstvshd.velocitypunishment.punishment.PunishmentDuration;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.punishment.TemporalPunishment;
import net.kyori.adventure.text.Component;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public abstract class AbstractTemporalPunishment extends AbstractPunishment implements TemporalPunishment {

    private final PunishmentDuration duration;

    public AbstractTemporalPunishment(UUID playerUuid, Component reason, DataSource dataSource,
                                      ExecutorService service, PunishmentManager punishmentManager,
                                      PunishmentDuration duration) {
        super(playerUuid, reason, dataSource, service, punishmentManager);
        this.duration = duration;
    }

    public AbstractTemporalPunishment(UUID playerUuid, Component reason, DataSource dataSource,
                                      ExecutorService service, PunishmentManager punishmentManager,
                                      PunishmentDuration duration, UUID punishmentUuid) {
        super(playerUuid, reason, dataSource, service, punishmentManager, punishmentUuid);
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
}
