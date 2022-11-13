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

package de.jvstvshd.velocitypunishment.paper.listeners;

import de.jvstvshd.velocitypunishment.api.duration.PunishmentDuration;
import de.jvstvshd.velocitypunishment.common.plugin.MuteData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MuteInformation {

    private final Player player;
    private final UUID punishmentUUID;
    private Component reason;
    private PunishmentDuration duration;

    public MuteInformation(Component reason, PunishmentDuration duration, Player player, UUID punishmentUUID) {
        this.reason = reason;
        this.duration = duration;
        this.player = player;
        this.punishmentUUID = punishmentUUID;
    }

    public static MuteInformation from(MuteData muteData) {
        return new MuteInformation(LegacyComponentSerializer.legacySection().deserialize(muteData.getReason()),
                PunishmentDuration.from(muteData.getExpiration()), Bukkit.getPlayer(muteData.getUuid()), muteData.getPunishmentId());
    }

    public void updateTo(MuteInformation other) {
        synchronized (this) {
            this.reason = other.reason;
            this.duration = other.duration;
        }
    }

    public Component getReason() {
        return reason;
    }

    public MuteInformation setReason(Component reason) {
        this.reason = reason;
        return this;
    }

    public PunishmentDuration getDuration() {
        return duration;
    }

    public MuteInformation setDuration(PunishmentDuration duration) {
        this.duration = duration;
        return this;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getPunishmentUUID() {
        return punishmentUUID;
    }
}
