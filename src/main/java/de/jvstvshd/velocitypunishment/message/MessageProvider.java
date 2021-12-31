package de.jvstvshd.velocitypunishment.message;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface MessageProvider {

    @NotNull
    Component internalError(CommandSource source);

    @NotNull
    Component prefix(CommandSource source);

    @NotNull
    Component prefix();

    @NotNull
    Component provide(String key, CommandSource source, Component... args);

    @NotNull
    Component provide(String key, Component... args);

    @NotNull
    Component internalError(CommandSource source, boolean withPrefix);

    @NotNull
    Component provide(String key, CommandSource source, boolean withPrefix, Component... args);
}
