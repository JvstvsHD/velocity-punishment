package de.jvstvshd.velocitypunishment.api;

import com.velocitypowered.api.proxy.ProxyServer;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;

import java.util.concurrent.ExecutorService;

public interface VelocityPunishment {

    PunishmentManager getPunishmentManager();

    void setPunishmentManager(PunishmentManager punishmentManager);

    PlayerResolver getPlayerResolver();

    void setPlayerResolver(PlayerResolver playerResolver);

    ProxyServer getServer();

    ExecutorService getService();

    MessageProvider getMessageProvider();

    void setMessageProvider(MessageProvider messageProvider);
}
