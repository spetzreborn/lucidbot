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

package commands.user.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.time.DateFactory;
import api.tools.time.DateUtil;
import database.daos.NotificationDAO;
import database.models.Alarm;
import database.models.Notification;
import database.models.NotificationMethod;
import database.models.NotificationType;
import listeners.AlarmManager;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

public class SetAlarmCommandHandler implements CommandHandler {
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final AlarmManager alarmManager;
    private final NotificationDAO notificationDAO;

    @Inject
    public SetAlarmCommandHandler(final UtopiaTimeFactory utopiaTimeFactory, final AlarmManager alarmManager,
                                  final NotificationDAO notificationDAO) {
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.alarmManager = alarmManager;
        this.notificationDAO = notificationDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Date date;
            BotUser botUser = context.getBotUser();
            if (params.containsKey("time")) {
                DateFormat dateFormat = DateFactory.getISOWithoutSecondsDateTimeFormat();
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT" + botUser.getTimeZone()));
                Date tempDate = dateFormat.parse(params.getParameter("time"));
                date = new Date(tempDate.getTime() + DateUtil.hoursToMillis(botUser.getDst()));
            } else {
                date = utopiaTimeFactory.newUtopiaTime(params.getParameter("utoTime")).getDate();
            }
            Alarm alarm = alarmManager.saveAlarm(new Alarm(date, params.getParameter("message"), botUser));
            ensureNotifications(botUser);
            return CommandResponse.resultResponse("alarm", alarm);
        } catch (ParseException | DBException e) {
            throw new CommandHandlingException(e);
        } catch (IllegalArgumentException e) {
            return CommandResponse.errorResponse(e.getMessage());
        }
    }

    private void ensureNotifications(final BotUser user) {
        Collection<Notification> notifications = notificationDAO.getNotifications(user, NotificationType.ALARM);
        if (notifications.isEmpty()) {
            notificationDAO.save(new Notification(user, NotificationType.ALARM, NotificationMethod.PM));
        }
    }
}
