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

package commands.province_management.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.CommonEntitiesAccess;
import database.daos.BuildDAO;
import database.daos.UserActivitiesDAO;
import database.models.Build;
import database.models.Personality;
import database.models.Race;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class BuildCommandHandler implements CommandHandler {
    private final BuildDAO buildDAO;
    private final CommonEntitiesAccess commonEntitiesAccess;
    private final BindingsManager bindingsManager;
    private final UserActivitiesDAO userActivitiesDAO;

    @Inject
    public BuildCommandHandler(final BindingsManager bindingsManager, final CommonEntitiesAccess commonEntitiesAccess,
                               final BuildDAO buildDAO, final UserActivitiesDAO userActivitiesDAO) {
        this.bindingsManager = bindingsManager;
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.buildDAO = buildDAO;
        this.userActivitiesDAO = userActivitiesDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            userActivitiesDAO.getUserActivities(context.getBotUser()).setLastBuildCheck(new Date());
            if (params.containsKey("id")) {
                Build build = buildDAO.getBuild(params.getLongParameter("id"));
                if (build == null) return CommandResponse.errorResponse("No build with that id found");
                return CommandResponse.resultResponse("build", build, "evenNoOfBuildings", build.getBuildings().size() % 2 == 0);
            } else if (params.containsKey("race")) {
                Race race = commonEntitiesAccess.getRace(params.getParameter("race"));
                Personality personality = params.getParameter("personality") == null ? null : commonEntitiesAccess
                        .getPersonality(params.getParameter("personality"));
                String type = params.getParameter("type");
                List<Build> builds = buildDAO.getBuilds(race, personality, type);
                if (builds.isEmpty()) return CommandResponse.errorResponse("No builds were found");

                return builds.size() == 1 ? CommandResponse.resultResponse("build", builds.get(0), "evenNoOfBuildings", builds.get(0).getBuildings().size() % 2 == 0) :
                        CommandResponse.resultResponse("builds", builds, "race", race, "personality", personality);
            } else {
                List<Build> buildsForUser = buildDAO.getBuildsForUser(context.getBotUser(), bindingsManager);
                if (buildsForUser.isEmpty()) return CommandResponse.errorResponse("No builds were found");
                return buildsForUser.size() == 1 ? CommandResponse.resultResponse("build", buildsForUser.get(0), "evenNoOfBuildings", buildsForUser.get(0).getBuildings().size() % 2 == 0) :
                        CommandResponse.resultResponse("builds", buildsForUser);
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
