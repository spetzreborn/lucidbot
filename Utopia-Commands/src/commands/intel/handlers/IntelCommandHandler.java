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

package commands.intel.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import com.google.common.collect.Lists;
import database.daos.KingdomDAO;
import database.daos.ProvinceDAO;
import database.models.Kingdom;
import database.models.Province;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BestMatchFinder;

import javax.inject.Inject;
import java.util.Collection;

public class IntelCommandHandler implements CommandHandler {
    private final KingdomDAO kingdomDAO;
    private final ProvinceDAO provinceDAO;
    private final BestMatchFinder bestMatchFinder;

    @Inject
    public IntelCommandHandler(final BestMatchFinder bestMatchFinder, final KingdomDAO kingdomDAO, final ProvinceDAO provinceDAO) {
        this.bestMatchFinder = bestMatchFinder;
        this.kingdomDAO = kingdomDAO;
        this.provinceDAO = provinceDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            if (params.isEmpty()) {
                if (context.getBotUser() == null) return CommandResponse.errorResponse("You're not registered");
                Province province = provinceDAO.getProvinceForUser(context.getBotUser());
                return province == null ? CommandResponse.errorResponse("No province is registered")
                        : CommandResponse.resultResponse("provinces", Lists.newArrayList(province));
            }
            if (params.containsKey("location") && params.size() == 1) {
                Kingdom kingdom = kingdomDAO.getKingdom(params.getParameter("location"));
                return kingdom == null ? CommandResponse.errorResponse("Could not find KD")
                        : CommandResponse.resultResponse("provinces", kingdom.getSortedProvinces());
            } else {
                Province province = params.containsKey("location") ? provinceDAO.getProvince(params.getParameter("nickOrProvince").trim())
                        : bestMatchFinder.findBestMatch(params.getParameter("nickOrProvince"));
                return province == null ? CommandResponse.errorResponse("Could not find province")
                        : CommandResponse.resultResponse("provinces", Lists.newArrayList(province));
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
