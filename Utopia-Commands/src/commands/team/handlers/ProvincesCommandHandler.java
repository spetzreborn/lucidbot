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

package commands.team.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.files.FilterUtil;
import database.daos.KingdomDAO;
import database.models.Province;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProvincesCommandHandler implements CommandHandler {
    private final KingdomDAO kingdomDAO;

    @Inject
    public ProvincesCommandHandler(final KingdomDAO kingdomDAO) {
        this.kingdomDAO = kingdomDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            List<Province> provinces = kingdomDAO.getSelfKD().getSortedProvinces();
            FilterUtil.applyFilters(provinces, filters);
            if (provinces.isEmpty()) return CommandResponse.errorResponse("No provinces fit the criteria");

            List<Province> provincesWithOwners = new ArrayList<>(provinces.size());
            for (Province province : provinces) {
                if (province.getProvinceOwner() != null) provincesWithOwners.add(province);
            }
            if (provinces.isEmpty()) return CommandResponse.errorResponse("No provinces found");

            return CommandResponse.resultResponse("provinces", provincesWithOwners);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}