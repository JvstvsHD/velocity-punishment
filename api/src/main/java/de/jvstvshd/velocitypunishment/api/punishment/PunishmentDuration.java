package de.jvstvshd.velocitypunishment.api.punishment;

import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PunishmentDuration implements Comparable<PunishmentDuration> {
    private final Duration duration;
    private final boolean permanent;
    private boolean relative = true;
    private LocalDateTime fixedExpiration = null;

    private PunishmentDuration(Duration duration, boolean permanent) {
        this.duration = duration;
        this.permanent = permanent;
    }

    public void absolute() {
        fixedExpiration = expiration();
        relative = false;
    }

    public static PunishmentDuration fromTimestamp(Timestamp timestamp) {
        if (timestamp.equals(MAX))
            return permanent();
        var duration = new PunishmentDuration(Duration.between(Instant.now(), timestamp.toInstant()), false);
        duration.absolute();
        return duration;
    }

    public static PunishmentDuration parse(String source) {
        return new Parser(source).parse();
    }

    public static PunishmentDuration permanent() {
        return new PunishmentDuration(Duration.between(LocalDateTime.now(), MAX.toLocalDateTime()), true);
    }

    public static PunishmentDuration zero() {
        return new PunishmentDuration(Duration.ofMillis(0), false);
    }

    public LocalDateTime expiration() {
        if (relative) {
            return LocalDateTime.now().plus(duration);
        }
        return fixedExpiration;
    }

    public Timestamp timestampExpiration() {
        if (permanent)
            return MAX;
        return Timestamp.from(Instant.from(OffsetDateTime.now().plus(duration)));
    }

    public static Timestamp MAX = Timestamp.valueOf(LocalDateTime.of(9999, 12, 31, 23, 59, 59));

    @Override
    public String toString() {
        return "PunishmentDuration{" +
                "duration=" + duration +
                ", permanent=" + permanent +
                ", expiration=" + expiration() +
                '}';
    }

    public Duration duration() {
        return duration;
    }

    public boolean isPermanent() {
        return permanent;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PunishmentDuration) obj;
        return Objects.equals(this.duration, that.duration) &&
                this.permanent == that.permanent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, permanent);
    }

    @Override
    public int compareTo(@NotNull PunishmentDuration o) {
        return expiration().compareTo(o.expiration());
    }

    public String getEnd() {
        return expiration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public String getRemainingDuration() {
        return representDuration(Duration.between(LocalDateTime.now(), expiration()));
    }

    public String getInitialDuration() {
        return representDuration(duration);
    }

    private String representDuration(Duration duration) {
        String days = duration.toDaysPart() > 0 ? duration.toDaysPart() + "d" : "";
        String hours = normalizeTimeUnit(duration.toHoursPart()) + "h";
        String minutes = normalizeTimeUnit(duration.toMinutesPart()) + "m";
        String seconds = normalizeTimeUnit(duration.toSecondsPart()) + "s";
        return formatRemainingDuration(days, hours, minutes, seconds);
    }

    private String formatRemainingDuration(String days, String hours, String minutes, String seconds) {
        if (hours.equalsIgnoreCase("00h") && days.isBlank()) {
            hours = "";
        }
        if (days.isBlank() && minutes.equalsIgnoreCase("00m")) {
            minutes = "";
        }
        return days + hours + minutes + seconds;
    }

    private String normalizeTimeUnit(long value) {
        String s = String.valueOf(value);
        if (value == 0)
            return "00";
        if (s.length() < 2)
            return "0" + s;
        return s;
    }

    public static class Parser {
        private final static Map<Character, TimeUnit> characterMapping = new HashMap<>() {
            {
                put('s', TimeUnit.SECONDS);
                put('m', TimeUnit.MINUTES);
                put('h', TimeUnit.HOURS);
                put('d', TimeUnit.DAYS);
            }
        };
        private final String source;
        private final Map<TimeUnit, Long> rawDuration;
        private final boolean permanent;

        public Parser(String source) {
            this(source, false);
        }

        public Parser(String source, boolean permanent) {
            this.source = source;
            this.rawDuration = new HashMap<>();
            this.permanent = permanent;
        }

        public void convert() {
            String[] numbers = source.split("[sSmMhHdDyY]");
            Map<TimeUnit, Long> converted = new HashMap<>();
            int index = 0;
            for (String number : numbers) {
                index += number.length();
                final long numericValue = Long.parseLong(number);
                if (numericValue < 0)
                    throw new IllegalArgumentException("Illegal numeric value: " + numericValue);
                final char unit;
                try {
                    unit = Character.toLowerCase(source.charAt(index));
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalArgumentException("Number is not followed by unit marking character.");
                }
                TimeUnit chronoUnit = characterMapping.get(unit);
                if (chronoUnit == null)
                    throw new IllegalArgumentException("Unknown time unit for character '" + unit + "'");
                converted.put(chronoUnit, numericValue);
                index++;
            }
            rawDuration.clear();
            rawDuration.putAll(converted);
        }

        public PunishmentDuration parse() {
            convert();
            if (rawDuration.isEmpty()) {
                throw new IllegalArgumentException("Converted map is empty.");
            }
            return new PunishmentDuration(Duration.ofMillis(durationToMillis()), permanent);
        }

        private long convertToMillis(TimeUnit unit, long value) {
            return unit.toMillis(value);
        }

        private long durationToMillis() {
            long total = 0;
            for (Map.Entry<TimeUnit, Long> entry : rawDuration.entrySet()) {
                total += convertToMillis(entry.getKey(), entry.getValue());
            }
            return total;
        }
    }
}
