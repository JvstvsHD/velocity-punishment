package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.util.PlayerResolver;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.util.Util.copyComponent;

public class BanCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final PunishmentManager punishmentManager;
    private final PlayerResolver playerResolver;

    public BanCommand(ProxyServer proxyServer, PunishmentManager punishmentManager, PlayerResolver playerResolver) {
        this.proxyServer = proxyServer;
        this.punishmentManager = punishmentManager;
        this.playerResolver = playerResolver;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (invocation.arguments().length < 1) {
            source.sendMessage(Component.text("Please use /ban <Player> [reason]").color(NamedTextColor.DARK_RED));
            return;
        }

        PunishmentHelper parser = new PunishmentHelper();
        Optional<UUID> optionalUUID = parser.parseUuid(playerResolver, invocation);
        UUID uuid;
        if (optionalUUID.isEmpty()) {
            source.sendMessage(Component.text(invocation.arguments()[0] + " could not be found."));
            return;
        }
        uuid = optionalUUID.get();
        TextComponent component = parser.parseComponent(1, invocation);
        punishmentManager.createPermanentBan(uuid, component).punish().whenCompleteAsync((unused, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                source.sendMessage(Util.INTERNAL_ERROR);
            } else {
                String uuidString = uuid.toString().toLowerCase();
                source.sendMessage(Component.text("You have banned the player ").color(NamedTextColor.RED)
                        .append(Component.text().append(copyComponent(invocation.arguments()[0]).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                                Component.text("/").color(NamedTextColor.WHITE),
                                copyComponent(uuidString).color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                                Component.text(" for ").color(NamedTextColor.RED),
                                component,
                                Component.text(".").color(NamedTextColor.RED))));
            }
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            return Util.getPlayerNames(proxyServer.getAllPlayers());
        }
        if (args.length == 1) {
            return Util.getPlayerNames(proxyServer.getAllPlayers())
                    .stream().filter(s -> s.toLowerCase().startsWith(args[0])).collect(Collectors.toList());
        }
        return ImmutableList.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.ban");
    }
}
