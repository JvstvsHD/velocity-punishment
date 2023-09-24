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
import java.time.format.DateTimeFormatter;

/**
 * A punishment duration that has an absolute expiration date.
 *
 * @since 1.0.1
 */
public class AbsolutePunishmentDuration implements PunishmentDuration {

    /**
     * The maximum supported {@code LocalDateTime} value.
     */
    public static final LocalDateTime MAX = LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    public static final Timestamp MAX_TIMESTAMP = Timestamp.valueOf(MAX);
    private final LocalDateTime expiration;

    AbsolutePunishmentDuration(LocalDateTime expiration) {
        this.expiration = expiration;
    }

    public static PunishmentDuration from(LocalDateTime ldt) {
        if (!ldt.isBefore(MAX))
            return PermanentPunishmentDuration.PERMANENT;
        return new AbsolutePunishmentDuration(ldt);
    }

    @Override
    public boolean isPermanent() {
        return !expiration.isBefore(MAX);
    }

    @Override
    public AbsolutePunishmentDuration absolute() {
        return this;
    }

    @Override
    public RelativePunishmentDuration relative() {
        return new RelativePunishmentDuration(Duration.between(LocalDateTime.now(), expiration));
    }

    @Override
    public LocalDateTime expiration() {
        return expiration;
    }

    @Override
    public Timestamp expirationAsTimestamp() {
        return Timestamp.valueOf(expiration);
    }

    @Override
    public String expirationAsString() {
        return expiration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    @Override
    public String remainingDuration() {
        return relative().remainingDuration();
    }

    @Override
    public int compareTo(@NotNull PunishmentDuration other) {
        return expiration.compareTo(other.expiration());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbsolutePunishmentDuration that = (AbsolutePunishmentDuration) o;

        return expiration.equals(that.expiration);
    }

    @Override
    public int hashCode() {
        return expiration.hashCode();
    }

    @Override
    public String toString() {
        return "AbsolutePunishmentDuration{" +
                "expiration=" + expiration +
                '}';
    }
}
