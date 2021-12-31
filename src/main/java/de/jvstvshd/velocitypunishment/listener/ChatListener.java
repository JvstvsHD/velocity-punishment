package de.jvstvshd.velocitypunishment.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.internal.Util;
import de.jvstvshd.velocitypunishment.punishment.Mute;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.StandardPunishmentType;
import de.jvstvshd.velocitypunishment.punishment.impl.DefaultMute;
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
            event.setResult(PlayerChatEvent.ChatResult.denied());
            event.getPlayer().sendMessage(container.getMute().createFullReason());
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
                service, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).get(5, TimeUnit.SECONDS);
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
