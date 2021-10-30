package de.jvstvshd.velocitypunishment.util;

import com.google.common.collect.Sets;
import com.velocitypowered.api.proxy.Player;
import de.jvstvshd.velocitypunishment.punishment.Punishment;
import de.jvstvshd.velocitypunishment.punishment.TemporalPunishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Util {

    public static final TextComponent INTERNAL_ERROR =
            Component.text("An internal error occurred. Please contact the network administrator.")
                    .color(NamedTextColor.DARK_RED);

    public static List<String> getPlayerNames(Collection<Player> players) {
        return players.stream().collect(new Collector<Player, List<Player>, List<String>>() {
            @Override
            public Supplier<List<Player>> supplier() {
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<Player>, Player> accumulator() {
                return List::add;
            }

            @Override
            public BinaryOperator<List<Player>> combiner() {
                return (players, players2) -> {
                    players.addAll(players2);
                    return players;
                };
            }

            @Override
            public Function<List<Player>, List<String>> finisher() {
                return players -> {
                    List<String> strings = new ArrayList<>();
                    for (Player player : players) {
                        strings.add(player.getUsername());
                    }
                    return strings;
                };
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Sets.immutableEnumSet(Characteristics.UNORDERED);
            }
        });
    }

    public static UUID parse(String uuidString) {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return UUID.fromString(uuidString.replaceAll(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"));
        }
    }

    public static TextComponent copyComponent(String text) {
        return Component.text(text).clickEvent(ClickEvent.copyToClipboard(text))
                .hoverEvent((HoverEventSource<Component>) op -> HoverEvent.showText(Component.text("Copy to clipboard").color(NamedTextColor.GREEN)));
    }

    public static <T> CompletableFuture<T> executeAsync(Callable<T> task, ExecutorService service) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        service.execute(() -> {
            try {
                cf.complete(task.call());
            } catch (Exception e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    public static <T extends TemporalPunishment> T getLongestPunishment(List<T> list) {
        List<T> sorted = sortPunishments(list);
        return sorted.get(sorted.size() - 1);
    }

    public static <T extends TemporalPunishment> List<T> sortPunishments(List<T> list) {
        return list.stream().sorted(Comparator.comparing(TemporalPunishment::getDuration)).collect(Collectors.toList());
    }

    public static <T extends Punishment> List<T> convert(List<? super T> list) {
        List<T> out = new ArrayList<>();
        for (Object o : list) {
            out.add((T) o);
        }
        return out;
    }

    public static String trimUuid(UUID origin) {
        return origin.toString().toLowerCase().replace("-", "");
    }
}
