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
import api.events.irc.IRCMessageEvent;
import api.irc.IRCEntityManager;
import api.irc.entities.IRCChannel;
import api.irc.entities.IRCUser;
import com.google.inject.Provider;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory for creating contexts
 */
@ParametersAreNonnullByDefault
public final class ContextFactory {
    private final IRCEntityManager entityManager;
    private final Provider<BotUserDAO> userDAOProvider;

    @Inject
    public ContextFactory(final IRCEntityManager entityManager, final Provider<BotUserDAO> userDAOProvider) {
        this.entityManager = checkNotNull(entityManager);
        this.userDAOProvider = checkNotNull(userDAOProvider);
    }

    /**
     * Creates and returns a new IRCContext based on the specified event and command
     *
     * @param event   the event to create the context for
     * @param command the command
     * @return new IRCContext
     */
    public IRCContext newIRCContext(final IRCMessageEvent event, final String parsedInput, @Nullable final Command command, final int commandPrefixesUsed) {
        IRCChannel channel = event.getChannel() == null ? null : entityManager.getChannel(event.getChannel());
        IRCUser user = entityManager.getUser(event.getSender());
        if (user == null) return null;
        String input = command == null ? parsedInput : getInputWithoutCommand(checkNotNull(parsedInput));
        return new IRCContext(userDAOProvider, channel, user, command, commandPrefixesUsed, input, event.getMessageType(), event.getReceiver());
    }

    /**
     * @param raw the raw irc input
     * @return the input with the command-name removed, if one exists
     */
    private static String getInputWithoutCommand(final String raw) {
        int firstSpace = raw.indexOf(' ');
        return firstSpace == -1 ? "" : raw.substring(firstSpace + 1);
    }
}
