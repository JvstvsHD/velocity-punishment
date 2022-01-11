package de.jvstvshd.velocitypunishment.api.punishment;

import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Super interface for all kick implementation.<br>
 * Unsupported operations:
 * <ul>
 *     <li>{@link #cancel()}</li>
 *     <li>{@link #change(PunishmentDuration, Component)}</li>
 * </ul>
 *
 * @see com.velocitypowered.api.proxy.Player#disconnect(Component)
 */
public interface Kick extends Punishment {

    @Override
    default boolean isOngoing() {
        return false;
    }

    @Override
    default CompletableFuture<Punishment> cancel() {
        throw new UnsupportedOperationException("Cannot annul kick since a kick lasts only one moment");
    }

    @Override
    default CompletableFuture<Punishment> change(PunishmentDuration newDuration, Component newReason) {
        throw new UnsupportedOperationException("Cannot change a kick lasts only one moment");
    }
}
