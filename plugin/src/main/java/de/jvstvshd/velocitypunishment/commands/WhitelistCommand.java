package de.jvstvshd.velocitypunishment.commands;

import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.SQLException;

public class WhitelistCommand implements SimpleCommand {

    private final VelocityPunishmentPlugin plugin;

    public WhitelistCommand(VelocityPunishmentPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        if (invocation.arguments().length < 1) {
            source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.usage", source, true).color(NamedTextColor.RED));
            return;
        }
        if (invocation.arguments().length == 1) {
            plugin.getPlayerResolver().getOrQueryPlayerUuid(invocation.arguments()[0], plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
                if (Util.sendErrorMessageIfErrorOccurred(invocation, source, uuid, throwable, plugin)) return;
                try (var connection = plugin.getDataSource().getConnection();
                     var statement = connection.prepareStatement("SELECT * FROM velocity_punishment_whitelist WHERE uuid = ?;")) {
                    statement.setString(1, Util.trimUuid(uuid));
                    var rs = statement.executeQuery();
                    var whitelisted = rs.next() ? plugin.getMessageProvider().provide("whitelist.status.whitelisted", source) :
                            plugin.getMessageProvider().provide("whitelist.status.disallowed", source);
                    source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.status", source, true, Component.text(invocation.arguments()[0]).color(NamedTextColor.YELLOW), whitelisted.color(NamedTextColor.YELLOW)));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, plugin.getService());
            return;
        }
        var option = invocation.arguments()[1].toLowerCase();
        switch (option) {
            case "add", "remove" -> {
                plugin.getPlayerResolver().getOrQueryPlayerUuid(invocation.arguments()[0], plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
                    if (Util.sendErrorMessageIfErrorOccurred(invocation, source, uuid, throwable, plugin)) return;
                    try (var connection = plugin.getDataSource().getConnection();
                         var statement = connection.prepareStatement(option.equals("add") ? "INSERT INTO velocity_punishment_whitelist (uuid) VALUES (?);" :
                                 "DELETE FROM velocity_punishment_whitelist WHERE uuid = ?;")) {
                        statement.setString(1, Util.trimUuid(uuid));
                        source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.success", source, true));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                    }
                }, plugin.getService());
            }
            default -> source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.usage", source, true));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.whitelist");
    }
}
