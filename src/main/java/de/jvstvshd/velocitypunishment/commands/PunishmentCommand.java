package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.listener.ChatListener;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
import de.jvstvshd.velocitypunishment.util.Util;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static de.jvstvshd.velocitypunishment.util.Util.INTERNAL_ERROR;

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

    private final static List<String> options = ImmutableList.of("annul", "remove", "info", "change");

    @Override
    public void execute(Invocation invocation) {
        String[] arguments = invocation.arguments();
        CommandSource source = invocation.source();
        if (arguments.length < 2) {
            source.sendMessage(Component.text("Please use /punishment <playerinfo> <player> or <punishment id> <annul|info|change|info|remove>").color(NamedTextColor.DARK_RED));
            return;
        }
        var punishmentManager = plugin.getPunishmentManager();
        if (arguments[0].equalsIgnoreCase("playerinfo")) {
            var playerResolver = plugin.getPlayerResolver();
            PunishmentHelper helper = new PunishmentHelper();
            helper.getPlayerUuid(1, service, playerResolver, invocation).whenCompleteAsync((uuid, throwable) -> {
                if (throwable != null) {
                    source.sendMessage(INTERNAL_ERROR);
                    throwable.printStackTrace();
                    return;
                }
                if (uuid == null) {
                    invocation.source().sendMessage(Component.text("This player is not banned at the moment.").color(NamedTextColor.RED));
                    return;
                }
                List<Punishment> punishments;
                try {
                    punishments = punishmentManager.getPunishments(uuid, service).get(10, TimeUnit.SECONDS);
                } catch (InterruptedException | TimeoutException | ExecutionException e) {
                    e.printStackTrace();
                    source.sendMessage(Util.INTERNAL_ERROR);
                    return;
                }
                source.sendMessage(Component.text("The player has " + punishments.size() + " punishments.").color(NamedTextColor.AQUA));
                for (Punishment punishment : punishments) {

                    Component component = helper.buildPunishmentData(punishment)
                            .clickEvent(ClickEvent.suggestCommand(punishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT)))
                            .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(Component.text("Click to copy punishment id")
                                    .color(NamedTextColor.GREEN)));
                    source.sendMessage(component);
                }
            }, service);
            return;
        }
        UUID uuid;
        try {
            uuid = Util.parseUuid(arguments[0]);
        } catch (IllegalArgumentException e) {
            source.sendMessage(Component.text().append(Component.text("Could not parse string '").color(NamedTextColor.RED),
                    Component.text(arguments[0]).color(NamedTextColor.YELLOW),
                    Component.text("' as uuid.").color(NamedTextColor.RED)));
            return;
        }
        String option = arguments[1].toLowerCase(Locale.ROOT);
        if (!options.contains(option)) {
            source.sendMessage(Component.text()
                    .append(Component.text("Unknown option: ").color(NamedTextColor.RED),
                            Component.text(option).color(NamedTextColor.YELLOW)));
            return;
        }
        punishmentManager.getPunishment(uuid, service).whenCompleteAsync((optional, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                source.sendMessage(Util.INTERNAL_ERROR);
                return;
            }
            Punishment punishment;
            if (optional.isEmpty()) {
                source.sendMessage(Component.text().append(Component.text("Could not find a punishment for id '").color(NamedTextColor.RED),
                        Component.text(uuid.toString().toLowerCase(Locale.ROOT)).color(NamedTextColor.YELLOW),
                        Component.text("'.").color(NamedTextColor.RED)));
                return;
            }
            punishment = optional.get();
            switch (option) {
                case "annul", "remove" -> punishment.cancel().whenCompleteAsync((unused, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                        source.sendMessage(Util.INTERNAL_ERROR);
                        return;
                    }
                    source.sendMessage(Component.text("The punishment was successfully annulled.").color(NamedTextColor.GREEN));
                    try {
                        chatListener.update(uuid);
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        source.sendMessage(INTERNAL_ERROR);
                        e.printStackTrace();
                    }
                });
                case "info" -> source.sendMessage(new PunishmentHelper().buildPunishmentData(punishment));
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
            return List.of("playerinfo");
        } else if (arguments.length == 1 && arguments[0].toLowerCase(Locale.ROOT).startsWith("p")) {
            return List.of("playerinfo");
        } else if (arguments.length == 2) {
            return options;
        } else if (arguments.length == 3) {
            return options.stream().filter(s -> s.toLowerCase().startsWith(arguments[2])).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.punishment");
    }
}
