package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.listener.ChatListener;
import de.jvstvshd.velocitypunishment.punishment.Mute;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.util.Util.INTERNAL_ERROR;
import static de.jvstvshd.velocitypunishment.util.Util.copyComponent;

public class MuteCommand implements SimpleCommand {

    private final VelocityPunishmentPlugin plugin;
    private final ChatListener chatListener;

    public MuteCommand(VelocityPunishmentPlugin plugin, ChatListener chatListener) {
        this.plugin = plugin;
        this.chatListener = chatListener;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (invocation.arguments().length < 1) {
            source.sendMessage(Component.text("Please user /mute <player> [reason]").color(NamedTextColor.DARK_RED));
            return;
        }
        var playerResolver = plugin.getPlayerResolver();
        var punishmentManager = plugin.getPunishmentManager();
        PunishmentHelper parser = new PunishmentHelper();
        playerResolver.getOrQueryPlayerUuid(invocation.arguments()[0], plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
            if (throwable != null) {
                source.sendMessage(INTERNAL_ERROR);
                throwable.printStackTrace();
                return;
            }
            if (uuid == null) {
                source.sendMessage(Component.text(invocation.arguments()[0] + " could not be found."));
                return;
            }
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
            Component component = Component.text().append(
                    Component.text("You have muted the player ").color(NamedTextColor.RED),
                    copyComponent(invocation.arguments()[0]).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                    Component.text("/").color(NamedTextColor.RED),
                    copyComponent(uuidString).color(NamedTextColor.RED).decorate(TextDecoration.BOLD).color(NamedTextColor.YELLOW),
                    Component.text(" for ").color(NamedTextColor.RED),
                    reason,
                    Component.text(".").color(NamedTextColor.RED)
            ).build();
            source.sendMessage(component);
            source.sendMessage(Component.text("Punishment id: " + mute.getPunishmentUuid().toString().toLowerCase()).color(NamedTextColor.YELLOW));
            if (plugin.getServer().getPlayer(uuid).isPresent()) {
                chatListener.getMutes().put(uuid, new ChatListener.MuteContainer().setMute(mute));
            }
        }, plugin.getService());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        var server = plugin.getServer();
        if (args.length == 0) {
            return Util.getPlayerNames(server.getAllPlayers());
        }
        if (args.length == 1) {
            return Util.getPlayerNames(server.getAllPlayers())
                    .stream().filter(s -> s.toLowerCase().startsWith(args[0])).collect(Collectors.toList());
        }
        return ImmutableList.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.mute");
    }
}
