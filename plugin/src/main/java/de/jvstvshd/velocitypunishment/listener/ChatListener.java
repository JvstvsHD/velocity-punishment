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

package de.jvstvshd.velocitypunishment.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.punishment.Mute;
import de.jvstvshd.velocitypunishment.api.punishment.Punishment;
import de.jvstvshd.velocitypunishment.api.punishment.StandardPunishmentType;
import de.jvstvshd.velocitypunishment.impl.DefaultMute;
import de.jvstvshd.velocitypunishment.internal.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChatListener {

    private final Map<UUID, MuteContainer> mutes;
    private final ExecutorService service;
    private final VelocityPunishmentPlugin plugin;

    public ChatListener(VelocityPunishmentPlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getService();
        mutes = new HashMap<>();
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        MuteContainer container = mutes.get(player.getUniqueId());
        if (container != null) {
            if (container.getType() == MuteType.NOT_MUTED) {
                return;
            }
            if (container.getType() == MuteType.LOADING) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                event.getPlayer().sendMessage(Component.text("Please wait a moment...").color(NamedTextColor.GRAY));
                return;
            }
            if (!container.getMute().isOngoing()) {
                container.setType(MuteType.LOADING);
                container.getMute().cancel().whenCompleteAsync((punishment, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                        event.getPlayer().sendMessage(plugin.getMessageProvider().internalError(event.getPlayer(), true));
                    }
                    container.setMute(null);
                    try {
                        update(event.getPlayer().getUniqueId());
                    } catch (ExecutionException | InterruptedException | TimeoutException e) {
                        e.printStackTrace();
                    }
                });
                return;
            }
            event.setResult(PlayerChatEvent.ChatResult.denied());
            event.getPlayer().sendMessage(container.getMute().createFullReason(event.getPlayer()));
        } else {
            try {
                update(player.getUniqueId());
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                e.printStackTrace();
                event.getPlayer().sendMessage(plugin.getMessageProvider().internalError(event.getPlayer(), true));
            }
            onChat(event);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        mutes.remove(event.getPlayer().getUniqueId());
    }

    public void update(UUID uuid) throws ExecutionException, InterruptedException, TimeoutException {
        List<Punishment> punishments = plugin.getPunishmentManager().getPunishments(uuid,
                service, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).get(7, TimeUnit.SECONDS);
        if (!punishments.isEmpty()) {
            Mute mute = Util.getLongestPunishment(Util.convert(punishments));
            mutes.put(uuid, new MuteContainer().setMute(mute));
        } else {
            mutes.put(uuid, new MuteContainer());
        }
    }

    public Map<UUID, MuteContainer> getMutes() {
        return mutes;
    }

    public enum MuteType {
        MUTED,
        NOT_MUTED,
        LOADING
    }

    public static class MuteContainer {
        private MuteType type;
        private Mute mute;

        public MuteContainer() {
            type = MuteType.NOT_MUTED;
        }

        public MuteContainer(boolean muted) {
            type = muted ? MuteType.MUTED : MuteType.NOT_MUTED;
        }

        public MuteType getType() {
            return type;
        }

        public MuteContainer setType(MuteType type) {
            this.type = type;
            return this;
        }

        public Mute getMute() {
            return mute;
        }

        public MuteContainer setMute(Mute mute) {
            if (mute != null && ((DefaultMute) mute).isValid())
                this.type = MuteType.MUTED;
            this.mute = mute;
            return this;
        }
    }
}
