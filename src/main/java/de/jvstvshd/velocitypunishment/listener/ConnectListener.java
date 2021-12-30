package de.jvstvshd.velocitypunishment.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.punishment.Ban;
import de.jvstvshd.velocitypunishment.punishment.Mute;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.StandardPunishmentType;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public record ConnectListener(VelocityPunishmentPlugin plugin,
                              ExecutorService service, ProxyServer proxyServer, ChatListener chatListener) {

    @Subscribe()
    public void onConnect(LoginEvent event) {
        plugin.getPunishmentManager().getPunishments(event.getPlayer().getUniqueId(), service, StandardPunishmentType.BAN,
                StandardPunishmentType.PERMANENT_BAN, StandardPunishmentType.MUTE, StandardPunishmentType.PERMANENT_MUTE).whenCompleteAsync((punishments, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                event.setResult(ResultedEvent.ComponentResult.denied(plugin.getMessageProvider().internalError()));
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
                ban.cancel().whenCompleteAsync((unused, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                        return;
                    }
                    proxyServer.getConsoleCommandSource().sendMessage(Component.text()
                            .append(Component.text("Ban ").color(NamedTextColor.GREEN),
                                    Component.text("'" + ban.getPunishmentUuid().toString().toLowerCase() + "'")
                                            .color(NamedTextColor.YELLOW),
                                    Component.text("was annulled.").color(NamedTextColor.GREEN)));
                }, service);
            }
        });
    }
}
