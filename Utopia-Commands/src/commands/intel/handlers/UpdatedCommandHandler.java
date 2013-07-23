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
import database.daos.IntelDAO;
import database.daos.KingdomDAO;
import database.models.Kingdom;
import database.models.Province;
import intel.ProvinceIntel;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.*;

public class UpdatedCommandHandler implements CommandHandler {
    private final IntelDAO intelDAO;
    private final KingdomDAO kingdomDAO;

    @Inject
    public UpdatedCommandHandler(final IntelDAO intelDAO, final KingdomDAO kingdomDAO) {
        this.intelDAO = intelDAO;
        this.kingdomDAO = kingdomDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            String location = params.containsKey("location") ? params.getParameter("location") : null;
            Kingdom kingdom = location == null ? kingdomDAO.getSelfKD() : kingdomDAO.getKingdom(location);
            if (kingdom == null) return CommandResponse.errorResponse("Could not find KD");
            String type = params.containsKey("type") ? params.getParameter("type") : "sot";
            List<ProvinceIntel> provincesWithIntel = getProvincesWithIntel(kingdom.getLocation(), type, filters);
            if (provincesWithIntel.isEmpty()) return CommandResponse.errorResponse("No matches found");
            return CommandResponse.resultResponse("updated", provincesWithIntel);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private List<ProvinceIntel> getProvincesWithIntel(final String location, final String type,
                                                      final Collection<Filter<?>> filters) {
        List<ProvinceIntel> out = new ArrayList<>(25);
        switch (type) {
            case "som":
                out.addAll(intelDAO.getSoMsForKD(location));
                break;
            case "sos":
                out.addAll(intelDAO.getSoSsForKD(location));
                break;
            case "sot":
                out.addAll(intelDAO.getSoTsForKD(location));
                break;
            case "survey":
                out.addAll(intelDAO.getSurveysForKD(location));
                break;
        }
        FilterUtil.applyFilters(out, filters, Province.class);
        Collections.sort(out, new UpdatedComparator());
        return out;
    }

    private static class UpdatedComparator implements Comparator<ProvinceIntel> {
        @Override
        public int compare(final ProvinceIntel o1, final ProvinceIntel o2) {
            return o1.getLastUpdated().compareTo(o2.getLastUpdated());
        }
    }
}
