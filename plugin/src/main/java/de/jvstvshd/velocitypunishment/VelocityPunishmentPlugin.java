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

package de.jvstvshd.velocitypunishment;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.jvstvshd.velocitypunishment.api.VelocityPunishment;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import de.jvstvshd.velocitypunishment.commands.*;
import de.jvstvshd.velocitypunishment.config.ConfigurationManager;
import de.jvstvshd.velocitypunishment.impl.DefaultPlayerResolver;
import de.jvstvshd.velocitypunishment.impl.DefaultPunishmentManager;
import de.jvstvshd.velocitypunishment.listener.ChatListener;
import de.jvstvshd.velocitypunishment.listener.ConnectListener;
import de.jvstvshd.velocitypunishment.message.ResourceBundleMessageProvider;
import net.kyori.adventure.text.Component;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Plugin(id = "velocity-punishment", name = "Velocity Punishment Plugin", version = "1.0.0-SNAPSHOT", description = "A simple punishment plugin for Velocity", authors = {"JvstvsHD"})
public class VelocityPunishmentPlugin implements VelocityPunishment {

    private final ProxyServer server;
    private final Logger logger;
    private final ConfigurationManager configurationManager;
    private final ExecutorService service = Executors.newCachedThreadPool();
    private PunishmentManager punishmentManager;
    private HikariDataSource dataSource;
    private PlayerResolver playerResolver;
    private MessageProvider messageProvider;

    private static final String MUTES_DISABLED_STRING = """
            Since 1.19.1, cancelling chat messages on proxy is not possible anymore. Therefore, we have to listen to the chat event on the actual game server. This means
            that there has to be a spigot/paper extension to this plugin which is not yet available unless there's a possibility. Therefore all mute related features won't work at the moment.
            If you use 1.19 or lower you will not be affected by this.The progress of the extension can be found here: https://github.com/JvstvsHD/velocity-punishment/issues/6""".replace("\n", " ");

    /**
     * Since 1.19.1, cancelling chat messages on proxy is not possible anymore. Therefore, we have to listen to the chat event on the actual game server. This means
     * that there has to be a spigot/paper extension to this plugin which is not yet available unless there's a possibility. Therefore all mute related features are disabled for now.
     * If you use 1.19 or lower you will not be affected by this.The progress of the extension can be found <a href=https://github.com/JvstvsHD/velocity-punishment/issues/6>here</a>.
     * For this reason, every mute related feature is deprecated and marked as for removal until this extension is available.
     */
    public static final Component MUTES_DISABLED = Component.text(MUTES_DISABLED_STRING);

    @Inject
    public VelocityPunishmentPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.configurationManager = new ConfigurationManager(Paths.get(dataDirectory.toAbsolutePath().toString(), "config.json"));
        this.playerResolver = new DefaultPlayerResolver(server);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            configurationManager.load();
            if (configurationManager.getConfiguration().isWhitelistActivated()) {
                logger.info("Whitelist is activated. This means that nobody can join this server beside players you have explicitly allowed to join this server via /whitelist <player> add");
            }
            this.messageProvider = new ResourceBundleMessageProvider(configurationManager.getConfiguration());
        } catch (IOException e) {
            logger.error("Could not load configuration", e);
        }
        dataSource = createDataSource();
        punishmentManager = new DefaultPunishmentManager(server, dataSource, this);
        try {
            initDataSource();
        } catch (SQLException e) {
            logger.error("Could not create table velocity_punishment in database " + dataSource.getDataSourceProperties().get("dataSource.databaseName"), e);
        }
        setup(server.getCommandManager(), server.getEventManager());
        logger.info("Velocity Punishment Plugin v1.0.0 has been loaded");
    }

    private void setup(CommandManager commandManager, EventManager eventManager) {
        ChatListener chatListener = new ChatListener(this);
        eventManager.register(this, new ConnectListener(this, Executors.newCachedThreadPool(), server, chatListener));
        logger.info(MUTES_DISABLED_STRING);
        eventManager.register(this, chatListener);

        commandManager.register(BanCommand.banCommand(this));

        commandManager.register(TempbanCommand.tempbanCommand(this));
        commandManager.register(PunishmentRemovalCommand.unbanCommand(this));
        commandManager.register(PunishmentRemovalCommand.unmuteCommand(this));
        commandManager.register(PunishmentCommand.punishmentCommand(this));
        commandManager.register(MuteCommand.muteCommand(this, chatListener));
        commandManager.register(commandManager.metaBuilder("tempmute").build(), new TempmuteCommand(this, chatListener));
        commandManager.register(KickCommand.kickCommand(this));
        commandManager.register(WhitelistCommand.whitelistCommand(this));
    }

    private HikariDataSource createDataSource() {
        var dbData = configurationManager.getConfiguration().getDataBaseData();
        var properties = new Properties();
        properties.setProperty("dataSource.databaseName", dbData.getDatabase());
        properties.setProperty("dataSource.serverName", dbData.getHost());
        properties.setProperty("dataSource.portNumber", dbData.getPort());
        properties.setProperty("dataSourceClassName", PGSimpleDataSource.class.getName());
        properties.setProperty("dataSource.user", dbData.getUsername());
        properties.setProperty("dataSource.password", dbData.getPassword());
        var config = new HikariConfig(properties);
        config.setPoolName("velocity-punishment-hikari");
        return new HikariDataSource(config);
    }

    private void initDataSource() throws SQLException {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement =
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS velocity_punishment (uuid  VARCHAR (36), name VARCHAR (16), type VARCHAR (1000), expiration TIMESTAMP (6), " +
                        "reason VARCHAR (1000), punishment_id VARCHAR (36))")) {
            statement.execute();
        }
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement =
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS velocity_punishment_whitelist (uuid VARCHAR (36))")) {
            statement.execute();
        }
    }

    @Override
    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    @Override
    public void setPunishmentManager(PunishmentManager punishmentManager) {
        this.punishmentManager = punishmentManager;
    }

    @Override
    public PlayerResolver getPlayerResolver() {
        return playerResolver;
    }

    @Override
    public void setPlayerResolver(PlayerResolver playerResolver) {
        this.playerResolver = playerResolver;
    }

    @Override
    public ProxyServer getServer() {
        return server;
    }

    @Override
    public ExecutorService getService() {
        return service;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    @Override
    public void setMessageProvider(MessageProvider messageProvider) {
        this.messageProvider = messageProvider;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean whitelistActive() {
        return configurationManager.getConfiguration().isWhitelistActivated();
    }
}
