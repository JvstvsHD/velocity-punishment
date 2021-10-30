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
                              ExecutorService service, ProxyServer proxyServer) {

    @Subscribe()
    public void onConnect(LoginEvent event) throws ExecutionException, InterruptedException, TimeoutException {
        List<Punishment> punishments = punishmentManager.getPunishments(event.getPlayer().getUniqueId(), service, StandardPunishmentType.BAN,
                StandardPunishmentType.PERMANENT_BAN, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).get(10, TimeUnit.SECONDS);
        List<Ban> bans = new ArrayList<>();
        List<Mute> mutes = new ArrayList<>();
        for (Punishment punishment : punishments) {
            if (punishment instanceof Ban velocityBan)
                bans.add(velocityBan);
            if (punishment instanceof Mute mute)
                mutes.add(mute);
        }

        if (bans.isEmpty())
            return;
        final Ban ban = Util.getLongestPunishment(bans);
        if (ban.isOngoing()) {
            Component deny = ban.createFullReason();
            event.setResult(ResultedEvent.ComponentResult.denied(deny));
        } else {
            service.execute(() -> {
                try {
                    boolean result = ban.annul().get(5, TimeUnit.SECONDS);
                    if (result) {
                        proxyServer.getConsoleCommandSource().sendMessage(Component.text()
                                .append(Component.text("Ban ").color(NamedTextColor.GREEN),
                                        Component.text("'" + ban.getPunishmentUuid().toString().toLowerCase() + "'")
                                                .color(NamedTextColor.YELLOW),
                                        Component.text("was annulled.").color(NamedTextColor.GREEN)));
                    } else {
                        proxyServer.getConsoleCommandSource().sendMessage(Component.text("Could not annul ban '" +
                                ban.getPunishmentUuid().toString().toLowerCase() + "'.").color(NamedTextColor.RED));
                    }
                } catch (InterruptedException | TimeoutException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
        }

    }
}
