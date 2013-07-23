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
import api.tools.collections.Params;
import database.daos.ProvinceDAO;
import database.models.Province;
import database.models.SoT;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;

public class DraftCommandHandler implements CommandHandler {
    private final ProvinceDAO provinceDAO;

    @Inject
    public DraftCommandHandler(final ProvinceDAO provinceDAO) {
        this.provinceDAO = provinceDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            int ticks;
            if (params.getDoubleParameter("goal") > 0 && params.getDoubleParameter("speed") > 0 && params.size() == 2) {
                Province province = provinceDAO.getProvinceForUser(context.getBotUser());
                if (province == null || province.getSot() == null)
                    return CommandResponse.errorResponse("You need to have a province and post intel to use the command like that");
                ticks = calcDraftTime(province, params.getDoubleParameter("goal"), params.getDoubleParameter("speed"));
            } else if (params.getDoubleParameter("goal") > 0 && params.getDoubleParameter("speed") > 0 &&
                    params.getIntParameter("total") > 0 &&
                    params.getIntParameter("peasants") > 0 && params.getIntParameter("wizards") >= 0) {
                ticks = calcDraftTime(null, params.getDoubleParameter("goal"), params.getDoubleParameter("speed"),
                        params.getIntParameter("total"), params.getIntParameter("peasants"),
                        params.getIntParameter("wizards"));
            } else return CommandResponse.errorResponse("Syntax error. Negative values are not valid");

            return ticks < 0 ? CommandResponse.errorResponse("You're either overdrafted already or don't have enough peasants")
                    : CommandResponse.resultResponse("result", ticks);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static int calcDraftTime(final Province prov, final double goaldraft, final double draftspeed, final int... args) {
        int out = -1;
        int totalpop;
        int peasants;
        int wizards;
        if (args.length == 3) {
            totalpop = args[0];
            peasants = args[1];
            wizards = args[2];
        } else {
            SoT sot = prov.getSot();
            totalpop = sot.getDefSpecs() + sot.getOffSpecs() + sot.getSoldiers() + sot.getPeasants() +
                    prov.getThieves() + prov.getWizards() + sot.getElites();
            peasants = sot.getPeasants();
            wizards = prov.getWizards();
        }
        int todraft = (int) (totalpop * goaldraft - (totalpop - peasants - wizards));
        if (todraft > peasants || todraft < 0) return out;
        out = (int) Math.ceil(Math.log(1 - todraft * 1.0 / peasants) / Math.log(1 - draftspeed));
        return out;
    }
}
