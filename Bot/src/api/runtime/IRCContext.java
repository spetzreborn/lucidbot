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

package api.runtime;

import api.commands.Command;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.irc.BotIRCInstance;
import api.irc.communication.IRCMessageType;
import api.irc.entities.IRCChannel;
import api.irc.entities.IRCUser;
import com.google.inject.Provider;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The context of a specific IRC event
 */
@Getter
@ParametersAreNonnullByDefault
public final class IRCContext {
    @Getter(AccessLevel.NONE)
    private final Provider<BotUserDAO> userDAOProvider;

    /**
     * The IRC Channel. Null if it's a PM context
     */
    private final IRCChannel channel;
    /**
     * The BotUser instance of the user in this context
     */
    @Getter(lazy = true)
    private final BotUser botUser = fetchBotUser();
    /**
     * The user that did something that lead to the event
     */
    private final IRCUser user;
    /**
     * The command that was called. Null if whatever happened wasn't a command call
     */
    private final Command command;
    /**
     * How many times the user wrote the command prefix. For example, if the prefix is !, the user may have done !!calc instead of !calc.
     */
    private final int commandPrefixesUsed;
    /**
     * The actual input from the user, with the command name removed if it was a command call
     */
    private final String input;
    /**
     * The type of input
     */
    private final IRCMessageType inputType;
    /**
     * The Bot Instance that received the input
     */
    private final BotIRCInstance receiver;

    IRCContext(final Provider<BotUserDAO> userDAOProvider,
               @Nullable final IRCChannel channel,
               final IRCUser user,
               @Nullable final Command command,
               final int commandPrefixesUsed,
               final String input,
               final IRCMessageType inputType,
               final BotIRCInstance receiver) {
        this.userDAOProvider = checkNotNull(userDAOProvider);
        this.channel = channel;
        this.user = checkNotNull(user);
        this.command = command;
        this.commandPrefixesUsed = commandPrefixesUsed;
        this.input = input;
        this.inputType = checkNotNull(inputType);
        this.receiver = checkNotNull(receiver);
    }

    private BotUser fetchBotUser() {
        return user.getMainNick() == null ? null : userDAOProvider.get().getUser(user.getMainNick());
    }
}
