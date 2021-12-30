package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.punishment.PunishmentDuration;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.util.Util.copyComponent;

public class TempmuteCommand implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final ExecutorService service;
    private final VelocityPunishmentPlugin plugin;

    public TempmuteCommand(VelocityPunishmentPlugin plugin) {
        this.proxyServer = plugin.getServer();
        this.service = plugin.getService();
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (invocation.arguments().length < 2) {
            source.sendMessage(plugin.getMessageProvider().provide("command.tempmute.usage", source, true).color(NamedTextColor.RED));
            return;
        }
        PunishmentHelper parser = new PunishmentHelper();
        plugin.getPlayerResolver().getOrQueryPlayerUuid(invocation.arguments()[0], service).whenCompleteAsync((uuid, throwable) -> {
            if (throwable != null) {
                source.sendMessage(plugin.getMessageProvider().internalError());
                throwable.printStackTrace();
                return;
            }
            if (uuid == null) {
                source.sendMessage(plugin.getMessageProvider().provide("commands.general.not-found", source, true, Component.text(invocation.arguments()[0]).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
                return;
            }
            Optional<PunishmentDuration> optDuration = parser.parseDuration(1, invocation);
            if (optDuration.isEmpty()) {
                return;
            }
            PunishmentDuration duration = optDuration.get();
            TextComponent component = parser.parseComponent(2, invocation, Component.text("mute").color(NamedTextColor.DARK_RED));
            plugin.getPunishmentManager().createMute(uuid, component, duration).punish().whenComplete((mute, t) -> {
                if (t != null) {
                    invocation.source().sendMessage(plugin.getMessageProvider().internalError());
                    t.printStackTrace();
                    return;
                }
                String until = duration.expiration().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                String uuidString = uuid.toString().toLowerCase();
                source.sendMessage(plugin.getMessageProvider().provide("command.tempmute.success", source, true,
                        copyComponent(invocation.arguments()[0]).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                        copyComponent(uuidString).color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.text(until).color(NamedTextColor.GREEN)));
                source.sendMessage(plugin.getMessageProvider().provide("commands.general.punishment.id", source, true, Component.text(mute.getPunishmentUuid().toString().toLowerCase()).color(NamedTextColor.YELLOW)));
            });
        }, service);
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
