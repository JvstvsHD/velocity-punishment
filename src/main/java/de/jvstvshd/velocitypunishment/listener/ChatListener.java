package de.jvstvshd.velocitypunishment.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.punishment.Mute;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.punishment.StandardPunishmentType;
import de.jvstvshd.velocitypunishment.punishment.impl.StandardMute;
import de.jvstvshd.velocitypunishment.util.Util;

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
    private final PunishmentManager punishmentManager;
    private final ExecutorService service;

    public ChatListener(PunishmentManager punishmentManager, ExecutorService service) {
        this.punishmentManager = punishmentManager;
        this.service = service;
        mutes = new HashMap<>();
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        MuteContainer container = mutes.get(player.getUniqueId());
        if (container != null) {
            if (container.getType() == MuteType.LOADING || container.getType() == MuteType.NOT_MUTED) {
                return;
            }
            event.setResult(PlayerChatEvent.ChatResult.denied());
            event.getPlayer().sendMessage(container.getMute().createFullReason());
        } else {
            update(player.getUniqueId());
            onChat(event);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        mutes.remove(event.getPlayer().getUniqueId());
    }

    public void update(UUID uuid) {
        try {
            List<Punishment> punishments = punishmentManager.getPunishments(uuid,
                    service, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).get(5, TimeUnit.SECONDS);
            if (!punishments.isEmpty()) {
                Mute mute = Util.getLongestPunishment(Util.convert(punishments));
                mutes.put(uuid, new MuteContainer().setMute(mute));
            } else {
                mutes.put(uuid, new MuteContainer());
            }
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, MuteContainer> getMutes() {
        return mutes;
    }

    public enum MuteType {
        MUTED,
        NOT_MUTED,
        LOADING;
    }

    public static class MuteContainer {
        private MuteType type;
        private Mute mute;

        public MuteContainer() {
            type = MuteType.NOT_MUTED;
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
            if (mute != null && ((StandardMute)mute).isValid())
                this.type = MuteType.MUTED;
            this.mute = mute;
            return this;
        }
    }
}
