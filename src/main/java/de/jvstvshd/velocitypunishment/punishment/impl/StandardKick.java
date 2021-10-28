package de.jvstvshd.velocitypunishment.punishment.impl;

import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.punishment.Kick;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.punishment.PunishmentType;
import de.jvstvshd.velocitypunishment.punishment.StandardPunishmentType;
import net.kyori.adventure.text.Component;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class StandardKick extends AbstractPunishment implements Kick {

    public StandardKick(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager) {
        super(playerUuid, reason, dataSource, service, punishmentManager);
    }

    public StandardKick(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager, UUID punishmentUuid) {
        super(playerUuid, reason, dataSource, service, punishmentManager, punishmentUuid);
    }

    @Override
    public CompletableFuture<Boolean> punish() {
        Optional<Player> optPlayer = getPunishmentManager().getServer().getPlayer(getPlayerUuid());
        if (optPlayer.isEmpty())
            throw new IllegalArgumentException("Invalid player was not found. This could be because " +
                    "a) this player does not exist (wrong uuid)\n" +
                    "b) the player is not online and so can't be kicked.");
        optPlayer.get().disconnect(getReason());
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public PunishmentType getType() {
        return StandardPunishmentType.KICK;
    }
}
