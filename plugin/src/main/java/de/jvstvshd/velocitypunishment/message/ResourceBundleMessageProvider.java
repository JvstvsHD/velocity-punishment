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

package de.jvstvshd.velocitypunishment.message;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.VelocityPunishmentPlugin;
import de.jvstvshd.velocitypunishment.api.message.MessageProvider;
import de.jvstvshd.velocitypunishment.config.ConfigData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class ResourceBundleMessageProvider implements MessageProvider {

    private final ConfigData configData;
    private final LocaleProvider localeProvider;
    private static List<PropertyResourceBundle> bundles; //temporary workaround for avoiding legacy color codes

    static {
        try {
            var registry = TranslationRegistry.create(Key.key("velocity-punishment"));
            registry.defaultLocale(Locale.ENGLISH);
            var baseDir = Path.of("plugins", "velocity-punishment", "translations");
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
            try (Stream<Path> paths = Files.list(baseDir)) {
                var bundles = new ArrayList<PropertyResourceBundle>();
                paths.filter(path -> path.getFileName().toString().endsWith(".properties")).forEach(path -> {
                    PropertyResourceBundle resource;
                    try {
                        resource = new PropertyResourceBundle(Files.newInputStream(path));
                        bundles.add(resource);
                        var locale = locale(path.getFileName().toString());//Objects.requireNonNull(Translator.parseLocale(path.getFileName().toString().substring(0, path.getFileName().toString().length() - ".properties".length())));
                        registry.registerAll(locale, resource, false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                ResourceBundleMessageProvider.bundles = Collections.unmodifiableList(bundles);
                try (JarFile jar = new JarFile(new File(VelocityPunishmentPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI()))) {
                    for (JarEntry translationEntry : jar.stream().filter(jarEntry -> jarEntry.getName().toLowerCase().contains("translations") && !jarEntry.isDirectory()).toList()) {
                        var path = Path.of(baseDir.toString(), translationEntry.getName().split("/")[1]);
                        if (Files.exists(path)) {
                            continue;
                        }
                        System.out.println("copying translation file " + translationEntry.getName());
                        Files.copy(Objects.requireNonNull(VelocityPunishmentPlugin.class.getResourceAsStream("/" + translationEntry.getName())), path);
                    }
                }
            }
            GlobalTranslator.translator().addSource(registry);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public ResourceBundleMessageProvider(@NotNull ConfigData configData) {
        this.configData = configData;
        this.localeProvider = new LocaleProvider(configData);
    }

    @Override
    public @NotNull Component prefix() {
        return standard("prefix", Locale.ROOT);
    }

    private Component standard(String key, CommandSource source) {
        var configLocale = configData.getForcedLanguage();
        var locale = configLocale == null ? localeProvider.provideLocale(source) : configLocale;
        return standard(key, locale);
    }

    private static Locale locale(String fileName) {
        return Objects.requireNonNull(Translator.parseLocale(fileName.substring(0, fileName.length() - ".properties".length())));
    }

    @Override
    public @NotNull
    Component internalError(CommandSource source) {
        return standard("error.internal", source);
    }

    @Override
    public @NotNull
    Component prefix(CommandSource source) {
        return standard("prefix", source);
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
        if (configData.getForcedLanguage() != null) {
            return GlobalTranslator.render(translatable, configData.getForcedLanguage());
        }
        return translatable;
    }


    private Component withPrefix(Component message, CommandSource source) {
        Objects.requireNonNull(message);
        return Component.text().append(prefix(source), message).build();
    }

    @Override
    public @NotNull
    Component internalError(CommandSource source, boolean withPrefix) {
        if (withPrefix) {
            return withPrefix(internalError(source), source);
        }
        return internalError(source);
    }

    @Override
    public @NotNull
    Component provide(String key, CommandSource source, boolean withPrefix, Component... args) {
        if (withPrefix) {
            return withPrefix(provide(key, source, args), source);
        }
        return provide(key, source, args);
    }

    private Component standard(String key, Locale locale) {
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        //temporary workaround for avoiding legacy color codes
        //TODO: replace with complete MiniMessage support
        Locale finalLocale = locale;
        var resourceBundle = bundles.stream().filter(bundle -> finalLocale.getISO3Language().equals(Objects.requireNonNullElse(Translator.parseLocale(bundle.getString("locale")), Locale.UK).getISO3Language()))
                .findFirst().orElseThrow(() -> new IllegalStateException("No bundle found for locale " + finalLocale));
        return MiniMessage.miniMessage().deserialize(resourceBundle.getString(key));
        /*var rendered = GlobalTranslator.render(Component.translatable(key), locale);
        if (rendered instanceof TextComponent textComponent) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(textComponent.content());
        }
        return rendered;*/
    }

    public static class LocaleProvider {
        private final ConfigData configData;

        public LocaleProvider(ConfigData configData) {
            this.configData = configData;
        }

        public Locale provideLocale(CommandSource source) {
            if (configData.getForcedLanguage() != null) {
                return configData.getForcedLanguage();
            }
            if (source instanceof Player player) {
                var effectiveLocale = player.getEffectiveLocale();
                return effectiveLocale == null ? Locale.getDefault() : effectiveLocale;
            }
            return Locale.getDefault();
        }
    }
}
