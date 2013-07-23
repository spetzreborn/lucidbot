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
import api.database.daos.BotUserDAO;
import api.database.daos.NicknameDAO;
import api.database.models.BotUser;
import api.database.models.Nickname;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.UserActivitiesDAO;
import database.models.UserActivities;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;

import static api.settings.PropertiesConfig.ALLOW_USER_REGISTRATION;

public class RegisterCommandHandler implements CommandHandler {
    private final boolean usersMayRegister;
    private final NicknameDAO nicknameDAO;
    private final BotUserDAO userDAO;
    private final UserActivitiesDAO userActivitiesDAO;

    @Inject
    public RegisterCommandHandler(@Named(ALLOW_USER_REGISTRATION) final boolean usersMayRegister, final NicknameDAO nicknameDAO,
                                  final BotUserDAO userDAO, final UserActivitiesDAO userActivitiesDAO) {
        this.usersMayRegister = usersMayRegister;
        this.nicknameDAO = nicknameDAO;
        this.userDAO = userDAO;
        this.userActivitiesDAO = userActivitiesDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            if (!usersMayRegister) return CommandResponse.errorResponse("You're not allowed to register");
            if (context.getUser().isAuthenticated() || context.getBotUser() != null)
                return CommandResponse.errorResponse("You're already registered");
            Nickname nick = nicknameDAO.getNickname(context.getUser().getCurrentNick());
            if (nick != null) return CommandResponse.errorResponse("That nick is already being used");
            BotUser newUser = new BotUser();
            newUser.setMainNick(context.getUser().getCurrentNick());
            newUser.setPassword("password");
            newUser.getNickList().add(new Nickname(newUser.getMainNick(), newUser));
            newUser = userDAO.save(newUser);
            userActivitiesDAO.save(new UserActivities(newUser));
            return CommandResponse.resultResponse("user", newUser);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
