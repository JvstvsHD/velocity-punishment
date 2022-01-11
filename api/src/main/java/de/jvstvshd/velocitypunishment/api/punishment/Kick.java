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

import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Super interface for all kick implementation.<br>
 * Unsupported operations:
 * <ul>
 *     <li>{@link #cancel()}</li>
 *     <li>{@link #change(PunishmentDuration, Component)}</li>
 * </ul>
 *
 * @see com.velocitypowered.api.proxy.Player#disconnect(Component)
 */
public interface Kick extends Punishment {

    @Override
    default boolean isOngoing() {
        return false;
    }

    @Override
    default CompletableFuture<Punishment> cancel() {
        throw new UnsupportedOperationException("Cannot annul kick since a kick lasts only one moment");
    }

    @Override
    default CompletableFuture<Punishment> change(PunishmentDuration newDuration, Component newReason) {
        throw new UnsupportedOperationException("Cannot change a kick lasts only one moment");
    }
}
