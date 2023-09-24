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

import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A punishment duration that is relative to the current time meaning its expiration date is never fixed.
 * To retrieve the expiration date, use {@link #expiration()} or use {@link #absolute()} to convert this instance into a absolut punishment duration.
 *
 * @since 1.0.1
 */
public class RelativePunishmentDuration implements PunishmentDuration {

    private final Duration duration;

    RelativePunishmentDuration(Duration duration) {
        this.duration = duration;
    }

    /**
     * A {@link PunishmentDuration} whose duration is 0ms.
     *
     * @return a non-duration {@link PunishmentDuration}
     */
    public static PunishmentDuration zero() {
        return new RelativePunishmentDuration(Duration.ofMillis(0));
    }

    /**
     * @return the duration set in the constructor
     */
    public Duration duration() {
        return duration;
    }

    @Override
    public boolean isPermanent() {
        return !expiration().isBefore(LocalDateTime.MAX);
    }

    @Override
    public AbsolutePunishmentDuration absolute() {
        return new AbsolutePunishmentDuration(expiration());
    }

    @Override
    public RelativePunishmentDuration relative() {
        return this;
    }

    @Override
    public LocalDateTime expiration() {
        return LocalDateTime.now().plus(duration);
    }

    @Override
    public Timestamp expirationAsTimestamp() {
        return Timestamp.valueOf(expiration());
    }

    @Override
    public String expirationAsString() {
        return absolute().expirationAsString();
    }

    @Override
    public String remainingDuration() {
        return representDuration(Duration.between(LocalDateTime.now(), expiration()));
    }

    @Override
    public String toString() {
        return "PunishmentDuration{" +
                "duration=" + duration +
                ", expiration=" + expiration() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RelativePunishmentDuration) obj;
        return Objects.equals(this.duration, that.duration);
    }

    @Override
    public int hashCode() {
        return duration.hashCode();
    }

    @Override
    public int compareTo(@NotNull PunishmentDuration other) {
        return duration.compareTo(other.relative().duration());
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
}