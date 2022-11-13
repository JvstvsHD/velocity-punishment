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

package de.jvstvshd.velocitypunishment.api.duration;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class holding the duration and expiration date of a punishment.
 *
 * @see #parse(String)
 */
public interface PunishmentDuration extends Comparable<PunishmentDuration> {

    /**
     * Parses a string to a {@link PunishmentDuration} using {@link Parser}. More information about how a duration will be parsed can be found <a href="https://github.com/JvstvsHD/VelocityPunishment#duration">here</a>.
     *
     * @param source the source string.
     * @return the parsed duration
     * @see Parser#parse()
     */
    static PunishmentDuration parse(String source) {
        return new Parser(source).parse();
    }

    /**
     * Creates a new permanent (expiration date: 31.12.9999, 23:59:59) absolute punishment duration.
     *
     * @return a permanent duration
     */
    static PunishmentDuration permanent() {
        return PermanentPunishmentDuration.PERMANENT;
    }

    /**
     * Creates a new absolute punishment duration with the given expiration date.
     *
     * @param ldt the expiration date
     * @return a new absolute punishment duration
     * @since 1.0.1
     */
    static PunishmentDuration from(LocalDateTime ldt) {
        return new AbsolutePunishmentDuration(ldt);
    }

    /**
     * Converts the given {@link Timestamp} into a {@link PunishmentDuration}. The duration is absolute because it already was before.
     *
     * @param timestamp the timestamp which should be converted
     * @return the converted duration
     */
    static PunishmentDuration fromTimestamp(Timestamp timestamp) {
        return from(timestamp.toLocalDateTime());
    }

    static PunishmentDuration fromMillis(long millis) {
        return fromDuration(Duration.ofMillis(millis));
    }

    static PunishmentDuration fromDuration(Duration duration) {
        var rpd = new RelativePunishmentDuration(duration);
        if (rpd.isPermanent())
            return PermanentPunishmentDuration.PERMANENT;
        return rpd;
    }

    /**
     * Whether this duration is permanent meaning the expiration should be 31.12.9999, 23:59:59.
     *
     * @return whether this duration is permanent
     * @see PermanentPunishmentDuration#PERMANENT
     * @see AbsolutePunishmentDuration#MAX
     * @see AbsolutePunishmentDuration#MAX_TIMESTAMP
     */
    boolean isPermanent();

    /**
     * Ensures this {@link PunishmentDuration} is absolute. If it is already absolute, this instance will be returned.
     * If it is relative, it will be converted to an absolute duration.
     *
     * @return an absolute duration
     * @see AbsolutePunishmentDuration
     */
    AbsolutePunishmentDuration absolute();

    /**
     * Ensures this {@link PunishmentDuration} is relative. If it is already relative, this instance will be returned.
     * If it is absolute, it will be converted to a relative duration.
     *
     * @return a relative duration
     * @see RelativePunishmentDuration
     */
    RelativePunishmentDuration relative();

    /**
     * The expiration date of this duration.
     *
     * @return the expiration date
     */
    LocalDateTime expiration();

    /**
     * The expiration date of this duration as {@link Timestamp}.
     *
     * @return the duration
     * @since 1.0.1
     */
    Timestamp expirationAsTimestamp();

    /**
     * Formats the expiration date of this duration in the following format as {@link String}, if not otherwise specified: {@code dd/MM/yyyy HH:mm:ss}.
     *
     * @return the end of this punishment as string
     * @since 1.0.1
     */
    String expirationAsString();

    /**
     * Formats the remaining duration in the same format as parsed by {@link Parser}, unless specified otherwise.
     *
     * @return the remaining duration
     */
    String remainingDuration();

    /**
     * The initial duration (before any changes to the punishment this duration was created for).
     *
     * @return the initial duration
     * @throws UnsupportedOperationException default; if this method is not implemented by its underlying implementation
     * @since 1.0.1
     */
    default PunishmentDuration initialDuration() {
        throw new UnsupportedOperationException("Initial durations are not stored.");
    }

    /**
     * A class for parsing player inputs into a valid {@link PunishmentDuration}.
     * Allowed format: <b>{@code \d+[smhdSMHD]}</b> (e.g. 1m (one minute), 2d (two days), 1d6h (one day and six hours) or 1h30m (one hour and thirty minutes))
     */
    class Parser {
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

        public Parser(String source) {
            this.source = source;
            this.rawDuration = new HashMap<>();
        }

        /**
         * Converts a string into multiple {@link TimeUnit}s and their corresponding values. For retrieving the values, use {@link #parse()}.
         *
         * @see #parse()
         */
        public void convert() {
            String[] numbers = source.split("[sSmMhHdD]");
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
                TimeUnit timeUnit = characterMapping.get(unit);
                if (timeUnit == null)
                    throw new IllegalArgumentException("Unknown time unit for character '" + unit + "'");
                converted.put(timeUnit, numericValue);
                index++;
            }
            rawDuration.clear();
            rawDuration.putAll(converted);
        }

        /**
         * Parses the source string into a {@link PunishmentDuration}. This usually is a {@link RelativePunishmentDuration}
         * since the player input contains only relative information and no absolute expiration date.
         *
         * @return the parsed duration
         */
        public PunishmentDuration parse() {
            convert();
            if (rawDuration.isEmpty()) {
                throw new IllegalArgumentException("Converted map is empty.");
            }
            return fromMillis(durationToMillis());
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
