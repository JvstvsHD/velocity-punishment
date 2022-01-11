package de.jvstvshd.velocitypunishment.api.punishment.util;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;

public interface ReasonHolder {

    /**
     * @return the reason of this punishment as as component
     */
    Component getReason();

    /**
     * Creates the full reason inclusive when the ban ends (or that the ban is permanent).
     *
     * @param source command source this reason should be displayed to
     * @return the full reason with all information.
     */
    Component createFullReason(CommandSource source);
}
