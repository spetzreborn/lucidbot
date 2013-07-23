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
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.time.TimeUtil;
import database.daos.EventDAO;
import database.daos.UserActivitiesDAO;
import database.models.AttendanceStatus;
import database.models.AttendanceType;
import database.models.Event;
import database.models.UserActivities;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.RecentActivitiesFinder;
import tools.time.UtopiaTimeFactory;
import tools.user_activities.RecentActivityType;
import tools.user_activities.UnseenInfo;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class WaveCommandHandler implements CommandHandler {
    private final EventDAO eventDAO;
    private final UserActivitiesDAO userActivitiesDAO;
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final RecentActivitiesFinder recentActivitiesFinder;

    @Inject
    public WaveCommandHandler(final EventDAO eventDAO, final UserActivitiesDAO userActivitiesDAO, final UtopiaTimeFactory utopiaTimeFactory,
                              final RecentActivitiesFinder recentActivitiesFinder) {
        this.eventDAO = eventDAO;
        this.userActivitiesDAO = userActivitiesDAO;
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.recentActivitiesFinder = recentActivitiesFinder;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Event wave = eventDAO.getWave();
            if (wave == null) return CommandResponse.errorResponse("No wave scheduled");

            BotUser botUser = context.getBotUser();
            UserActivities userActivities = userActivitiesDAO.getUserActivities(botUser);
            userActivities.setLastWaveCheck(new Date());

            if (params.containsKey("attendance")) {
                String attendance = params.getParameter("attendance");
                AttendanceType type = AttendanceType.fromString(attendance);
                String details = type.extractDetails(attendance);
                List<AttendanceStatus> attendanceInformation = wave.getAttendanceInformation();
                boolean overwrote = false;
                for (AttendanceStatus status : attendanceInformation) {
                    if (status.getUser().equals(botUser)) {
                        status.setType(type);
                        status.setDetails(details);
                        overwrote = true;
                    }
                }
                if (!overwrote) attendanceInformation.add(new AttendanceStatus(botUser, wave, type, details));
                return CommandResponse.resultResponse("attendance", true);
            } else {
                List<UnseenInfo> unseenInfoOfInterest = recentActivitiesFinder
                        .mapUnseenActivities(botUser, userActivities);
                return CommandResponse.resultResponse("wave", wave, "utodate",
                        utopiaTimeFactory.newUtopiaTime(wave.getEventTime().getTime()).formattedUT(),
                        "timeLeft", TimeUtil.compareDateToCurrent(wave.getEventTime()),
                        "unseenInfoOfInterest", unseenInfoOfInterest, "thisActivityType",
                        RecentActivityType.WAVE);
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
