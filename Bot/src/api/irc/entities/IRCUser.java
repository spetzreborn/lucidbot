/*
 * Copyright (c) 2012, Fredrik Yttergren
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name LucidBot nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Fredrik Yttergren BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package api.irc.entities;

import api.common.HasName;
import api.irc.communication.IRCMessageType;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a user on IRC, and contains information about it
 */
@ParametersAreNonnullByDefault
public final class IRCUser implements IRCEntity, HasName {
    private static final AtomicLong counter = new AtomicLong();

    private final AtomicReference<String> currentNick = new AtomicReference<>();
    private final AtomicReference<String> mainNick = new AtomicReference<>();
    private final long uniqueId;

    /**
     * Whether this user is an admin or not
     */
    @Setter
    @Getter
    private volatile boolean isAdmin;

    public IRCUser(final String nick) {
        currentNick.set(checkNotNull(nick));
        mainNick.set(null);
        uniqueId = counter.incrementAndGet();
    }

    /**
     * Authenticates the user, settings the specified info. Does nothing if the user is already authenticated
     *
     * @param mainNick the main nick of the user
     * @param isAdmin  true if the user is an admin, false if not
     */
    public boolean authenticate(final String mainNick, final boolean isAdmin) {
        if (isAuthenticated()) return false;
        this.mainNick.set(checkNotNull(mainNick));
        this.isAdmin = isAdmin;
        return true;
    }

    /**
     * Drops the user's authentication
     */
    public void deauthenticate() {
        mainNick.set(null);
        isAdmin = false;
    }

    /**
     * Sets the current nick of the user
     *
     * @param newNick the nick to set
     */
    public void setCurrentNick(final String newNick) {
        currentNick.set(checkNotNull(newNick));
    }

    /**
     * @return the user's current nick
     */
    public String getCurrentNick() {
        return currentNick.get();
    }

    /**
     * @return the user's main nick. null if the user isn't authenticated
     */
    public String getMainNick() {
        return mainNick.get();
    }

    /**
     * Sets the user's main nick
     *
     * @param mainNick the main nick to set
     */
    public void setMainNick(final String mainNick) {
        this.mainNick.set(checkNotNull(mainNick));
    }

    @Override
    public String getName() {
        return currentNick.get();
    }

    /**
     * @return true if the user is authenticated
     */
    public boolean isAuthenticated() {
        return mainNick.get() != null;
    }

    @Override
    public boolean requiresOutputBlocking(final IRCMessageType type) {
        return type == IRCMessageType.NOTICE;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this || obj instanceof IRCUser && uniqueId == ((IRCUser) obj).uniqueId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uniqueId);
    }
}
