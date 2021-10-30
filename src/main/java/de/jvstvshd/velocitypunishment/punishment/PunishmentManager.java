package de.jvstvshd.velocitypunishment.punishment;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.punishment.impl.StandardPunishmentManager;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public interface PunishmentManager {

    /**
     * Prepares a ban for a player with custom reason and duration.
     *
     * @param player   the uuid of the player which should be banned (by either {@link Player#getUsername()} or {@link #getPlayerNameMojangApi(UUID, ExecutorService)}.
     * @param reason   the reason given as a {@link Component}. This component will be serialized as Json with a {@link net.kyori.adventure.text.serializer.gson.GsonComponentSerializer}
     *                 for storing it in the database.
     * @param duration the duration, which can be created via {@link PunishmentDuration#parse(String)} when it's source is from minecraft commands.
     * @return the prepared ban with the given reason and duration. This duration remains the same at any duration since it is only added when the player is banned.
     * Only {@link Ban#punish()} ()} is needed to execute the punishment.
     */
    Ban createBan(UUID player, Component reason, PunishmentDuration duration);

    /**
     * Prepares a ban for a player with custom reason and duration. Its only difference to {@link #createBan(UUID, Component, PunishmentDuration)} is, that the {@link PunishmentDuration}
     * is {@link PunishmentDuration#permanent()}.
     *
     * @param player the uuid of the player which should be banned (by either {@link Player#getUsername()} or {@link #getPlayerNameMojangApi(UUID, ExecutorService)}.
     * @param reason the reason given as a {@link Component}. This component will be serialized as Json with a {@link net.kyori.adventure.text.serializer.gson.GsonComponentSerializer}
     *               for storing it in the database.
     * @return the prepared ban with the given reason and duration. The duration is permanent, equals {@link java.time.LocalDateTime#MAX}
     * Only {@link Ban#punish()} is needed to execute the punishment.
     */
    default Ban createPermanentBan(UUID player, Component reason) {
        return createBan(player, reason, PunishmentDuration.permanent());
    }

    /**
     * Prepares a mute for a player with custom reason and duration.
     *
     * @param player   the uuid of the player which should be banned (by either {@link Player#getUsername()} or {@link #getPlayerNameMojangApi(UUID, ExecutorService)}.
     * @param reason   the reason given as a {@link Component}. This component will be serialized as Json with a {@link net.kyori.adventure.text.serializer.gson.GsonComponentSerializer}
     *                 for storing it in the database.
     * @param duration the duration, which can be created via {@link PunishmentDuration#parse(String)} when it's source is from minecraft commands.
     * @return the prepared ban with the given reason and duration. This duration remains the same at any duration since it is only added when the player is banned.
     * Only {@link Ban#punish()} is needed to execute the punishment.
     */
    Mute createMute(UUID player, Component reason, PunishmentDuration duration);

    /**
     * Prepares a ban for a player with custom reason and duration. Its only difference to {@link #createMute(UUID, Component, PunishmentDuration)} is, that the {@link PunishmentDuration}
     * is {@link PunishmentDuration#permanent()}.
     *
     * @param player the uuid of the player which should be banned (by either {@link Player#getUsername()} or {@link #getPlayerNameMojangApi(UUID, ExecutorService)}.
     * @param reason the reason given as a {@link Component}. This component will be serialized as Json with a {@link net.kyori.adventure.text.serializer.gson.GsonComponentSerializer}
     *               for storing it in the database.
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
     * @return the list of punishments which are running at the moment.
     */
    <T extends Punishment> CompletableFuture<List<T>>  getPunishments(UUID player, ExecutorService service, PunishmentType... type);

    <T extends Punishment> CompletableFuture<Optional<T>> getPunishment(UUID punishmentId, ExecutorService service);

    /**
     * @return the underlying {@link ProxyServer} of this punishment manager.
     */
    ProxyServer getServer();

    /**
     * Gets the name of the minecraft user with the {@link UUID} given in <code>uuid</code>. If the player is not online, his name will be queried in the mojang api (at least in
     * {@link StandardPunishmentManager#getPlayerName(UUID, ExecutorService)}.
     *
     * @param uuid the {@link UUID} of the player
     * @param service an {@link ExecutorService} for executing the request asynchronous
     * @return a {@link CompletableFuture} in which the name should be completed.
     * @see #getPlayerNameMojangApi(UUID, ExecutorService)
     */
    CompletableFuture<String> getPlayerName(UUID uuid, ExecutorService service);

    /**
     * Queries the name by using the official <a href="https://sessionserver.mojang.com/session/minecraft/profile/">mojang api with the user's account UUID</a>.
     * @param uuid the uuid of the player
     * @param service an {@link ExecutorService} for executing the request asynchronous
     * @return a {@link CompletableFuture} in which the name should be completed.
     */
    CompletableFuture<String> getPlayerNameMojangApi(UUID uuid, ExecutorService service);

    CompletableFuture<UUID> getPlayerUuid(String name, ExecutorService service);

    CompletableFuture<UUID> getPlayerUuidMojangApi(String name, ExecutorService service);

    CompletableFuture<Boolean> isBanned(UUID playerUuid, ExecutorService service);

}
