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

package commands.targets.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.TargetDAO;
import database.daos.UserActivitiesDAO;
import database.models.Target;
import database.models.UserActivities;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static api.tools.collections.CollectionUtil.isEmpty;

public class TargetsCommandHandler implements CommandHandler {
    private final TargetDAO targetDAO;
    private final BotUserDAO userDAO;
    private final UserActivitiesDAO userActivitiesDAO;
    private final BindingsManager bindingsManager;

    @Inject
    public TargetsCommandHandler(final BotUserDAO userDAO,
                                 final TargetDAO targetDAO,
                                 final BindingsManager bindingsManager,
                                 final UserActivitiesDAO userActivitiesDAO) {
        this.userDAO = userDAO;
        this.targetDAO = targetDAO;
        this.bindingsManager = bindingsManager;
        this.userActivitiesDAO = userActivitiesDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            UserActivities userActivities = userActivitiesDAO.getUserActivities(context.getBotUser());
            userActivities.setLastTargetsCheck(new Date());

            List<Target> targets = targetDAO.getTargetsOfType(Target.TargetType.GENERATED_TARGET, Target.TargetType.MANUAL_TARGET);
            if (targets.isEmpty()) return CommandResponse.errorResponse("No targets have been added");

            if (params.isEmpty()) targets = filterTargetsByBindings(targets, context.getBotUser());
            else if (params.containsKey("user")) {
                BotUser user = userDAO.getUser(params.getParameter("user"));
                if (user == null) return CommandResponse.errorResponse("No such user");
                targets = filterTargetsByBindings(targets, user);
            }
            //Implied here: the "all" parameter was used, meaning no filtering at all should be used

            if (isEmpty(targets)) return CommandResponse.errorResponse("No matching targets found");

            return CommandResponse.resultResponse("targets", targets);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private List<Target> filterTargetsByBindings(Iterable<Target> targets, BotUser user) {
        List<Target> out = new ArrayList<>();
        for (Target target : targets) {
            if (bindingsManager.matchesBindings(target.getBindings(), user) || target.containsUserAsHitter(user))
                out.add(target);
        }
        return out;
    }

}
