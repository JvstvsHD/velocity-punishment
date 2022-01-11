package de.jvstvshd.velocitypunishment.config;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class DataBaseData {
    private String host = "";
    private String password = "";
    private String username = "";
    private String database = "";
    private String port = "";

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getDatabase() {
        return database;
    }

    public String getPort() {
        return port;
    }
}
