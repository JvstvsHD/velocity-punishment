package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.util.Util.INTERNAL_ERROR;
import static de.jvstvshd.velocitypunishment.util.Util.copyComponent;

public class BanCommand implements SimpleCommand {

    private final VelocityPunishmentPlugin plugin;

    public BanCommand(VelocityPunishmentPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (invocation.arguments().length < 1) {
            //source.sendMessage(Component.text().append(plugin.getMessageProvider().prefix(), plugin.getMessageProvider().provide("command.ban.usage").color(NamedTextColor.RED)));
            source.sendMessage(Component.text("Please use /ban <player> [reason]").color(NamedTextColor.DARK_RED));
            return;
        }
        var playerResolver = plugin.getPlayerResolver();
        var punishmentManager = plugin.getPunishmentManager();
        var parser = new PunishmentHelper();
        playerResolver.getOrQueryPlayerUuid(invocation.arguments()[0], plugin.getService()).whenComplete((uuid, throwable) -> {
            if (throwable != null) {
                source.sendMessage(INTERNAL_ERROR);
                throwable.printStackTrace();
                return;
            }
            if (uuid == null) {
                source.sendMessage(Component.translatable().args(Component.text(invocation.arguments()[0])).key(invocation.arguments()[0] + " could not be found.").build());
                return;
            }
            TextComponent component = parser.parseComponent(1, invocation);
            punishmentManager.createPermanentBan(uuid, component).punish().whenCompleteAsync((unused, t) -> {
                if (t != null) {
                    t.printStackTrace();
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
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        var proxyServer = plugin.getServer();
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
