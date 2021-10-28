package de.jvstvshd.velocitypunishment.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationManager {

    private final Path path;
    private final ObjectMapper objectMapper;
    private Configuration configuration;

    public ConfigurationManager(Path path) {
        this.path = path;
        this.objectMapper = new ObjectMapper();
    }

    private void create() throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        boolean write = false;
        if (!Files.exists(path)) {
            Files.createFile(path);
            write = true;
        }
        try (FileChannel channel = FileChannel.open(path)) {
            if (channel.size() <= 0 || write) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValues(path.toFile()).write(new Configuration());
            }
        }

    }

    public void load() throws IOException {
        create();
        configuration = objectMapper.readValue(path.toFile(), Configuration.class);
    }

    public void save() throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValues(path.toFile()).write(configuration);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
