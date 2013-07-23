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
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.communication.MailClient;
import api.tools.communication.MailException;
import com.google.common.collect.Lists;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static api.tools.text.StringUtil.isNullOrEmpty;

public class MailCommandHandler implements CommandHandler {
    private final MailClient mailClient;
    private final BotUserDAO userDAO;

    @Inject
    public MailCommandHandler(final MailClient mailClient, final BotUserDAO userDAO) {
        this.mailClient = mailClient;
        this.userDAO = userDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Collection<BotUser> users = "*".equals(params.getParameter("user")) ? userDAO.getAllUsers() : Lists.newArrayList(
                    userDAO.getClosestMatch(params.getParameter("user")));
            List<String> addys = new ArrayList<>(users.size());
            for (Iterator<BotUser> iter = users.iterator(); iter.hasNext(); ) {
                BotUser user = iter.next();
                if (user != null) {
                    String emailAddy = user.getEmail();
                    if (isNullOrEmpty(emailAddy)) iter.remove();
                    else addys.add(emailAddy);
                } else return CommandResponse.errorResponse("No such user");
            }
            if (addys.isEmpty())
                return CommandResponse.errorResponse("None of the specified users had email addresses set");
            mailClient.sendMail(context.getUser().getMainNick(), addys, params.getParameter("message"), params.getParameter("message"));
            return CommandResponse.resultResponse("users", users, "message", params.getParameter("message"));
        } catch (MailException | DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
