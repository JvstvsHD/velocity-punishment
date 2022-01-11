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

package de.jvstvshd.velocitypunishment.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@SuppressWarnings("ClassCanBeRecord")
public class DefaultPlayerResolver implements PlayerResolver {

    private final ProxyServer proxyServer;

    public DefaultPlayerResolver(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public Optional<String> getPlayerName(@NotNull UUID uuid) {
        Optional<Player> optional = proxyServer.getPlayer(uuid);
        if (optional.isEmpty())
            return Optional.empty();
        return Optional.ofNullable(optional.get().getUsername());
    }

    @Override
    public CompletableFuture<String> queryPlayerName(@NotNull UUID uuid, @NotNull Executor executor) {
        CompletableFuture<String> cf = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonElement jsonElement = JsonParser.parseString(response.body());
                cf.complete(jsonElement.getAsJsonObject().get("name").getAsString());
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    @Override
    public CompletableFuture<String> getOrQueryPlayerName(@NotNull UUID uuid, @NotNull Executor executor) {
        if (getPlayerName(uuid).isPresent()) {
            return CompletableFuture.completedFuture(getPlayerName(uuid).get());
        }
        return queryPlayerName(uuid, executor);
    }

    @Override
    public Optional<UUID> getPlayerUuid(@NotNull String name) {
        Optional<Player> optional = proxyServer.getPlayer(name);
        if (optional.isEmpty())
            return Optional.empty();
        return Optional.ofNullable(optional.get().getUniqueId());
    }

    @Override
    public CompletableFuture<UUID> queryPlayerUuid(@NotNull String name, @NotNull Executor executor) {
        CompletableFuture<UUID> cf = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name)).GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonElement jsonElement = JsonParser.parseString(response.body());
                if (jsonElement == null || jsonElement.isJsonNull()) {
                    cf.complete(null);
                    return;
                }
                var result = jsonElement.getAsJsonObject().get("id").getAsString();
                UUID uuid;
                try {
                    uuid = UUID.fromString(result);
                } catch (IllegalArgumentException e) {
                    uuid = UUID.fromString(result.replaceAll(
                            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                            "$1-$2-$3-$4-$5"));
                }
                cf.complete(uuid);
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    @Override
    public CompletableFuture<UUID> getOrQueryPlayerUuid(@NotNull String name, @NotNull Executor executor) {
        if (getPlayerUuid(name).isPresent()) {
            return CompletableFuture.completedFuture(getPlayerUuid(name).get());
        }
        return queryPlayerUuid(name, executor);
    }
}
