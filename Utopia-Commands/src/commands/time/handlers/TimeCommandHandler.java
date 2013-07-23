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

package commands.time.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.time.TimeUtil;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.time.UtopiaTime;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;

public class TimeCommandHandler implements CommandHandler {
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final BotUserDAO userDAO;

    @Inject
    public TimeCommandHandler(final BotUserDAO userDAO, final UtopiaTimeFactory utopiaTimeFactory) {
        this.userDAO = userDAO;
        this.utopiaTimeFactory = utopiaTimeFactory;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        if (params.isEmpty()) {
            UtopiaTime time = utopiaTimeFactory.newUtopiaTime(System.currentTimeMillis());
            return CommandResponse.resultResponse("time", time.formattedUT(), "isUtoTime", true);
        }
        try {
            String userNick = params.containsKey("nick") ? params.getParameter("nick") : context.getUser().getMainNick();
            Date specifiedDate = params.containsKey("time") ? utopiaTimeFactory.newUtopiaTime(params.getParameter("time")).getDate() : null;
            String difference = specifiedDate == null ? null : TimeUtil.compareDateToCurrent(specifiedDate) +
                    (specifiedDate.compareTo(new Date()) < 0 ? " ago" : " from now");
            String time = userDAO.getFormattedDateInUsersTimezone(userNick, specifiedDate == null ? new Date() : specifiedDate);
            if (time == null) return CommandResponse.errorResponse("User not found");
            return CommandResponse.resultResponse("time", time, "isUtoTime", false, "user", userNick, "timeAgo", difference);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
