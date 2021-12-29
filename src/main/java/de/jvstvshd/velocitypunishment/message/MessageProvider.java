package de.jvstvshd.velocitypunishment.message;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface MessageProvider {

    @NotNull
    Component internalError();

    @NotNull
    Component prefix();

    @NotNull
    Component provide(String key, Player player, Component... args);

    @NotNull
    Component provide(String key, Component... args);

    @NotNull
    Component internalError(boolean withPrefix);

    @NotNull
    Component provide(String key, Player player, boolean withPrefix, Component... args);
}
