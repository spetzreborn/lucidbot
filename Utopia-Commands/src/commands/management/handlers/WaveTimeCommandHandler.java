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

package commands.management.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.EventDAO;
import database.daos.UserActivitiesDAO;
import database.models.Event;
import database.models.UserActivities;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.RecentActivitiesFinder;
import tools.user_activities.RecentActivityType;
import tools.user_activities.UnseenInfo;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class WaveTimeCommandHandler implements CommandHandler {
    private final EventDAO eventDAO;
    private final BotUserDAO userDAO;
    private final UserActivitiesDAO userActivitiesDAO;
    private final RecentActivitiesFinder recentActivitiesFinder;

    @Inject
    public WaveTimeCommandHandler(final BotUserDAO userDAO, final EventDAO eventDAO, final UserActivitiesDAO userActivitiesDAO,
                                  final RecentActivitiesFinder recentActivitiesFinder) {
        this.userDAO = userDAO;
        this.eventDAO = eventDAO;
        this.userActivitiesDAO = userActivitiesDAO;
        this.recentActivitiesFinder = recentActivitiesFinder;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Event wave = eventDAO.getWave();
            if (wave == null) return CommandResponse.errorResponse("No wave scheduled");

            String userNick = params.containsKey("nick") ? params.getParameter("nick") : context.getUser().getMainNick();
            String time = userDAO.getFormattedDateInUsersTimezone(userNick, wave.getEventTime());

            BotUser botUser = context.getBotUser();
            UserActivities userActivities = userActivitiesDAO.getUserActivities(botUser);
            userActivities.setLastWaveCheck(new Date());

            List<UnseenInfo> unseenInfoOfInterest = recentActivitiesFinder
                    .mapUnseenActivities(botUser, userActivities);
            return CommandResponse
                    .resultResponse("wave", wave, "time", time, "unseenInfoOfInterest", unseenInfoOfInterest, "thisActivityType",
                            RecentActivityType.WAVE);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
