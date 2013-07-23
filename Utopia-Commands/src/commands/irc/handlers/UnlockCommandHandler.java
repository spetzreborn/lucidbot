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

package commands.irc.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.events.DelayedEventPoster;
import api.events.bot.UnlockEvent;
import api.irc.IRCEntityManager;
import api.irc.entities.IRCChannel;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

public class UnlockCommandHandler implements CommandHandler {
    private final IRCEntityManager entityManager;

    @Inject
    public UnlockCommandHandler(final IRCEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        Collection<IRCChannel> channels = new ArrayList<>();
        if (params.isEmpty()) {
            channels = entityManager.getChannels();
        } else {
            for (String chan : params.getParameters("channels")) {
                IRCChannel channel = entityManager.getChannel(chan);
                if (channel != null) channels.add(channel);
            }
        }

        delayedEventPoster.enqueue(new UnlockEvent(channels, context));
        return CommandResponse.resultResponse("channels", channels);
    }
}
