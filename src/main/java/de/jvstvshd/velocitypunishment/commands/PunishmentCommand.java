package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
import de.jvstvshd.velocitypunishment.listener.ChatListener;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

public class PunishmentCommand implements SimpleCommand {

    private final ExecutorService service;
    private final DataSource dataSource;
    private final ProxyServer server;
    private final ChatListener chatListener;
    private final VelocityPunishmentPlugin plugin;

    public PunishmentCommand(VelocityPunishmentPlugin plugin, ChatListener chatListener) {
        this.service = plugin.getService();
        this.dataSource = plugin.getDataSource();
        this.server = plugin.getServer();
        this.chatListener = chatListener;
        this.plugin = plugin;
    }

    private final static List<String> PUNISHMENT_OPTIONS = ImmutableList.of("cancel", "remove", "info", "change");
    private final static List<String> ALL_OPTIONS;

    static {
        var full = new ArrayList<>(PUNISHMENT_OPTIONS);
        full.add("playerinfo");
        ALL_OPTIONS = ImmutableList.copyOf(full);
    }

    @Override
    public void execute(Invocation invocation) {
        String[] arguments = invocation.arguments();
        CommandSource source = invocation.source();
        if (arguments.length < 2) {
            source.sendMessage(plugin.getMessageProvider().provide("command.punishment.usage", source, true).color(NamedTextColor.RED));
            return;
        }
        var punishmentManager = plugin.getPunishmentManager();
        if (arguments[0].equalsIgnoreCase("playerinfo")) {
            var playerResolver = plugin.getPlayerResolver();
            PunishmentHelper helper = new PunishmentHelper();
            helper.getPlayerUuid(1, service, playerResolver, invocation).whenCompleteAsync((uuid, throwable) -> {
                if (throwable != null) {
                    source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                    throwable.printStackTrace();
                    return;
                }
                if (uuid == null) {
                    source.sendMessage(plugin.getMessageProvider().provide("command.punishment.not-banned", source, true).color(NamedTextColor.RED));
                    return;
                }
                punishmentManager.getPunishments(uuid, service).whenComplete((punishments, t) -> {
                    if (t != null) {
                        source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                        t.printStackTrace();
                        return;
                    }
                    source.sendMessage(plugin.getMessageProvider().provide("command.punishment.punishments", source, true, Component.text(punishments.size())).color(NamedTextColor.AQUA));
                    for (Punishment punishment : punishments) {
                        Component component = helper.buildPunishmentData(punishment, plugin.getMessageProvider(), source)
                                .clickEvent(ClickEvent.suggestCommand(punishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT)))
                                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(plugin.getMessageProvider().provide("commands.general.copy")
                                        .color(NamedTextColor.GREEN)));
                        source.sendMessage(component);
                    }
                });
            }, service);
            return;
        }
        UUID uuid;
        try {
            uuid = Util.parseUuid(arguments[1]);
        } catch (IllegalArgumentException e) {
            source.sendMessage(plugin.getMessageProvider().provide("command.punishment.uuid-parse-error", source, true, Component.text(arguments[0]).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
            return;
        }
        String option = arguments[0].toLowerCase(Locale.ROOT);
        if (!PUNISHMENT_OPTIONS.contains(option)) {
            source.sendMessage(plugin.getMessageProvider().provide("command.punishment.unknown-option", source, true, Component.text(option).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
            return;
        }
        punishmentManager.getPunishment(uuid, service).whenCompleteAsync((optional, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                return;
            }
            Punishment punishment;
            if (optional.isEmpty()) {
                source.sendMessage(plugin.getMessageProvider().provide("command.punishment.unknown-punishment-id", source, true,
                        Component.text(uuid.toString().toLowerCase(Locale.ROOT)).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
                return;
            }
            punishment = optional.get();
            switch (option) {
                case "cancel", "remove" -> punishment.cancel().whenCompleteAsync((unused, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                        source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                        return;
                    }
                    source.sendMessage(plugin.getMessageProvider().provide("punishment.remove", source, true).color(NamedTextColor.GREEN));
                    try {
                        chatListener.update(uuid);
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                        e.printStackTrace();
                    }
                });
                case "info" -> source.sendMessage(new PunishmentHelper().buildPunishmentData(punishment, plugin.getMessageProvider(), source));
                case "change" -> source.sendMessage(Component.text("Soon™️"));
            }
        });

    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        if (invocation.arguments().length == 2 && invocation.arguments()[0].equalsIgnoreCase("playerinfo")) {
            return Util.executeAsync(() -> {
                Set<String> list = new HashSet<>();
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("SELECT name FROM velocity_punishment WHERE name LIKE ?")) {
                    String suggestion = invocation.arguments().length == 1 ? "" : invocation.arguments()[1].toLowerCase();
                    statement.setString(1, suggestion + "%");
                    ResultSet rs = statement.executeQuery();
                    while (rs.next()) {
                        list.add(rs.getString(1));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                list.addAll(Util.getPlayerNames(server.getAllPlayers()).stream().map(String::toLowerCase).toList());
                return ImmutableList.copyOf(list);
            }, service);
        }
        return CompletableFuture.completedFuture(suggest(invocation));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] arguments = invocation.arguments();
        if (arguments.length == 0) {
            return ALL_OPTIONS;
        } else if (arguments.length == 1 && arguments[0].toLowerCase(Locale.ROOT).startsWith("p")) {
            return ALL_OPTIONS.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(arguments[0].toLowerCase(Locale.ROOT))).toList();
        }
        return new ArrayList<>();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.punishment");
    }
}
