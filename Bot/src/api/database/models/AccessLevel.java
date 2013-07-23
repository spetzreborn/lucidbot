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

package api.database.models;

import api.common.HasName;
import api.irc.entities.IRCChannel;
import api.irc.entities.IRCUser;
import api.tools.text.StringUtil;

import javax.annotation.Nullable;

/**
 * Describes a level of access
 */
public enum AccessLevel implements HasName {
    PUBLIC {
        @Override
        public boolean allows(final IRCUser user, final IRCChannel channel) {
            return true;
        }

        @Override
        public boolean allows(final BotUser user) {
            return true;
        }
    }, USER {
        @Override
        public boolean allows(final IRCUser user, final IRCChannel channel) {
            return (channel == null || channel.getType() != ChannelType.PUBLIC) && user.isAuthenticated();
        }

        @Override
        public boolean allows(final BotUser user) {
            return user != null;
        }
    }, ADMIN {
        @Override
        public boolean allows(final IRCUser user, final IRCChannel channel) {
            return user.isAdmin() || (channel != null && channel.getType() == ChannelType.ADMIN);
        }

        @Override
        public boolean allows(final BotUser user) {
            return user.isAdmin();
        }
    };
    // note that the order of them is important. later in the list means higher access

    @Override
    public String getName() {
        return StringUtil.prettifyEnumName(this);
    }

    public static AccessLevel fromName(final String name) {
        for (AccessLevel level : values()) {
            if (level.getName().equalsIgnoreCase(name)) return level;
        }
        throw new IllegalArgumentException("No such access level: " + name);
    }

    /**
     * Checks if the access level prevents or allows access to the specified user in the specified channel
     *
     * @param user    the user
     * @param channel the irc channel, or null if it's a pm
     * @return true if access is allowed for the user
     */
    public abstract boolean allows(final IRCUser user, @Nullable final IRCChannel channel);

    /**
     * Checks if the access level prevents or allows access to the specified user
     *
     * @param user the user
     * @return true if access is allowed for the user
     */
    public abstract boolean allows(final BotUser user);
}
