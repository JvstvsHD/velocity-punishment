package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.StandardPunishmentType;
import de.jvstvshd.velocitypunishment.internal.PunishmentHelper;
import de.jvstvshd.velocitypunishment.internal.Util;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class UnbanCommand implements SimpleCommand {

    private final ExecutorService service;
    private final DataSource dataSource;
    private final VelocityPunishmentPlugin plugin;

    public UnbanCommand(VelocityPunishmentPlugin plugin) {
        this.dataSource = plugin.getDataSource();
        this.service = plugin.getService();
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length < 1) {
            invocation.source().sendMessage(plugin.getMessageProvider().provide("command.unban.usage").color(NamedTextColor.RED));
            return;
        }
        var source = invocation.source();
        PunishmentHelper helper = new PunishmentHelper();
        helper.getPlayerUuid(0, service, plugin.getPlayerResolver(), invocation).whenCompleteAsync((uuid, throwable) -> {
            if (throwable != null) {
                source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                throwable.printStackTrace();
                return;
            }
            if (uuid == null) {
                source.sendMessage(plugin.getMessageProvider().provide("commands.general.not-found", source, true,
                        Component.text(invocation.arguments()[0]).color(NamedTextColor.YELLOW)).color(NamedTextColor.RED));
                return;
            }
            plugin.getPunishmentManager().getPunishments(uuid, service, StandardPunishmentType.BAN, StandardPunishmentType.PERMANENT_BAN).whenComplete((punishments, t) -> {
                if (t != null) {
                    t.printStackTrace();
                    invocation.source().sendMessage(plugin.getMessageProvider().internalError(source, true));
                    return;
                }
                if (punishments.isEmpty()) {
                    invocation.source().sendMessage(plugin.getMessageProvider().provide("command.punishment.not-banned", source, true).color(NamedTextColor.RED));
                    return;
                }
                if (punishments.size() > 1) {
                    invocation.source().sendMessage(plugin.getMessageProvider().provide("command.unban.multiple-bans", source, true).color(NamedTextColor.YELLOW));
                    for (Punishment punishment : punishments) {
                        invocation.source().sendMessage(buildComponent(helper.buildPunishmentData(punishment, plugin.getMessageProvider(), source), punishment));
                    }
                } else {
                    Punishment punishment = punishments.get(0);
                    punishment.cancel().whenCompleteAsync((unused, th) -> {
                        if (th != null) {
                            th.printStackTrace();
                            invocation.source().sendMessage(plugin.getMessageProvider().internalError(source, true));
                            return;
                        }
                        invocation.source().sendMessage(plugin.getMessageProvider().provide("command.unban.success").color(NamedTextColor.GREEN));
                    }, service);
                }
            });
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
        return invocation.source().hasPermission("punishment.command.unban");
    }

    private Component buildComponent(Component dataComponent, Punishment punishment) {
        return dataComponent.clickEvent(ClickEvent.runCommand("/punishment " + punishment.getPunishmentUuid()
                        .toString().toLowerCase(Locale.ROOT) + " remove"))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(Component
                        .text("Click to remove punishment").color(NamedTextColor.GREEN)));
    }
}
