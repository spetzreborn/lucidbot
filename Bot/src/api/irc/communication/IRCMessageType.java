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

package api.irc.communication;

import api.common.HasName;
import internal.irc.communication.IrcCommands;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Types of IRC Messages
 */
@ParametersAreNonnullByDefault
public enum IRCMessageType implements HasName {
    ACTION("me") {
        @Override
        public String formatCommand(final String target, final String message) {
            return IrcCommands.PrivMsgCommand.format(checkNotNull(target), "\u0001ACTION " + checkNotNull(message) + '\u0001');
        }
    }, CTCP("ctcp") {
        @Override
        public String formatCommand(final String target, final String message) {
            return IrcCommands.NoticeCommand.format(checkNotNull(target), '\u0001' + checkNotNull(message) + '\u0001');
        }
    }, MESSAGE("msg") {
        @Override
        public String formatCommand(final String target, final String message) {
            return IrcCommands.PrivMsgCommand.format(checkNotNull(target), checkNotNull(message));
        }
    }, NOTICE("notice") {
        @Override
        public String formatCommand(final String target, final String message) {
            return IrcCommands.NoticeCommand.format(checkNotNull(target), checkNotNull(message));
        }
    }, PRIVATE_MESSAGE("pm") {
        @Override
        public String formatCommand(final String target, final String message) {
            return IrcCommands.PrivMsgCommand.format(checkNotNull(target), checkNotNull(message));
        }
    };

    private final String shortName;

    IRCMessageType(final String shortName) {
        this.shortName = shortName;
    }

    public static Collection<String> nameValues() {
        IRCMessageType[] values = values();
        Collection<String> names = new ArrayList<>(values.length);
        for (IRCMessageType value : values) {
            names.add(value.shortName);
        }
        return names;
    }

    @Override
    public String getName() {
        return toString();
    }

    /**
     * @param nameOrShortName the name or short name of the message type sought (not case sensitive)
     * @return the IRCMessageType that has the name or short name specified, or null if no match is found
     */
    public static IRCMessageType fromName(final String nameOrShortName) {
        for (IRCMessageType type : values()) {
            if (type.name().equalsIgnoreCase(nameOrShortName) || type.shortName.equalsIgnoreCase(nameOrShortName)) return type;
        }
        throw new IllegalArgumentException("Unknown message type: " + nameOrShortName);
    }

    /**
     * @return a String with the names of all the message types
     */
    public static String allNames() {
        IRCMessageType[] values = values();
        List<String> names = new ArrayList<>(values.length * 2);
        for (IRCMessageType value : values) {
            names.add(value.getName());
            names.add(value.shortName);
        }
        return names.toString();
    }

    /**
     * Formats a message into a command that can actually be sent to the IRC server
     *
     * @param target  the target/recipient
     * @param message the message
     * @return a String with the message formatted appropriately for sending to the IRC server
     */
    public abstract String formatCommand(final String target, final String message);
}
