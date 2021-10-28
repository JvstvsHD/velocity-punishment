package de.jvstvshd.velocitypunishment.punishment;

import net.kyori.adventure.text.Component;

public interface Ban extends TemporalPunishment {

    /**
     * Creates the full reason inclusive when the ban ends (or that the ban is permanent).
     * @return the full reason with all information.
     */
    Component createFullReason();
}
