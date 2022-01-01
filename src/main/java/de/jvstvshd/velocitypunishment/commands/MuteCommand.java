package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import de.jvstvshd.velocitypunishment.listener.ChatListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.internal.Util.copyComponent;

@SuppressWarnings("ClassCanBeRecord")
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
            source.sendMessage(plugin.getMessageProvider().provide("command.mute.usage", source, true).color(NamedTextColor.RED));
            return;
        }
        var playerResolver = plugin.getPlayerResolver();
        var punishmentManager = plugin.getPunishmentManager();
        PunishmentHelper parser = new PunishmentHelper();
        playerResolver.getOrQueryPlayerUuid(invocation.arguments()[0], plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
            if (throwable != null) {
                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                throwable.printStackTrace();
                return;
            }
            if (uuid == null) {
                source.sendMessage(Component.translatable().args(Component.text(invocation.arguments()[0]).color(NamedTextColor.YELLOW)).key("commands.general.not-found").color(NamedTextColor.RED));
                return;
            }
            TextComponent reason = parser.parseComponent(1, invocation, Component.text("mute").color(NamedTextColor.DARK_RED));
            punishmentManager.createPermanentMute(uuid, reason).punish().whenComplete((mute, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                } else {
                    String uuidString = uuid.toString().toLowerCase();
                    source.sendMessage(plugin.getMessageProvider().provide("command.mute.success", source, true, copyComponent(invocation.arguments()[0], plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                            copyComponent(uuidString, plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                            reason).color(NamedTextColor.GREEN));
                    source.sendMessage(plugin.getMessageProvider().provide("commands.general.punishment.id", source, true, copyComponent(mute.getPunishmentUuid().toString().toLowerCase(), plugin.getMessageProvider(), source).color(NamedTextColor.YELLOW)));
                }
                if (plugin.getServer().getPlayer(uuid).isPresent()) {
                    try {
                        chatListener.update(uuid);
                    } catch (ExecutionException | TimeoutException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
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
