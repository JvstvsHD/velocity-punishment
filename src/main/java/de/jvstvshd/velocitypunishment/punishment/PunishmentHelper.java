package de.jvstvshd.velocitypunishment.punishment;

import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.util.PlayerResolver;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class PunishmentHelper {

    public Component buildPunishmentData(Punishment punishment) {
        return Component.text()
                .append(Component.text("Type: ").color(NamedTextColor.AQUA),
                        Component.text(punishment.getType().getName()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        Component.text("reason: ").color(NamedTextColor.AQUA),
                        punishment.getReason(),
                        Component.newline(),
                        punishment instanceof TemporalPunishment temporalPunishment ?
                                buildPunishmentDataTemporal(temporalPunishment) : Component.text(""),
                        Component.newline()
                )
                .build();
    }

    public Component buildPunishmentDataTemporal(TemporalPunishment punishment) {
        return punishment.isPermanent() ? Component.text("permanent").color(NamedTextColor.RED) : Component.text()
                .append(Component.text("duration: ").color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().getRemainingDuration()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        Component.text("end of punishment: ").color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().getEnd()).color(NamedTextColor.YELLOW),
                        Component.newline(),
                        Component.text("initial duration: ").color(NamedTextColor.AQUA),
                        Component.text(punishment.getDuration().getInitialDuration()).color(NamedTextColor.YELLOW))
                .build();
    }

    public Optional<PunishmentDuration> parseDuration(int argumentIndex, SimpleCommand.Invocation invocation) {
        try {
            return Optional.ofNullable(PunishmentDuration.parse(invocation.arguments()[argumentIndex]));
        } catch (IllegalArgumentException e) {
            invocation.source().sendMessage(Component.text().append(Component.text("Cannot parse duration: ").color(NamedTextColor.RED),
                    Component.text(e.getMessage()).color(NamedTextColor.YELLOW)));
            return Optional.empty();
        } catch (Exception e) {
            invocation.source().sendMessage(Util.INTERNAL_ERROR);
            throw new RuntimeException(e);
        }
    }

    public TextComponent parseComponent(int startIndex, SimpleCommand.Invocation invocation) {
        if (invocation.arguments().length == startIndex) {
            return Component.text("Ban!").color(NamedTextColor.DARK_RED);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < invocation.arguments().length; i++) {
            builder.append(invocation.arguments()[i]).append(" ");
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(builder.toString());
    }

    public CompletableFuture<UUID> getPlayerUuid(int argumentIndex, ExecutorService service, PlayerResolver playerResolver, SimpleCommand.Invocation invocation) {
        String argument = invocation.arguments()[argumentIndex];
        if (argument.length() <= 16) {
            return playerResolver.getOrQueryPlayerUuid(argument, service);
        } else if (argument.length() <= 36) {
            try {
                return CompletableFuture.completedFuture(Util.parseUuid(argument));
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
