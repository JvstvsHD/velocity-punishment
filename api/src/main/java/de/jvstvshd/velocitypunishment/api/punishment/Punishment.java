/*
 * This file is part of Velocity Punishment, which is licensed under the MIT license.
 *
 * Copyright (c) 2022 JvstvsHD
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.jvstvshd.velocitypunishment.api.punishment;

import de.jvstvshd.velocitypunishment.api.duration.PunishmentDuration;
import de.jvstvshd.velocitypunishment.api.punishment.util.ReasonHolder;
import net.kyori.adventure.text.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Super interface for all sort of punishments..<br>
 * To punish a player, use one of the methods starting with "create" from the {@link PunishmentManager}.<br>
 * Example:<br>
 * <pre>{@code
 *     //punishment manager, for example DefaultPunishmentManager
 *     PunishmentManager punishmentManager;
 *     //the uuid of the player, for example either from Player#getUniqueId() or from PlayerResolver#getOrQueryPlayerUuid(String, Executor)
 *     UUID playerUuid;
 *     //parse the duration of the punishment from a string in the format [number, ranging from 0 to Long.MAX_VALUE] and one char for s [second], m[minute], h[our], d[ay].
 *     PunishmentDuration duration = PunishmentDuration.parse("1d");
 *     //Create a reason as an adventure component.
 *     Component reason = Component.text("You are banned from this server!");
 *     //Create the ban (or another punishment) with the corresponding method(s).
 *     Ban ban = punishmentManager.createBan(playerUuid, reason, duration);
 *     //Execute Punishment#punish() to perform the punishment finally.
 *     ban.punish();
 * }</pre>
 */
public interface Punishment extends ReasonHolder {

    /**
     * Determines whether the expiration of this punishment is after {@link LocalDateTime#now()} or not.
     *
     * @return true, if the expiration date is after now, otherwise false
     */
    boolean isOngoing();

    /**
     * Punishes the player finally.
     *
     * @return a {@link CompletableFuture} containing the exerted punishment
     */
    CompletableFuture<Punishment> punish();

    /**
     * Cancels this punishment thus allowing the player e.g. to join the server
     *
     * @return a {@link CompletableFuture} containing the cancelled punishment
     */
    CompletableFuture<Punishment> cancel();

    /**
     * Changes the duration and reason of this punishment. This method can be used if a player created an appeal an it was accepted.
     *
     * @param newDuration the new duration of this punishment
     * @param newReason   the new reason which should be displayed to the player
     * @return a {@link CompletableFuture} containing the new punishment
     * @see #cancel()
     */
    CompletableFuture<Punishment> change(PunishmentDuration newDuration, Component newReason);

    /**
     * Returns the type of this punishment. By default, this is a field from {@link StandardPunishmentType}.
     * @return the type of this punishment
     * @see StandardPunishmentType
     */
    PunishmentType getType();

    /**
     * @return the id of this punishment.
     */
    UUID getPunishmentUuid();
}
