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
import api.tools.files.FilterUtil;
import database.daos.KingdomDAO;
import database.models.Kingdom;
import database.models.Province;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.*;

import static api.tools.text.StringUtil.lowerCase;

public class MissingCommandHandler implements CommandHandler {
    private final KingdomDAO kingdomDAO;

    @Inject
    public MissingCommandHandler(final KingdomDAO kingdomDAO) {
        this.kingdomDAO = kingdomDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Kingdom kingdom =
                    params.containsKey("location") ? kingdomDAO.getKingdom(params.getParameter("location")) : kingdomDAO.getSelfKD();
            if (kingdom == null) return CommandResponse.errorResponse("Could not find KD");

            Set<Province> missing = new HashSet<>(kingdom.getProvinces());
            String type = params.containsKey("type") ? lowerCase(params.getParameter("type")) : "sot";
            missing.removeAll(getProvincesWithIntel(missing, type));
            FilterUtil.applyFilters(missing, filters);

            if (missing.isEmpty()) return CommandResponse.errorResponse("No matches found");

            return CommandResponse.resultResponse("provinces", missing);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static List<Province> getProvincesWithIntel(final Collection<Province> allProvs, final String type) {
        List<Province> out = new ArrayList<>(allProvs.size());
        switch (type) {
            case "som":
                for (Province prov : allProvs) {
                    if (prov.getSom() != null) out.add(prov);
                }
                break;
            case "sos":
                for (Province prov : allProvs) {
                    if (prov.getSos() != null) out.add(prov);
                }
                break;
            case "sot":
                for (Province prov : allProvs) {
                    if (prov.getSot() != null) out.add(prov);
                }
                break;
            case "survey":
                for (Province prov : allProvs) {
                    if (prov.getSurvey() != null) out.add(prov);
                }
                break;
        }
        return out;
    }
}
