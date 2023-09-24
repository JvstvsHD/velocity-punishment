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
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.zaxxer.hikari.HikariDataSource;
import de.chojo.sadu.databases.PostgreSql;
import de.chojo.sadu.datasource.DataSourceCreator;
import de.chojo.sadu.updater.SqlUpdater;
import de.chojo.sadu.wrapper.QueryBuilderConfig;
import de.jvstvshd.velocitypunishment.api.VelocityPunishment;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.api.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.api.punishment.util.PlayerResolver;
import de.jvstvshd.velocitypunishment.commands.*;
import de.jvstvshd.velocitypunishment.common.plugin.MuteData;
import de.jvstvshd.velocitypunishment.config.ConfigurationManager;
import de.jvstvshd.velocitypunishment.impl.DefaultPlayerResolver;
import de.jvstvshd.velocitypunishment.impl.DefaultPunishmentManager;
import de.jvstvshd.velocitypunishment.listener.ConnectListener;
import de.jvstvshd.velocitypunishment.message.ResourceBundleMessageProvider;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Plugin(id = "velocity-punishment", name = "Velocity Punishment Plugin", version = "1.0.0-SNAPSHOT", description = "A simple punishment plugin for Velocity", authors = {"JvstvsHD"})
public class VelocityPunishmentPlugin implements VelocityPunishment {

    private final ProxyServer server;
    private final Logger logger;
    private final ConfigurationManager configurationManager;
    private final ExecutorService service = Executors.newCachedThreadPool();
    public static final ChannelIdentifier MUTE_DATA_CHANNEL_IDENTIFIER = MinecraftChannelIdentifier.from(MuteData.MUTE_DATA_CHANNEL_IDENTIFIER);
    private PunishmentManager punishmentManager;
    private HikariDataSource dataSource;
    private PlayerResolver playerResolver;
    private MessageProvider messageProvider;

    private static final String MUTES_DISABLED_STRING = """
            Since 1.19.1, cancelling chat messages on proxy is not possible anymore. Therefore, we have to listen to the chat event on the actual game server. This means
            that there has to be a spigot/paper extension to this plugin which is not yet available unless there's a possibility. Therefore all mute related features won't work at the moment.
            If you use 1.19 or lower you will not be affected by this.The progress of the extension can be found here: https://github.com/JvstvsHD/velocity-punishment/issues/6""".replace("\n", " ");
    private final MessagingChannelCommunicator communicator;

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
        this.communicator = new MessagingChannelCommunicator(server, logger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("An error occurred in thread " + t.getName(), e));
        try {
            configurationManager.load();
            if (configurationManager.getConfiguration().isWhitelistActivated()) {
                logger.info("Whitelist is activated. This means that nobody can join this server beside players you have explicitly allowed to join this server via /whitelist <player> add");
            }
            this.messageProvider = new ResourceBundleMessageProvider(configurationManager.getConfiguration());
        } catch (IOException e) {
            logger.error("Could not load configuration", e);
        }
        QueryBuilderConfig.setDefault(QueryBuilderConfig.builder().withExceptionHandler(e -> logger.error("An error occurred during a database request", e)).build());
        dataSource = createDataSource();
        punishmentManager = new DefaultPunishmentManager(server, dataSource, this);
        try {
            updateDatabase();
            //initDataSource();
        } catch (SQLException | IOException e) {
            logger.error("Could not create table velocity_punishment in database " + dataSource.getDataSourceProperties().get("dataSource.databaseName"), e);
        }
        setup(server.getCommandManager(), server.getEventManager());
        logger.info("Velocity Punishment Plugin v1.0.0 has been loaded");
    }

    private void setup(CommandManager commandManager, EventManager eventManager) {
        eventManager.register(this, communicator);
        eventManager.register(this, new ConnectListener(this, Executors.newCachedThreadPool(), server));
        logger.info(MUTES_DISABLED_STRING);

        commandManager.register(BanCommand.banCommand(this));

        commandManager.register(TempbanCommand.tempbanCommand(this));
        commandManager.register(PunishmentRemovalCommand.unbanCommand(this));
        commandManager.register(PunishmentRemovalCommand.unmuteCommand(this));
        commandManager.register(PunishmentCommand.punishmentCommand(this));
        commandManager.register(MuteCommand.muteCommand(this));
        commandManager.register(TempmuteCommand.tempmuteCommand(this));
        commandManager.register(KickCommand.kickCommand(this));
        commandManager.register(WhitelistCommand.whitelistCommand(this));
    }

    private HikariDataSource createDataSource() {
        var dbData = configurationManager.getConfiguration().getDataBaseData();
        //TODO add config option for sql type
        return DataSourceCreator.create(PostgreSql.get())
                .configure(jdbcConfig -> jdbcConfig.host(dbData.getHost())
                        .port(dbData.getPort())
                        .database(dbData.getDatabase())
                        .user(dbData.getUsername())
                        .password(dbData.getPassword())
                        .applicationName("Velocity Punishment Plugin"))
                .create().withMaximumPoolSize(dbData.getMaxPoolSize())
                .withMinimumIdle(dbData.getMinIdle())
                .withPoolName("velocity-punishment-hikari")
                .build();
    }

    private void updateDatabase() throws IOException, SQLException {
        //TODO config option for sql type
        SqlUpdater.builder(dataSource, PostgreSql.get())
                .setSchemas("punishment")
                .execute();

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

    @NotNull
    public MessagingChannelCommunicator communicator() {
        return communicator;
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
