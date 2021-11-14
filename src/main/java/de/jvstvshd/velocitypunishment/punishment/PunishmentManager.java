package de.jvstvshd.velocitypunishment.punishment;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
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
     * @param player   the uuid of the player which should be banned (by either {@link Player#getUniqueId()} ()} or {@link de.jvstvshd.velocitypunishment.util.PlayerResolver#getPlayerUuid(String)}.
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
     * @param player the uuid of the player which should be banned (by either {@link Player#getUniqueId()} ()} or {@link de.jvstvshd.velocitypunishment.util.PlayerResolver#getPlayerUuid(String)}.
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
     * @param player   the uuid of the player which should be banned (by either {@link Player#getUniqueId()} ()} or {@link de.jvstvshd.velocitypunishment.util.PlayerResolver#getPlayerUuid(String)}.
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
     * @param player the uuid of the player which should be banned (by either {@link Player#getUniqueId()} ()} or {@link de.jvstvshd.velocitypunishment.util.PlayerResolver#getPlayerUuid(String)}
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
    <T extends Punishment> CompletableFuture<List<T>> getPunishments(UUID player, ExecutorService service, PunishmentType... type);

    <T extends Punishment> CompletableFuture<Optional<T>> getPunishment(UUID punishmentId, ExecutorService service);

    /**
     * @return the underlying {@link ProxyServer} of this punishment manager.
     */
    ProxyServer getServer();

    CompletableFuture<Boolean> isBanned(UUID playerUuid, ExecutorService service);

}
