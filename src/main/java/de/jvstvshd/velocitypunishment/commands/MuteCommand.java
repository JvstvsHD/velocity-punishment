package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.listener.ChatListener;
import de.jvstvshd.velocitypunishment.punishment.Mute;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.util.Util.copyComponent;

public class MuteCommand implements SimpleCommand {

    private final PunishmentManager punishmentManager;
    private final ProxyServer server;
    private final ChatListener chatListener;

    public MuteCommand(PunishmentManager punishmentManager, ProxyServer server, ChatListener chatListener) {
        this.punishmentManager = punishmentManager;
        this.server = server;
        this.chatListener = chatListener;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (invocation.arguments().length < 1) {
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
        TextComponent reason = parser.parseComponent(1, invocation);

        Mute mute;
        try {
            mute = punishmentManager.createPermanentMute(uuid, reason);
            mute.punish().get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            invocation.source().sendMessage(Util.INTERNAL_ERROR);
            e.printStackTrace();
            return;
        }
        String uuidString = uuid.toString().toLowerCase();
        source.sendMessage(new PunishmentHelper().buildPunishmentData(mute));
        Component component = Component.text().append(
                Component.text("You have muted the player ").color(NamedTextColor.RED),
                copyComponent(invocation.arguments()[0]).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                copyComponent(uuidString).color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text(" for ").color(NamedTextColor.RED),
                reason,
                Component.text(".").color(NamedTextColor.RED)
        ).build();
        source.sendMessage(component);
        if (server.getPlayer(uuid).isPresent()) {
            chatListener.getMutes().put(uuid, mute);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0) {
            return Util.getPlayerNames(server.getAllPlayers());
        }
        if (args.length == 1) {
            return Util.getPlayerNames(server.getAllPlayers())
                    .stream().filter(s -> s.toLowerCase().startsWith(args[0])).collect(Collectors.toList());
        }
        return ImmutableList.of();
    }
}
