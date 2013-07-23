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
import api.database.models.AccessLevel;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.NotificationDAO;
import database.models.Notification;
import database.models.NotificationMethod;
import database.models.NotificationType;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AddNotificationCommandHandler implements CommandHandler {
    private final BotUserDAO botUserDAO;
    private final NotificationDAO notificationDAO;

    @Inject
    public AddNotificationCommandHandler(final BotUserDAO botUserDAO, final NotificationDAO notificationDAO) {
        this.botUserDAO = botUserDAO;
        this.notificationDAO = notificationDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            List<BotUser> users = new ArrayList<>();
            if (params.containsKey("user") && AccessLevel.ADMIN.allows(context.getUser(), context.getChannel())) {
                String nickname = params.getParameter("user");
                if ("*".equals(nickname)) users.addAll(botUserDAO.getAllUsers());
                else {
                    BotUser user = botUserDAO.getUser(nickname);
                    if (user == null) return CommandResponse.errorResponse("No such user");
                    users.add(user);
                }
            } else users.add(context.getBotUser());
            NotificationType type = NotificationType.getByName(params.getParameter("type"));
            NotificationMethod method = NotificationMethod.getByName(params.getParameter("method"));

            List<Notification> added = new ArrayList<>(users.size());
            for (BotUser user : users) {
                Notification existing = notificationDAO.getNotification(user, type, method);
                if (existing != null) continue;

                Notification notification = new Notification(user, type, method);
                if (notification.getMethod().userIsSupported(user)) {
                    added.add(notification);
                }
            }

            notificationDAO.save(added);
            return CommandResponse.resultResponse("notifications", added, "hasIgnored", added.size() != users.size());
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
