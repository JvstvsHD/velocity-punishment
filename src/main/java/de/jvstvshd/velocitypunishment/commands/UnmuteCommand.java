package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.listener.ChatListener;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
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
import java.util.concurrent.*;

import static de.jvstvshd.velocitypunishment.util.Util.INTERNAL_ERROR;

public class UnmuteCommand implements SimpleCommand {

    private final ExecutorService service;
    private final DataSource dataSource;
    private final VelocityPunishmentPlugin plugin;
    private final ChatListener chatListener;

    public UnmuteCommand(VelocityPunishmentPlugin plugin, ChatListener chatListener) {
        this.dataSource = plugin.getDataSource();
        this.service = plugin.getService();
        this.plugin = plugin;
        this.chatListener = chatListener;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length < 1) {
            invocation.source().sendMessage(Component.text("Please use /unmute <player>").color(NamedTextColor.DARK_RED));
            return;
        }
        var source = invocation.source();
        PunishmentHelper helper = new PunishmentHelper();
        helper.getPlayerUuid(0, service, plugin.getPlayerResolver(), invocation).whenCompleteAsync((uuid, throwable) -> {
            if (throwable != null) {
                source.sendMessage(INTERNAL_ERROR);
                throwable.printStackTrace();
                return;
            }
            if (uuid == null) {
                source.sendMessage(Component.text(invocation.arguments()[0] + " could not be found."));
                return;
            }
            List<Punishment> punishments;
            try {
                punishments = plugin.getPunishmentManager().getPunishments(uuid, service, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).get(5, TimeUnit.SECONDS);
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
                punishment.cancel().whenCompleteAsync((unused, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                        invocation.source().sendMessage(Util.INTERNAL_ERROR);
                        return;
                    }
                    invocation.source().sendMessage(Component.text("The ban was annulled.").color(NamedTextColor.GREEN));
                    try {
                        chatListener.update(uuid);
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        source.sendMessage(INTERNAL_ERROR);
                        e.printStackTrace();
                    }
                });
            }
        }, service);
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
