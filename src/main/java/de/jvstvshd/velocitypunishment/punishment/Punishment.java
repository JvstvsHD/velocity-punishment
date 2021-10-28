package de.jvstvshd.velocitypunishment.punishment;

import net.kyori.adventure.text.Component;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Super interface for all sort of punishments. The super class for all standard implementations is {@link de.jvstvshd.velocitypunishment.punishment.impl.AbstractPunishment}.<br>
 * To punish a player, use one of the methods starting with "create" from the {@link PunishmentManager}.<br>
 * Example:<br>
 * <pre>{@code
 *     //punishment manager, for example StandardPunishmentManager
 *     PunishmentManager punishmentManager;
 *     //the uuid of the player, for example either from Player#getUniqueId() or from PunishmentManager#getPlayerUuid(String, ExecutorService);
 *     UUID playerUuid;
 *     //parse the duration of the punishment from a string in the format [number, ranging from 0 to Long.MAX_VALUE] and one char for s[econd], m[inute], h[our], d[ay].
 *     PunishmentDuration duration = PunishmentDuration.parse("1d");
 *     //Create a reason as an adventure component.
 *     Component reason = Component.text("You are banned from this server!");
 *     //Create the ban (or another punishment) with the corresponding method(s).
 *     Ban ban = punishmentManager.createBan(playerUuid, reason, duration);
 *     //Execute Punishment#punish() to perform the punishment finally.
 *     ban.punish();
 * }</pre>
 */
public interface Punishment {

    /**
     * Determines whether the expiration of this punishment is after {@link LocalDateTime#now()} or not.
     * @return true, if {@link java.time.LocalDateTime#isAfter(ChronoLocalDateTime)} returns true, otherwise false
     */
    boolean isOngoing();

    /**
     * Punishes the player finally.
     * @return true, if the action was successfully or false if not wrapped in a {@link CompletableFuture}.
     *
     */
    CompletableFuture<Boolean> punish();

    /**
     * Annuls this punishment.
     * @return true, if the action was successfully or false if not wrapped in a {@link CompletableFuture}.
     * @throws UnsupportedOperationException if this action is not supported (example: {@link Kick#annul()})
     */
    CompletableFuture<Boolean> annul();

    /**
     * Changes the duration and reason of this punishment. This method can be used if a player created an appeal an it was accepted.
     * @param newDuration the new duration of this punishment
     * @param newReason the new reason which should be displayed to the player
     * @return the new created punishment
     * @throws UnsupportedOperationException if this action is not supported (example: {@link Kick#change(PunishmentDuration, Component)})
     */
    CompletableFuture<Punishment> change(PunishmentDuration newDuration, Component newReason);

    /**
     * Returns the type of this punishment. By default, this is a field from {@link StandardPunishmentType}.
     * @return the type from this punishment
     * @see StandardPunishmentType
     */
    PunishmentType getType();

    /**
     * @return the reason of this punishment as as component
     */
    Component getReason();

    /**
     * @return the id of this punishment.
     */
    UUID getPunishmentUuid();
}
