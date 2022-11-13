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

package de.jvstvshd.velocitypunishment.common.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.UUID;

public class MuteData {

    public static final String MUTE_DATA_CHANNEL_IDENTIFIER = "velocitypunishment:mutedata";
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    public static final int ADD = 0;
    public static final int REMOVE = 1;
    public static final int UPDATE = 2;
    public static final int UNKNOWN = -1;

    private final UUID uuid;
    private final String reason;
    private final LocalDateTime expiration;
    private final int type;
    private final UUID punishmentId;

    public MuteData(@JsonProperty("uuid") UUID uuid, @JsonProperty("reason") String reason,
                    @JsonProperty("expiration") LocalDateTime expiration, @JsonProperty("type") int type, @JsonProperty("punishment_id") UUID punishmentId) {
        this.uuid = uuid;
        this.reason = reason;
        this.expiration = expiration;
        this.type = type;
        this.punishmentId = punishmentId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getExpiration() {
        return expiration;
    }

    public int getType() {
        return type;
    }

    @JsonIgnore
    public boolean isAdd() {
        return type == ADD;
    }

    @JsonIgnore
    public boolean isRemove() {
        return type == REMOVE;
    }

    @JsonIgnore
    public boolean isUpdate() {
        return type == UPDATE;
    }

    public UUID getPunishmentId() {
        return punishmentId;
    }
}
