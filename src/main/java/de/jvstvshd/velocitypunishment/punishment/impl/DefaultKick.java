package de.jvstvshd.velocitypunishment.punishment.impl;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.message.MessageProvider;
import de.jvstvshd.velocitypunishment.punishment.*;
import de.jvstvshd.velocitypunishment.punishment.util.PlayerResolver;
import net.kyori.adventure.text.Component;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DefaultKick extends AbstractPunishment implements Kick {

    public DefaultKick(UUID playerUuid, Component reason, DataSource dataSource, PlayerResolver playerResolver, PunishmentManager punishmentManager, ExecutorService service, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, playerResolver, punishmentManager, service, messageProvider);
    }

    public DefaultKick(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, service, punishmentManager, punishmentUuid, playerResolver, messageProvider);
    }

    @Override
    public CompletableFuture<Punishment> punish() {
        Optional<Player> optPlayer = getPunishmentManager().getServer().getPlayer(getPlayerUuid());
        if (optPlayer.isEmpty())
            throw new IllegalArgumentException("Invalid player was not found. This could be because " +
                    "a) this player does not exist (wrong uuid)\n" +
                    "b) the player is not online and so can't be kicked.");
        optPlayer.get().disconnect(getReason());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public PunishmentType getType() {
        return StandardPunishmentType.KICK;
    }

    @Override
    public Component createFullReason(CommandSource source) {
        return getReason();
    }
}
