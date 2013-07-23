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
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.NotificationDAO;
import database.daos.WaitDAO;
import database.models.Notification;
import database.models.NotificationMethod;
import database.models.NotificationType;
import database.models.Wait;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;

public class WaitCommandHandler implements CommandHandler {
    private final BotUserDAO userDAO;
    private final WaitDAO waitDAO;
    private final NotificationDAO notificationDAO;

    @Inject
    public WaitCommandHandler(final NotificationDAO notificationDAO, final WaitDAO waitDAO, final BotUserDAO userDAO) {
        this.notificationDAO = notificationDAO;
        this.waitDAO = waitDAO;
        this.userDAO = userDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            BotUser user = context.getBotUser();
            BotUser waitFor = userDAO.getUser(params.getParameter("user"));
            if (waitFor == null) return CommandResponse.errorResponse("No such user");
            ensureNotifications(user);
            Wait wait = waitDAO.getWait(user, waitFor);
            if (wait != null) return CommandResponse.errorResponse("Wait already added for that user");
            wait = new Wait(user, waitFor);
            wait = waitDAO.save(wait);
            return CommandResponse.resultResponse("wait", wait);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private void ensureNotifications(BotUser user) {
        Collection<Notification> notifications = notificationDAO.getNotifications(user, NotificationType.WAIT);
        if (notifications.isEmpty()) {
            notificationDAO.save(new Notification(user, NotificationType.WAIT, NotificationMethod.PM));
        }
    }
}
