package de.jvstvshd.velocitypunishment.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.PunishmentHelper;
import de.jvstvshd.velocitypunishment.punishment.PunishmentManager;
import de.jvstvshd.velocitypunishment.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class PunishmentCommand implements SimpleCommand {

    private final ExecutorService service;
    private final PunishmentManager punishmentManager;

    public PunishmentCommand(ExecutorService service, PunishmentManager punishmentManager) {
        this.service = service;
        this.punishmentManager = punishmentManager;
    }

    private final static List<String> options = ImmutableList.of("annul", "remove", "info", "change");

    @Override
    public void execute(Invocation invocation) {
        String[] arguments = invocation.arguments();
        CommandSource source = invocation.source();
        if (arguments.length < 2) {
            source.sendMessage(Component.text("invalid usage!").color(NamedTextColor.DARK_RED));
            return;
        }
        if (arguments[0].equalsIgnoreCase("playerinfo")) {
            PunishmentHelper helper = new PunishmentHelper();
            UUID playerUuid = helper.getPlayerUuid(1, service, punishmentManager, invocation);
            if (playerUuid == null) {
                invocation.source().sendMessage(Component.text("This player is not banned at the moment.").color(NamedTextColor.RED));
                return;
            }
            service.execute(() -> {
                List<Punishment> punishments;
                try {
                    punishments = punishmentManager.getPunishments(playerUuid, service).get(10, TimeUnit.SECONDS);
                } catch (InterruptedException | TimeoutException | ExecutionException e) {
                    e.printStackTrace();
                    source.sendMessage(Util.INTERNAL_ERROR);
                    return;
                }
                source.sendMessage(Component.text("The player has " + punishments.size() + " punishments.").color(NamedTextColor.AQUA));
                for (Punishment punishment : punishments) {
                    Component component = helper.buildPunishmentData(punishment)
                            .clickEvent(ClickEvent.copyToClipboard(punishment.getPunishmentUuid().toString().toLowerCase(Locale.ROOT)))
                            .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(Component.text("Click to copy punishment id")
                                    .color(NamedTextColor.GREEN)));
                    source.sendMessage(component);
                }
            });
            return;
        }
        UUID uuid;
        try {
            uuid = Util.parse(arguments[0]);
        } catch (IllegalArgumentException e) {
            source.sendMessage(Component.text().append(Component.text("Could not parse string '").color(NamedTextColor.RED),
                    Component.text(arguments[0]).color(NamedTextColor.YELLOW),
                    Component.text("' as uuid.").color(NamedTextColor.RED)));
            return;
        }
        String option = arguments[1].toLowerCase(Locale.ROOT);
        if (!options.contains(option)) {
            source.sendMessage(Component.text()
                    .append(Component.text("Unknown option: ").color(NamedTextColor.RED),
                            Component.text(option).color(NamedTextColor.YELLOW)));
            return;
        }
        service.execute(() -> {
            Punishment punishment;
            try {
                Optional<? extends Punishment> optionalPunishment = punishmentManager.getPunishment(uuid, service).get(5, TimeUnit.SECONDS);
                if (optionalPunishment.isEmpty()) {
                    source.sendMessage(Component.text().append(Component.text("Could not find a punishment for id '").color(NamedTextColor.RED),
                            Component.text(uuid.toString().toLowerCase(Locale.ROOT)).color(NamedTextColor.YELLOW),
                            Component.text("'.").color(NamedTextColor.RED)));
                    return;
                }
                punishment = optionalPunishment.get();
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                e.printStackTrace();
                source.sendMessage(Util.INTERNAL_ERROR);
                return;
            }
            switch (option) {
                case "annul":
                case "remove":
                    try {
                        if (punishment.annul().get(5, TimeUnit.SECONDS)) {
                            source.sendMessage(Component.text("The punishment was successfully annulled.").color(NamedTextColor.GREEN));
                        } else {
                            source.sendMessage(Component.text().append(Component.text("Could not annul punishment for id '").color(NamedTextColor.RED),
                                    Component.text(uuid.toString().toLowerCase()).color(NamedTextColor.YELLOW),
                                    Component.text("'.").color(NamedTextColor.RED)));
                            return;
                        }
                    } catch (InterruptedException | TimeoutException | ExecutionException e) {
                        e.printStackTrace();
                        source.sendMessage(Util.INTERNAL_ERROR);
                        return;
                    }
                    break;
                case "info":
                    source.sendMessage(new PunishmentHelper().buildPunishmentData(punishment));
                    break;
                case "change":
                    source.sendMessage(Component.text("Soon™️"));
                    break;
            }
        });

    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] arguments = invocation.arguments();
        if (arguments.length == 0) {
            return List.of("playerinfo");
        } else if (arguments.length == 1 && arguments[0].toLowerCase(Locale.ROOT).startsWith("p")) {
            return List.of("playerinfo");
        } else if (arguments.length == 2) {
            return options;
        } else if (arguments.length == 3) {
            return options.stream().filter(s -> s.toLowerCase().startsWith(arguments[2])).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return SimpleCommand.super.hasPermission(invocation);
    }
}
