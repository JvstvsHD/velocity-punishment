/*
 * This file is part of Velocity Punishment, which is licensed under the MIT license.
 *
 * Copyright (c) 2022 JvstvsHD
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
            case "add", "remove" ->
                    plugin.getPlayerResolver().getOrQueryPlayerUuid(invocation.arguments()[0], plugin.getService()).whenCompleteAsync((uuid, throwable) -> {
                        if (Util.sendErrorMessageIfErrorOccurred(invocation, source, uuid, throwable, plugin)) return;
                        try (var connection = plugin.getDataSource().getConnection();
                             var statement = connection.prepareStatement(option.equals("add") ? "INSERT INTO velocity_punishment_whitelist (uuid) VALUES (?);" :
                                     "DELETE FROM velocity_punishment_whitelist WHERE uuid = ?;")) {
                            statement.setString(1, Util.trimUuid(uuid));
                            statement.executeUpdate();
                            source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.success", source, true));
                            if (option.equals("remove")) {
                                plugin.getServer().getPlayer(uuid).ifPresent(player -> player.disconnect(Component.text("You have been blacklisted.").color(NamedTextColor.DARK_RED)));
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            source.sendMessage(plugin.getMessageProvider().internalError(source, true));
                        }
                    }, plugin.getService());
            default -> source.sendMessage(plugin.getMessageProvider().provide("command.whitelist.usage", source, true));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("punishment.command.whitelist");
    }
}
