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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ToTrainCommandHandler implements CommandHandler {
    private final BuildDAO buildDAO;
    private final ProvinceDAO provinceDAO;
    private final BindingsManager bindingsManager;

    @Inject
    public ToTrainCommandHandler(final BindingsManager bindingsManager, final BuildDAO buildDAO, final ProvinceDAO provinceDAO) {
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
            if (province.getSot() == null) return CommandResponse.errorResponse("You need to send at least a SoT (preferably SoM too)");

            if (params.containsKey("id")) {
                Build toBuild = buildDAO.getBuild(params.getLongParameter("id"));
                if (toBuild == null) return CommandResponse.errorResponse("No build with that id found");
                return CommandResponse.resultResponse("train", getToTrainInfo(toBuild, province));
            } else if (params.containsKey("type")) {
                List<Build> builds = buildDAO.getBuilds(province.getRace(), province.getPersonality(), params.getParameter("type"));
                if (builds.isEmpty()) return CommandResponse.errorResponse("No build found with that type name");
                Build toBuild = builds.get(0);
                return CommandResponse.resultResponse("train", getToTrainInfo(toBuild, province));
            } else {
                List<Build> builds = buildDAO.getBuildsForUser(context.getBotUser(), bindingsManager);
                if (builds.isEmpty()) return CommandResponse.errorResponse("Found no suitable build for your setup");
                Collections.sort(builds);
                Build toBuild = builds.get(0);
                return CommandResponse.resultResponse("train", getToTrainInfo(toBuild, province));
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static List<TroopAmountPair> getToTrainInfo(final Build build, final Province province) {
        int totalLand = province.getLand();
        SoT sot = province.getSot();
        SoM som = province.getSom();
        Army inTraining = som == null ? null : som.getArmyInTraining();

        List<TroopAmountPair> out = new ArrayList<>();

        int wizards = province.getWizards();
        int thieves = province.getThieves() + (inTraining == null ? 0 : inTraining.getThieves());
        int offSpecs = sot.getOffSpecs() + (inTraining == null ? 0 : inTraining.getOffSpecs());
        int defSpecs = sot.getDefSpecs() + (inTraining == null ? 0 : inTraining.getDefSpecs());
        int elites = sot.getElites() + (inTraining == null ? 0 : inTraining.getElites());

        int wizardsGoal = (int) (build.getWpa() * totalLand);
        int thievesGoal = (int) (build.getTpa() * totalLand);
        int offSpecsGoal = (int) (build.getOspa() * totalLand);
        int defSpecsGoal = (int) (build.getDspa() * totalLand);
        int elitesGoal = (int) (build.getEpa() * totalLand);

        out.add(new TroopAmountPair("Off Specs", offSpecsGoal - offSpecs));
        out.add(new TroopAmountPair("Def Specs", defSpecsGoal - defSpecs));
        out.add(new TroopAmountPair("Elites", elitesGoal - elites));
        out.add(new TroopAmountPair("Wizards", wizardsGoal - wizards));
        out.add(new TroopAmountPair("Thieves", thievesGoal - thieves));

        return out;
    }

    public static class TroopAmountPair {
        private final String troop;
        private final Integer amount;

        public TroopAmountPair(final String troop, final Integer amount) {
            this.troop = troop;
            this.amount = amount;
        }

        public String getTroop() {
            return troop;
        }

        public Integer getAmount() {
            return amount;
        }
    }
}
