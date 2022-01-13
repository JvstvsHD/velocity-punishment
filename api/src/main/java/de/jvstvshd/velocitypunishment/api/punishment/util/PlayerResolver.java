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

package de.jvstvshd.velocitypunishment.api.punishment.util;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An interface used to retrieve player names und uuids used for storing them in a database.
 * To retrieve a player's uuid, for example, there are three different ways:<br>
 * 1.: {@link #getPlayerUuid(String)}: returns the uuid of the player via {@link Optional} or {@link Optional#empty()} if this player is not online/cached/etc.<br>
 * 2.: {@link #queryPlayerUuid(String, Executor)} queries the player uuid via the mojang api or some internal methods.<br>
 * 3.: {@link #getOrQueryPlayerName(UUID, Executor)}: a combination of the first two possibilities. If the Optional returned by {@link #getPlayerUuid(String)} was empty,
 * {@link #getOrQueryPlayerUuid(String, Executor)} is called. The result of this operation will be returned.
 */
public interface PlayerResolver {

    /**
     * Retrieves the player's name via {@link com.velocitypowered.api.proxy.ProxyServer#getPlayer(UUID)}, a caching mechanism or related things.
     *
     * @param uuid the uuid of the player
     * @return the name, or {@link Optional#empty()}
     */
    Optional<String> getPlayerName(@NotNull UUID uuid);

    /**
     * Queries the player's name, for example through the Mojang API
     *
     * @param uuid     the uuid of the player
     * @param executor an executor to compute async operations
     * @return a {@link CompletableFuture} being completed with the player's name or null, if not found.
     */
    CompletableFuture<String> queryPlayerName(@NotNull UUID uuid, @NotNull Executor executor);

    /**
     * At first, {@link #getPlayerName(UUID)} is invoked. If the result is empty, the result of {@link #queryPlayerName(UUID, Executor)} will be returned.
     *
     * @param uuid     the uuid of the player
     * @param executor an executor to compute async operations
     * @return a {@link CompletableFuture} being completed with the player's name or null, if not found.
     */
    CompletableFuture<String> getOrQueryPlayerName(@NotNull UUID uuid, @NotNull Executor executor);

    /**
     * Retrieves the player's uuid via {@link com.velocitypowered.api.proxy.ProxyServer#getPlayer(String)}, a caching mechanism or related things.
     *
     * @param name the name of the player
     * @return the name, or {@link Optional#empty()}
     */
    Optional<UUID> getPlayerUuid(@NotNull String name);

    /**
     * Queries the player's uuid, for example through the Mojang API
     *
     * @param name     the name of the player
     * @param executor an executor to compute async operations
     * @return a {@link CompletableFuture} being completed with the player's uuid or null, if not found.
     */
    CompletableFuture<UUID> queryPlayerUuid(@NotNull String name, @NotNull Executor executor);

    /**
     * At first, {@link #getPlayerUuid(String)} is invoked. If the result is empty, the result of {@link #queryPlayerUuid(String, Executor)} will be returned.
     *
     * @param name     the uuid of the player
     * @param executor an executor to compute async operations
     * @return a {@link CompletableFuture} being completed with the player's name or null, if not found.
     */
    CompletableFuture<UUID> getOrQueryPlayerUuid(@NotNull String name, @NotNull Executor executor);
}
