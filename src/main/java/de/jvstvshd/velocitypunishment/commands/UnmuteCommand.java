package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.SimpleCommand;
import com.zaxxer.hikari.HikariDataSource;
import de.jvstvshd.velocitypunishment.listener.ChatListener;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.punishment.StandardPunishmentType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.*;

public class UnmuteCommand implements SimpleCommand {

    private final ExecutorService service;
    private final PunishmentManager manager;
    private final DataSource dataSource;
    private final ChatListener chatListener;

    public UnmuteCommand(PunishmentManager punishmentManager, HikariDataSource dataSource, ExecutorService service, ChatListener chatListener) {
        this.manager = punishmentManager;
        this.dataSource = dataSource;
        this.service = service;
        this.chatListener = chatListener;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length < 1) {
            invocation.source().sendMessage(Component.text("Invalid usage!").color(NamedTextColor.DARK_RED));
            return;
        }
        PunishmentHelper helper = new PunishmentHelper();
        UUID playerUuid = helper.getPlayerUuid(0, service, manager, invocation);
        if (playerUuid == null) {
            invocation.source().sendMessage(Component.text("This player is not muted at the moment.").color(NamedTextColor.RED));
            return;
        }
        List<Punishment> punishments;
        try {
            punishments = manager.getPunishments(playerUuid, service, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            invocation.source().sendMessage(Util.INTERNAL_ERROR);
            return;
        }
        if (punishments.isEmpty()) {
            invocation.source().sendMessage(Component.text("This player is not muted at the moment.").color(NamedTextColor.RED));
            return;
        }
        if (punishments.size() > 1) {
            invocation.source().sendMessage(Component.text("This player has multiple punishments with type (permanent) mute.").color(NamedTextColor.YELLOW));
            for (Punishment punishment : punishments) {
                invocation.source().sendMessage(buildComponent(helper.buildPunishmentData(punishment), punishment));
            }
        } else {
            Punishment punishment = punishments.get(0);
            if (helper.annul(invocation, punishment)) {
                invocation.source().sendMessage(Component.text("The mute was annulled.").color(NamedTextColor.GREEN));
                chatListener.update(playerUuid);
            } else {
                invocation.source().sendMessage(Component.text("The mute could not be annulled.").color(NamedTextColor.RED));
            }
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            if (invocation.arguments()[0].length() > 1) {
                return Util.executeAsync(() -> {
                    List<String> list = new ArrayList<>();
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement("SELECT name FROM velocity_punishment WHERE name LIKE ?")) {
                        statement.setString(1, invocation.arguments()[0].toLowerCase() + "%");
                        ResultSet rs = statement.executeQuery();
                        while (rs.next()) {
                            list.add(rs.getString(1));
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return ImmutableList.copyOf(list);
                }, service);

            }
        }
        return SimpleCommand.super.suggestAsync(invocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.unmute");
    }

    private Component buildComponent(Component dataComponent, Punishment punishment) {
        return dataComponent.clickEvent(ClickEvent.runCommand("/punishment " + punishment.getPunishmentUuid()
                        .toString().toLowerCase(Locale.ROOT) + " remove"))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(Component
                        .text("Click to remove punishment").color(NamedTextColor.GREEN)));
    }
}
