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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.ByteStreams;
import de.jvstvshd.velocitypunishment.common.plugin.MuteData;
import de.jvstvshd.velocitypunishment.paper.VelocityPunishmentPaperPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class MessagingChannelListener implements PluginMessageListener {

    private final VelocityPunishmentPaperPlugin plugin;

    public MessagingChannelListener(VelocityPunishmentPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        var input = ByteStreams.newDataInput(message);
        var content = input.readUTF();
        System.out.println("Received message from channel " + channel + ", message: " + content);
        MuteData data;
        try {
            data = MuteData.OBJECT_MAPPER.readValue(content, MuteData.class);
        } catch (JsonProcessingException e) {
            plugin.getSLF4JLogger().error("Could not parse MuteData", e);
            return;
        }
        var mute = MuteInformation.from(data);
        switch (data.getType()) {
            case MuteData.ADD -> plugin.cachedMutes().add(mute);
            case MuteData.REMOVE ->
                    plugin.cachedMutes().removeIf(muteInformation -> muteInformation.getPunishmentUUID().equals(mute.getPunishmentUUID()));
            case MuteData.UPDATE ->
                    plugin.cachedMutes().stream().filter(muteInformation -> muteInformation.getPunishmentUUID().equals(mute.getPunishmentUUID())).findFirst().ifPresent(muteInformation -> muteInformation.updateTo(mute));
        }
    }
}
