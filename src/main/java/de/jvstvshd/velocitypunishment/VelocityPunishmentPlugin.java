package de.jvstvshd.velocitypunishment;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.jvstvshd.velocitypunishment.commands.BanCommand;
import de.jvstvshd.velocitypunishment.commands.PunishmentCommand;
import de.jvstvshd.velocitypunishment.commands.TempbanCommand;
import de.jvstvshd.velocitypunishment.commands.UnbanCommand;
import de.jvstvshd.velocitypunishment.config.ConfigurationManager;
import de.jvstvshd.velocitypunishment.listener.ConnectListener;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.punishment.impl.StandardPunishmentManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Plugin(id = "velocity-punishment", name = "Velocity Punishment Plugin", version = "1.0.0-SNAPSHOT", description = "A simple punishment plugin for Velocity", authors = {"JvstvsHD"})
public class VelocityPunishmentPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final ConfigurationManager configurationManager;
    private final Path dataDirectory;
    private PunishmentManager punishmentManager;
    private HikariDataSource dataSource;
    private final Map<String, SimpleCommand> commandMap = new HashMap<>();

    @Inject
    public VelocityPunishmentPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.configurationManager = new ConfigurationManager(Paths.get(dataDirectory.toAbsolutePath().toString(), "config.json"));
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws ReflectiveOperationException {
        try {
            configurationManager.load();
        } catch (IOException e) {
            logger.error("Could not load configuration", e);
        }
        dataSource = createDataSource();
        punishmentManager = new StandardPunishmentManager(server, dataSource);
        try {
            initDataSource();
        } catch (SQLException e) {
            logger.error("Could not create table velocity_punishment in database " + dataSource.getDataSourceProperties().get("dataSource.databaseName"), e);
        }
        setup(server.getCommandManager());
        setupListeners(server.getEventManager());
        new MuteListener().inject(null);
    }

    private void setup(CommandManager commandManager) {
        ExecutorService commandService = Executors.newCachedThreadPool();
        commandManager.register(commandManager.metaBuilder("ban").build(), new BanCommand(server, punishmentManager, commandService));
        commandManager.register(commandManager.metaBuilder("tempban").build(), new TempbanCommand(punishmentManager, server, commandService));
        commandManager.register(commandManager.metaBuilder("unban").build(), new UnbanCommand(punishmentManager, dataSource, commandService));
        commandManager.register(commandManager.metaBuilder("punishment").build(), new PunishmentCommand(commandService, punishmentManager));
    }

    private void setupListeners(EventManager eventManager) {
        eventManager.register(this, new ConnectListener(punishmentManager, Executors.newCachedThreadPool(), server));
    }

    public ProxyServer getServer() {
        return server;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    private HikariDataSource createDataSource() {
        var dbData = configurationManager.getConfiguration().getDataBaseData();
        var properties = new Properties();
        properties.setProperty("dataSource.databaseName", dbData.getDatabase());
        properties.setProperty("dataSource.serverName", dbData.getHost());
        properties.setProperty("dataSource.portNumber", dbData.getPort());
        properties.setProperty("dataSourceClassName", org.mariadb.jdbc.MariaDbDataSource.class.getName());
        properties.setProperty("dataSource.user", dbData.getUsername());
        properties.setProperty("dataSource.password", dbData.getPassword());
        var config = new HikariConfig(properties);
        config.setPoolName("velocity-punishment-hikari");
        return new HikariDataSource(config);
    }

    private void initDataSource() throws SQLException {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement =
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS velocity_punishment (uuid  VARCHAR (36), name VARCHAR (16), type VARCHAR (1000), expiration DATETIME (6), " +
                        "reason VARCHAR (1000), punishment_id VARCHAR (36))")) {
            statement.execute();
        }
    }

    @Deprecated(forRemoval = true)
    private void registerCommand(CommandMeta meta, SimpleCommand command) {
        server.getCommandManager().register(meta, command);
    }

    public Map<String, SimpleCommand> getCommandMap() {
        return ImmutableMap.copyOf(commandMap);
    }
}
