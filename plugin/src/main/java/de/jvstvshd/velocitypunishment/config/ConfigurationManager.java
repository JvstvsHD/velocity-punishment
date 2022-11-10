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

package de.jvstvshd.velocitypunishment.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationManager {

    private final Path path;
    private final ObjectMapper objectMapper;
    private ConfigData configData;

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
                try (var writer = objectMapper.writerWithDefaultPrettyPrinter().writeValues(path.toFile())) {
                    writer.write(new ConfigData());
                }
            }
        }

    }

    public void load() throws IOException {
        create();
        configData = objectMapper.readValue(path.toFile(), ConfigData.class);
    }

    public void save() throws IOException {
        try (var writer = objectMapper.writerWithDefaultPrettyPrinter().writeValues(path.toFile())) {
            writer.write(configData);
        }
    }

    public ConfigData getConfiguration() {
        return configData;
    }
}