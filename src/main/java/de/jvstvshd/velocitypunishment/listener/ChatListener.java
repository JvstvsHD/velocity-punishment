package de.jvstvshd.velocitypunishment.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import de.jvstvshd.velocitypunishment.punishment.Mute;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.punishment.StandardPunishmentType;
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

    private final Map<UUID, Mute> mutes;
    private final PunishmentManager punishmentManager;
    private final ExecutorService service;

    public ChatListener(PunishmentManager punishmentManager, ExecutorService service) {
        this.punishmentManager = punishmentManager;
        this.service = service;
        mutes = new HashMap<>();
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (mutes.containsKey(event.getPlayer().getUniqueId())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            event.getPlayer().sendMessage(mutes.get(event.getPlayer().getUniqueId()).getReason());
        } else {
            try {
                List<Punishment> punishments = punishmentManager.getPunishments(event.getPlayer().getUniqueId(),
                        service, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).get(5, TimeUnit.SECONDS);
                if (!punishments.isEmpty()) {
                    Mute mute = Util.getLongestPunishment(Util.convert(punishments));
                    mutes.put(event.getPlayer().getUniqueId(), mute);
                    event.setResult(PlayerChatEvent.ChatResult.denied());
                    event.getPlayer().sendMessage(mute.getReason());
                } else {
                    mutes.put(event.getPlayer().getUniqueId(), null);
                }
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<UUID, Mute> getMutes() {
        return mutes;
    }
}
