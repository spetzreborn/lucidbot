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
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.BuildDAO;
import database.daos.ProvinceDAO;
import database.models.*;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.*;

public class ToBuildCommandHandler implements CommandHandler {
    private final BuildDAO buildDAO;
    private final ProvinceDAO provinceDAO;
    private final BindingsManager bindingsManager;

    @Inject
    public ToBuildCommandHandler(final BindingsManager bindingsManager, final BuildDAO buildDAO, final ProvinceDAO provinceDAO) {
        this.bindingsManager = bindingsManager;
        this.buildDAO = buildDAO;
        this.provinceDAO = provinceDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            BotUser user = context.getBotUser();
            Province province = provinceDAO.getProvinceForUser(user);
            if (province == null) return CommandResponse.errorResponse("You're not registered to a province");
            Survey survey = province.getSurvey();
            if (survey == null) return CommandResponse.errorResponse("You need to upload a survey first");

            if (params.containsKey("id")) {
                Build toBuild = buildDAO.getBuild(params.getLongParameter("id"));
                if (toBuild == null) return CommandResponse.errorResponse("No build with that id found");
                return CommandResponse.resultResponse("build", getToBuildInfo(toBuild, survey));
            } else if (params.containsKey("type")) {
                List<Build> builds = buildDAO.getBuilds(province.getRace(), province.getPersonality(), params.getParameter("type"));
                if (builds.isEmpty()) return CommandResponse.errorResponse("No build found with that type name");
                Build toBuild = builds.get(0);
                return CommandResponse.resultResponse("build", getToBuildInfo(toBuild, survey));
            } else {
                List<Build> builds = buildDAO.getBuildsForUser(user, bindingsManager);
                if (builds.isEmpty()) return CommandResponse.errorResponse("Could not find a build for your setup");
                Collections.sort(builds);
                Build toBuild = builds.get(0);
                return CommandResponse.resultResponse("build", getToBuildInfo(toBuild, survey));
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static List<BuildingAmountPair> getToBuildInfo(final Build build, final Survey survey) {
        int totalLand = survey.getProvince().getLand();

        Map<Building, Integer> targetBuildMap = new HashMap<>();
        for (BuildEntry entry : build.getBuildings()) {
            int target = (int) (entry.getPercentage() / 100 * totalLand);
            targetBuildMap.put(entry.getBuilding(), target);
        }

        Map<Building, Integer> existing = new HashMap<>();
        for (SurveyEntry entry : survey.getBuildings()) {
            Building building = entry.getBuilding();
            if (!existing.containsKey(building)) existing.put(building, 0);
            existing.put(building, existing.get(building) + entry.getValue());
        }

        List<BuildingAmountPair> out = new ArrayList<>();
        for (Map.Entry<Building, Integer> entry : targetBuildMap.entrySet()) {
            Building building = entry.getKey();
            Integer goal = entry.getValue();
            Integer current = existing.remove(building);

            if (current == null) out.add(new BuildingAmountPair(building, goal));
            else out.add(new BuildingAmountPair(building, goal - current));
        }
        for (Map.Entry<Building, Integer> entry : existing.entrySet()) {
            out.add(new BuildingAmountPair(entry.getKey(), 0 - entry.getValue()));
        }
        return out;
    }

    public static class BuildingAmountPair {
        private final Building building;
        private final Integer amount;

        public BuildingAmountPair(final Building building, final Integer amount) {
            this.building = building;
            this.amount = amount;
        }

        public Building getBuilding() {
            return building;
        }

        public Integer getAmount() {
            return amount;
        }
    }
}
