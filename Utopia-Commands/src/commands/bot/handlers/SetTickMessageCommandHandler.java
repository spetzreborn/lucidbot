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

package commands.bot.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.ChannelDAO;
import api.database.models.Channel;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.TickChannelMessageDAO;
import database.models.TickChannelMessage;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;

public class SetTickMessageCommandHandler implements CommandHandler {
    private final ChannelDAO channelDAO;
    private final TickChannelMessageDAO tickChannelMessageDAO;

    @Inject
    public SetTickMessageCommandHandler(final ChannelDAO channelDAO, final TickChannelMessageDAO tickChannelMessageDAO) {
        this.channelDAO = channelDAO;
        this.tickChannelMessageDAO = tickChannelMessageDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        if (!params.containsKey("channel") && context.getChannel() == null)
            return CommandResponse.errorResponse("You have the specify the channel when using this command in pm");

        try {
            String channelName = params.containsKey("channel") ? params.getParameter("channel") : context.getChannel().getName();
            return handleHourlyMessage(params.getParameter("message"), tickChannelMessageDAO, channelDAO, channelName);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static CommandResponse handleHourlyMessage(final String message, final TickChannelMessageDAO tickChannelMessageDAO,
                                                       final ChannelDAO channelDAO, final String channelName) {
        Channel channel = channelDAO.getChannel(channelName);
        if (channel == null) return CommandResponse.errorResponse("No such channel exists in the database");

        TickChannelMessage channelMessage = tickChannelMessageDAO.getTickChannelMessage(channel);
        if (channelMessage == null) channelMessage = new TickChannelMessage(channel, message);
        else channelMessage.setMessage(message);
        tickChannelMessageDAO.save(channelMessage);

        return CommandResponse.resultResponse("channelName", channel.getName(), "message", message);
    }
}
