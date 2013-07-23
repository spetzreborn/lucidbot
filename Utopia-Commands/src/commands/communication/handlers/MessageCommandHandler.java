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

package commands.communication.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.irc.IRCEntityManager;
import api.irc.communication.IRCAccess;
import api.irc.entities.IRCUser;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.PrivateMessageDAO;
import database.models.PrivateMessage;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MessageCommandHandler implements CommandHandler {
    private final BotUserDAO userDAO;
    private final PrivateMessageDAO pmDAO;
    private final IRCEntityManager entityManager;
    private final IRCAccess ircAccess;

    @Inject
    public MessageCommandHandler(final PrivateMessageDAO pmDAO, final BotUserDAO userDAO, final IRCEntityManager entityManager,
                                 final IRCAccess ircAccess) {
        this.pmDAO = pmDAO;
        this.userDAO = userDAO;
        this.entityManager = entityManager;
        this.ircAccess = ircAccess;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Collection<BotUser> users = "*".equals(params.getParameter("user")) ? userDAO.getAllUsers() : Arrays.asList(
                    userDAO.getUser(params.getParameter("user")));
            Collection<PrivateMessage> messages = new ArrayList<>(users.size());
            BotUser sender = context.getBotUser();
            String message = params.getParameter("message");
            for (BotUser user : users) {
                if (user == null) return CommandResponse.errorResponse("No such user");
                if (!user.equals(sender)) {
                    PrivateMessage pm = new PrivateMessage(user, sender.getMainNick(), message);
                    messages.add(pm);
                    Collection<IRCUser> allOfUsersConnections = entityManager.getAllOfUsersConnections(user.getMainNick());
                    for (IRCUser ircUser : allOfUsersConnections) {
                        ircAccess.sendPrivateMessage(ircUser, "PM from " + sender.getMainNick() + ": " + message);
                    }
                    if (!allOfUsersConnections.isEmpty()) pm.setRead(true);
                }
            }
            messages = pmDAO.save(messages);
            return CommandResponse.resultResponse("messages", messages);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
