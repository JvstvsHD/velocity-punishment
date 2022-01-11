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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An interface for managing punishments.
 *
 * @see Punishment
 */
public interface PunishmentManager {

    /**
     * Prepares a ban for a player with custom reason and duration.
     *
     * @param player   the uuid of the player which should be banned (by either {@link Player#getUniqueId()} ()} or {@link PlayerResolver#getPlayerUuid(String)}.
     * @param reason   the reason given as a {@link Component}.
     * @param duration the duration, which can be created via {@link PunishmentDuration#parse(String)} when it's source is from minecraft commands.
     * @return the prepared ban with the given reason and duration. This duration remains the same at any duration since it is only added when the player is banned.
     * Only {@link Ban#punish()} is needed to execute the punishment.
     */
    Ban createBan(UUID player, Component reason, PunishmentDuration duration);

    /**
     * Prepares a ban for a player with custom reason and duration. Its only difference to {@link #createBan(UUID, Component, PunishmentDuration)} is, that the {@link PunishmentDuration}
     * is {@link PunishmentDuration#permanent()}.
     *
     * @param player the uuid of the player which should be banned (by either {@link Player#getUniqueId()} ()} or {@link PlayerResolver#getPlayerUuid(String)}.
     * @param reason the reason given as a {@link Component}.
     * @return the prepared ban with the given reason and duration. The duration is permanent, equals {@link java.time.LocalDateTime#MAX}
     * Only {@link Ban#punish()} is needed to execute the punishment.
     */
    default Ban createPermanentBan(UUID player, Component reason) {
        return createBan(player, reason, PunishmentDuration.permanent());
    }

    /**
     * Prepares a mute for a player with custom reason and duration.
     *
     * @param player   the uuid of the player which should be banned (by either {@link Player#getUniqueId()} ()} or {@link PlayerResolver#getPlayerUuid(String)}.
     * @param reason   the reason given as a {@link Component}.
     * @param duration the duration, which can be created via {@link PunishmentDuration#parse(String)} when it's source is from minecraft commands.
     * @return the prepared ban with the given reason and duration. This duration remains the same at any duration since it is only added when the player is banned.
     * Only {@link Mute#punish()} is needed to execute the punishment.
     */
    Mute createMute(UUID player, Component reason, PunishmentDuration duration);

    /**
     * Prepares a ban for a player with custom reason and duration. Its only difference to {@link #createMute(UUID, Component, PunishmentDuration)} is, that the {@link PunishmentDuration}
     * is {@link PunishmentDuration#permanent()}.
     *
     * @param player the uuid of the player which should be banned (by either {@link Player#getUniqueId()} ()} or {@link PlayerResolver#getPlayerUuid(String)}
     * @param reason the reason given as a {@link Component}.
     * @return the prepared ban with the given reason and duration. The duration is permanent, equals {@link java.time.LocalDateTime#MAX}
     * Only {@link Mute#punish()} is needed to execute the punishment.
     */
    default Mute createPermanentMute(UUID player, Component reason) {
        return createMute(player, reason, PunishmentDuration.permanent());
    }

    /**
     * This method queries all punishments with the given {@link UUID} of a player and returns them in a list.
     *
     * @param player the player whose punishments should be queried
     * @param <T>    the type of punishment(s), matching them in <code>type</code>
     * @return the list of punishments which are stored at the moment. This list may contains punishments which are over.
     */
    <T extends Punishment> CompletableFuture<List<T>> getPunishments(UUID player, Executor service, PunishmentType... type);

    /**
     * Queries the punishment stored with the given {@code punishmentId}
     *
     * @param punishmentId the punishment id from the punishment that should be queried
     * @param service      an {@link Executor} which will be used to perform async operations
     * @param <T>          the type of punishment
     * @return an {@link Optional} containing the queried punishment or {@link Optional#empty()} if it was not found
     */
    <T extends Punishment> CompletableFuture<Optional<T>> getPunishment(UUID punishmentId, Executor service);

    /**
     * @return the underlying {@link ProxyServer} of this punishment manager.
     */
    ProxyServer getServer();

    CompletableFuture<Boolean> isBanned(UUID playerUuid, Executor service);
}
