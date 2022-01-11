package de.jvstvshd.velocitypunishment.api.punishment.util;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface PlayerResolver {

    Optional<String> getPlayerName(@NotNull UUID uuid);

    CompletableFuture<String> queryPlayerName(@NotNull UUID uuid, @NotNull Executor executor);

    CompletableFuture<String> getOrQueryPlayerName(@NotNull UUID uuid, @NotNull Executor executor);

    Optional<UUID> getPlayerUuid(@NotNull String name);

    CompletableFuture<UUID> queryPlayerUuid(@NotNull String name, @NotNull Executor executor);

    CompletableFuture<UUID> getOrQueryPlayerUuid(@NotNull String name, @NotNull Executor executor);
}
