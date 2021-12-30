package de.jvstvshd.velocitypunishment.punishment;

/**
 * An interface containing some methods to only punish a player for a defined duration.
 *
 * @see Ban
 * @see Mute
 */
public interface TemporalPunishment extends Punishment {

    /**
     * @return the duration of the underlying punishment
     */
    PunishmentDuration getDuration();

    /**
     * @return true if the punishment is permanent, otherwise false
     * @see PunishmentDuration#isPermanent()
     */
    boolean isPermanent();
}
