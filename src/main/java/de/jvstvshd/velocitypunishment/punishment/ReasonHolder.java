package de.jvstvshd.velocitypunishment.punishment;

import net.kyori.adventure.text.Component;

public interface ReasonHolder {

    /**
     * @return the reason of this punishment as as component
     */
    Component getReason();

    /**
     * Creates the full reason inclusive when the ban ends (or that the ban is permanent).
     * @return the full reason with all information.
     */
    Component createFullReason();
}
