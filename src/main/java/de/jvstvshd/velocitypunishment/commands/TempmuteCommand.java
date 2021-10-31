package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.punishment.Mute;
import de.jvstvshd.velocitypunishment.punishment.PunishmentDuration;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.util.Util.copyComponent;

public class TempmuteCommand implements SimpleCommand {

    private final PunishmentManager punishmentManager;
    private final ProxyServer proxyServer;

    public TempmuteCommand(PunishmentManager punishmentManager, ProxyServer proxyServer) {
        this.punishmentManager = punishmentManager;
        this.proxyServer = proxyServer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!hasPermission(invocation)) {
            source.sendMessage(Component.text("no_permission").color(NamedTextColor.RED));
            return;
        }
        if (invocation.arguments().length < 2) {
            source.sendMessage(Component.text("invalid_usage").color(NamedTextColor.DARK_RED));
            return;
        }

        PunishmentHelper parser = new PunishmentHelper();
        Optional<UUID> optionalUUID = parser.parseUuid(punishmentManager, invocation);
        UUID uuid;
        if (optionalUUID.isEmpty()) {
            source.sendMessage(Component.text(invocation.arguments()[0] + " could not be found."));
            return;
        }
        uuid = optionalUUID.get();
        Optional<PunishmentDuration> optDuration = parser.parseDuration(1, invocation);
        if (optDuration.isEmpty()) {
            return;
        }
        PunishmentDuration duration = optDuration.get();
        TextComponent component = parser.parseComponent(2, invocation);
        Mute mute;
        try {
            mute = punishmentManager.createMute(uuid, component, duration);
            mute.punish().get();
        } catch (InterruptedException | ExecutionException e) {
            invocation.source().sendMessage(Util.INTERNAL_ERROR);
            e.printStackTrace();
            return;
        }
        String until = duration.expiration().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        String uuidString = uuid.toString().toLowerCase();//.replace('-', Character.MIN_VALUE);
        source.sendMessage(Component.text("You have muted the player ").color(NamedTextColor.RED)
                .append(Component.text().append(copyComponent(invocation.arguments()[0]).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                        Component.text("/").color(NamedTextColor.WHITE),
                        copyComponent(uuidString).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                        Component.text(" for ").color(NamedTextColor.RED),
                        component,
                        Component.text(" until ").color(NamedTextColor.RED),
                        Component.text(until).color(NamedTextColor.GREEN),
                        Component.text(".").color(NamedTextColor.RED))));
        source.sendMessage(Component.text("Punishment id: " + mute.getPunishmentUuid().toString().toLowerCase()).color(NamedTextColor.YELLOW));
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
        return invocation.source().hasPermission("punishment.command.tempmute");
    }
}
