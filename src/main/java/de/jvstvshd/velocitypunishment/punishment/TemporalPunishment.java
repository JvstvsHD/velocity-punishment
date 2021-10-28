package de.jvstvshd.velocitypunishment.punishment;

public interface TemporalPunishment extends Punishment {

    PunishmentDuration getDuration();

    boolean isPermanent();
}
