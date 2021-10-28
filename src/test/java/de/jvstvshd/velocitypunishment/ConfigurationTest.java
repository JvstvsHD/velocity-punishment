package de.jvstvshd.velocitypunishment;

import de.jvstvshd.velocitypunishment.config.ConfigurationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class ConfigurationTest {

    @Test
    public void testConfig() throws Exception {
        var manager = new ConfigurationManager(Paths.get("config.json"));
        manager.load();
        Component component = LegacyComponentSerializer.legacy('&').deserialize("&aHI!!!");
        System.out.println(LegacyComponentSerializer.legacy(LegacyComponentSerializer.SECTION_CHAR).serialize(component));
    }


}
