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

package de.jvstvshd.velocitypunishment.impl;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.*;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import net.kyori.adventure.text.Component;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class DefaultKick extends AbstractPunishment implements Kick {

    public DefaultKick(UUID playerUuid, Component reason, DataSource dataSource, ExecutorService service, PunishmentManager punishmentManager, UUID punishmentUuid, PlayerResolver playerResolver, MessageProvider messageProvider) {
        super(playerUuid, reason, dataSource, service, punishmentManager, punishmentUuid, playerResolver, messageProvider);
    }

    @Override
    public CompletableFuture<Punishment> punish() {
        Optional<Player> optPlayer = getPunishmentManager().getServer().getPlayer(getPlayerUuid());
        if (optPlayer.isEmpty())
            throw new IllegalArgumentException("Invalid player was not found. This could be because " +
                    "a) this player does not exist (wrong uuid)\n" +
                    "b) the player is not online and so can't be kicked.");
        optPlayer.get().disconnect(getReason());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public PunishmentType getType() {
        return StandardPunishmentType.KICK;
    }

    @Override
    public Component createFullReason(CommandSource source) {
        return getReason();
    }
}
