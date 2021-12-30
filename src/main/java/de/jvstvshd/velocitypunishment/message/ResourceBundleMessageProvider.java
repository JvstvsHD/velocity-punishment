package de.jvstvshd.velocitypunishment.message;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.config.ConfigData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.stream.Stream;

@SuppressWarnings("ClassCanBeRecord")
public class ResourceBundleMessageProvider implements MessageProvider {

    private final ConfigData configData;
    private LocaleProvider localeProvider;

    public ResourceBundleMessageProvider(@NotNull ConfigData configData) {
        this.configData = configData;
        this.localeProvider = new LocaleProvider(configData);
        load();
    }

    private void load() {
        try {
            var registry = TranslationRegistry.create(Key.key("velocity-punishment"));
            registry.defaultLocale(configData.getDefaultLanguage());
            try (Stream<Path> paths = Files.list(Path.of("plugins", "velocity-punishment", "translations"))) {
                paths.filter(path -> path.getFileName().toString().endsWith(".properties")).forEach(path -> {
                    PropertyResourceBundle resource;
                    try {
                        resource = new PropertyResourceBundle(Files.newInputStream(path));
                        var locale = Objects.requireNonNull(Translator.parseLocale(path.getFileName().toString().substring(0, path.getFileName().toString().length() - ".properties".length())));
                        registry.registerAll(locale, resource, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            GlobalTranslator.get().addSource(registry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Component standard(String key, Component def) {
        var rendered = GlobalTranslator.render(Component.translatable(key), configData.getDefaultLanguage());
        if (rendered instanceof TextComponent text && text.content().equalsIgnoreCase(key)) {
            return def;
        }
        return rendered;
    }

    @Override
    public @NotNull
    Component internalError() {
        return standard("error.internal", Component.text("An internal error occurred. Please contact the network administrator.")
                .color(NamedTextColor.DARK_RED));
    }

    @Override
    public @NotNull
    Component prefix() {
        return standard("prefix", Component.text()
                .append(Component.text("[").color(NamedTextColor.GRAY))
                .append(Component.text("Punishment").color(NamedTextColor.YELLOW))
                .append(Component.text("] ").color(NamedTextColor.GRAY)).build());
    }


    @Override
    public @NotNull
    Component provide(String key, CommandSource source, Component... args) {
        return GlobalTranslator.render(Component.translatable(key, args), localeProvider.provideLocale(source));
    }

    @Override
    public @NotNull
    Component provide(String key, Component... args) {
        Objects.requireNonNull(key, "key may not be null");
        var translatable = Component.translatable(key, args);
        if (configData.isForceUsingDefaultLanguage()) {
            return GlobalTranslator.render(translatable, configData.getDefaultLanguage());
        }
        return translatable;
    }


    private Component withPrefix(Component message) {
        Objects.requireNonNull(message);
        return Component.text().append(prefix(), message).build();
    }

    @Override
    public @NotNull
    Component internalError(boolean withPrefix) {
        if (withPrefix) {
            return withPrefix(internalError());
        }
        return internalError();
    }

    @Override
    public @NotNull
    Component provide(String key, CommandSource source, boolean withPrefix, Component... args) {
        if (withPrefix) {
            return withPrefix(provide(key, source, args));
        }
        return provide(key, source, args);
    }

    public LocaleProvider getLocaleProvider() {
        return localeProvider;
    }

    public void setLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    public static class LocaleProvider {
        private final ConfigData configData;

        public LocaleProvider(ConfigData configData) {
            this.configData = configData;
        }

        public Locale provideLocale(CommandSource source) {
            if (configData.isForceUsingDefaultLanguage()) {
                return configData.getDefaultLanguage();
            }
            if (source instanceof Player player) {
                return player.getEffectiveLocale();
            }
            //TODO: Implement correct locale
            return Locale.ENGLISH;
        }
    }
}
