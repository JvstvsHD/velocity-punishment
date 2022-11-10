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

package de.jvstvshd.velocitypunishment.api.punishment;

import com.google.common.annotations.Beta;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Class holding the duration and expiration date of a punishment.
 *
 * @see #parse(String)
 */
public class PunishmentDuration implements Comparable<PunishmentDuration> {
    private final Duration duration;
    private final boolean permanent;
    private boolean relative = true;
    private LocalDateTime fixedExpiration = null;

    private PunishmentDuration(Duration duration, boolean permanent) {
        this.duration = duration;
        this.permanent = permanent;
    }

    /**
     * Makes this duration absolute, meaning the duration will not be added anymore. Instead, a fixed expiration date will be set.
     */
    public void absolute() {
        synchronized (this) {
            fixedExpiration = expiration();
            relative = false;
        }
    }

    /**
     * Converts the given {@link Timestamp} into a {@link PunishmentDuration}. The duration is absolute because it already was before.
     *
     * @param timestamp the timestamp which should be converted
     * @return the converted duration
     */
    public static PunishmentDuration fromTimestamp(Timestamp timestamp) {
        if (timestamp.equals(MAX))
            return permanent();
        var duration = new PunishmentDuration(Duration.between(Instant.now(), timestamp.toInstant()), false);
        duration.absolute();
        return duration;
    }

    /**
     * Parses a string to a {@link PunishmentDuration} using {@link Parser}. More information about how a duration will be parsed can be found <a href="https://github.com/JvstvsHD/VelocityPunishment#duration">here</a>.
     *
     * @param source the source string.
     * @return the parsed duration
     * @see Parser#parse()
     */
    public static PunishmentDuration parse(String source) {
        return new Parser(source).parse();
    }

    /**
     * Creates a new permanent (expiration date: 31.12.9999, 23:59:59) absolute punishment duration.
     *
     * @return a permanent duration
     */
    public static PunishmentDuration permanent() {
        var duration = new PunishmentDuration(Duration.between(LocalDateTime.now(), MAX.toLocalDateTime()), true);
        duration.absolute();
        return duration;
    }

    /**
     * @return a new punishment duration whose duration is 0 seconds.
     */
    public static PunishmentDuration zero() {
        return new PunishmentDuration(Duration.ofMillis(0), false);
    }

    /**
     * If this duration is relative ({@link #absolute()} was not called before), the created duration is now plus {@link #duration()}, otherwise the fixed expiration is returned.
     *
     * @return the expiration date of this punishment
     */
    public LocalDateTime expiration() {
        if (relative) {
            return LocalDateTime.now().plus(duration);
        }
        return fixedExpiration;
    }

    /**
     * @return the {@link #expiration()} date as {@link Timestamp} or {@link #MAX} if it is permanent
     */
    public Timestamp timestampExpiration() {
        if (permanent)
            return MAX;
        return Timestamp.valueOf(expiration());
    }

    public static final Timestamp MAX = Timestamp.valueOf(LocalDateTime.of(9999, 12, 31, 23, 59, 59));

    @Override
    public String toString() {
        return "PunishmentDuration{" +
                "duration=" + duration +
                ", permanent=" + permanent +
                ", expiration=" + expiration() +
                '}';
    }

    /**
     * @return the duration set in the constructor
     */
    public Duration duration() {
        return duration;
    }

    /**
     * @return whether this duration is permanent or not
     */
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
        int result = duration.hashCode();
        result = 31 * result + (permanent ? 1 : 0);
        result = 31 * result + (relative ? 1 : 0);
        result = 31 * result + fixedExpiration.hashCode();
        return result;
    }

    @Override
    public int compareTo(@NotNull PunishmentDuration o) {
        return expiration().compareTo(o.expiration());
    }

    /**
     * @return the end of this punishment as string
     */
    public String getEnd() {
        return expiration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    /**
     * @return the remaining duration represented as string
     */
    public String getRemainingDuration() {
        return representDuration(Duration.between(LocalDateTime.now(), expiration()));
    }

    /**
     * not yet supported.
     *
     * @return the initial duration
     */
    @Beta
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
