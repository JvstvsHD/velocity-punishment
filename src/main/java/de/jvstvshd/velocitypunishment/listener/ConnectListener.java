package de.jvstvshd.velocitypunishment.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.punishment.*;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public record ConnectListener(PunishmentManager punishmentManager,
                              ExecutorService service, ProxyServer proxyServer, ChatListener chatListener) {

    @Subscribe()
    public void onConnect(LoginEvent event) {
        List<Punishment> punishments;
        try {
            punishments = punishmentManager.getPunishments(event.getPlayer().getUniqueId(), service, StandardPunishmentType.BAN,
                    StandardPunishmentType.PERMANENT_BAN, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            return;
        }
        List<Ban> bans = new ArrayList<>();
        List<Mute> mutes = new ArrayList<>();
        for (Punishment punishment : punishments) {
            if (punishment instanceof Ban velocityBan)
                bans.add(velocityBan);
            if (punishment instanceof Mute mute)
                mutes.add(mute);
        }
        Mute longestMute = Util.getLongestPunishment(mutes);
        if (longestMute != null) {
            chatListener.getMutes().put(event.getPlayer().getUniqueId(), new ChatListener.MuteContainer().setMute(longestMute));
        }
        if (bans.isEmpty())
            return;
        final Ban ban = Util.getLongestPunishment(bans);
        if (ban == null)
            return;
        if (ban.isOngoing()) {
            Component deny = ban.createFullReason();
            event.setResult(ResultedEvent.ComponentResult.denied(deny));
        } else {
            service.execute(() -> ban.cancel().whenCompleteAsync((unused, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                    return;
                }
                proxyServer.getConsoleCommandSource().sendMessage(Component.text()
                        .append(Component.text("Ban ").color(NamedTextColor.GREEN),
                                Component.text("'" + ban.getPunishmentUuid().toString().toLowerCase() + "'")
                                        .color(NamedTextColor.YELLOW),
                                Component.text("was annulled.").color(NamedTextColor.GREEN)));
            }));
        }
    }
}
