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

package commands.army.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.ArmyDAO;
import database.models.Army;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArmyTimeCommandHandler implements CommandHandler {
    private final BotUserDAO userDAO;
    private final ArmyDAO armyDAO;

    @Inject
    public ArmyTimeCommandHandler(final BotUserDAO userDAO, final ArmyDAO armyDAO) {
        this.userDAO = userDAO;
        this.armyDAO = armyDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            BotUser user = params.isEmpty() ? context.getBotUser() : userDAO.getClosestMatch(params.getParameter("user"));
            if (user == null) return CommandResponse.errorResponse("Could not find a user");
            List<Army> armies = armyDAO.getIRCArmiesForUser(user);
            List<ArmyTimePair> armyTimePairs = new ArrayList<>(armies.size());
            for (Army army : armies) {
                String time = user.getDateInUsersLocalTime(army.getReturningDate());
                armyTimePairs.add(new ArmyTimePair(army, time));
            }
            return CommandResponse.resultResponse("armies", armyTimePairs, "user", user);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    public static class ArmyTimePair {
        private final Army army;
        private final String time;

        private ArmyTimePair(final Army army, final String time) {
            this.army = army;
            this.time = time;
        }

        public Army getArmy() {
            return army;
        }

        public String getTime() {
            return time;
        }
    }
}
