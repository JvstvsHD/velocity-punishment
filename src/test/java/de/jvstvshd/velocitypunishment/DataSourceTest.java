package de.jvstvshd.velocitypunishment;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.jvstvshd.velocitypunishment.config.ConfigurationManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

public class DataSourceTest {

    private final ConfigurationManager configurationManager = new ConfigurationManager(Paths.get("config.json"));

    @Test
    public void test() throws IOException, SQLException {
        configurationManager.load();
        HikariDataSource dataSource = createDataSource();
        UUID uuid = UUID.randomUUID();
        System.out.println("uuid.toString() = " + uuid.toString().replace('-', Character.MIN_VALUE).length());
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement =
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS velocity_punishment (uuid  VARCHAR (36), name VARCHAR (16), type VARCHAR (1000), expiration DATETIME (4), " +
                        "reason VARCHAR (1000), punishment_id VARCHAR (36))")) {
            statement.execute();
        }
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
}
