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

package commands.calculator.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import api.tools.collections.Params;
import database.daos.ProvinceDAO;
import database.models.*;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;

import static tools.UtopiaPropertiesConfig.DEFAULT_BPA;

public class WPACalcCommandHandler implements CommandHandler {
    private final PropertiesCollection properties;
    private final ProvinceDAO provinceDAO;

    @Inject
    public WPACalcCommandHandler(final PropertiesCollection properties, final ProvinceDAO provinceDAO) {
        this.properties = properties;
        this.provinceDAO = provinceDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            String provName = params.getParameter("province").trim();

            Province province = provinceDAO.getProvince(provName);
            if (province == null) {
                return CommandResponse.errorResponse("There's no such province");
            } else {
                if (province.getRace() == null) return CommandResponse.errorResponse("No race info available");
                if (province.getSot() == null) return CommandResponse.errorResponse("No SoT available");
                if (province.getThievesLastUpdated() == null) return CommandResponse.errorResponse("No infiltrate info available");
                if (province.getNetworth() == 0) return CommandResponse.errorResponse("No networth available");
                int wizards = calcWizards(province);
                province.setWizards(wizards);
                province.setWizardsLastUpdated(new Date());

                return CommandResponse.resultResponse("result", wizards);
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private int calcWizards(final Province province) {
        SoT sot = province.getSot();
        Race race = province.getRace();

        int nw = province.getNetworth();
        nw -= province.getThieves() * 4;
        nw -= sot.getSoldiers() * race.getSoldierNetworth();
        nw -= sot.getPeasants();
        nw -= sot.getDefSpecs() * race.getDefSpecStrength();
        nw -= sot.getOffSpecs() * race.getOffSpecStrength() * 0.8;
        nw -= sot.getElites() * race.getEliteNetworth();
        nw -= sot.getWarHorses() * 0.6;
        nw -= sot.getMoney() / 1000.0;

        SoS sos = province.getSos();
        nw -= sos == null ? province.getLand() * properties.getInteger(DEFAULT_BPA) / 92.0 : sos.getTotalBooks() / 92.0;
        Survey survey = province.getSurvey();
        nw -= survey == null ? province.getLand() * 55 : survey.getTotalBuilt() * 55 + survey.getTotalInProgress() * 30 +
                (province.getLand() - survey.getTotalBuilt() - survey.getTotalInProgress()) * 15;
        return Math.max(0, nw / 4);
    }
}
