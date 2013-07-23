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
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.CommonEntitiesAccess;
import database.daos.KingdomDAO;
import database.daos.ProvinceDAO;
import database.models.Kingdom;
import database.models.Personality;
import database.models.Province;
import database.models.Race;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;

public class AddProvinceCommandHandler implements CommandHandler {
    private final BotUserDAO userDAO;
    private final ProvinceDAO provinceDAO;
    private final KingdomDAO kingdomDAO;
    private final CommonEntitiesAccess commonEntitiesAccess;

    @Inject
    public AddProvinceCommandHandler(final KingdomDAO kingdomDAO, final CommonEntitiesAccess commonEntitiesAccess,
                                     final ProvinceDAO provinceDAO, final BotUserDAO userDAO) {
        this.kingdomDAO = kingdomDAO;
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.provinceDAO = provinceDAO;
        this.userDAO = userDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            BotUser user = userDAO.getUser(params.getParameter("user"));
            if (user == null) return CommandResponse.errorResponse("No such user exists");
            Race race = commonEntitiesAccess.getRace(params.getParameter("race"));
            Personality personality = commonEntitiesAccess.getPersonality(params.getParameter("personality"));
            Province existingProvince = provinceDAO.getProvinceForUser(user);
            String provinceName = params.getParameter("name").trim();
            if (existingProvince != null && !existingProvince.getName().equalsIgnoreCase(provinceName))
                provinceDAO.delete(existingProvince);
            Province province = provinceDAO.getProvince(provinceName);
            if (province == null) {
                Kingdom kingdom =
                        params.containsKey("kd") ? kingdomDAO.getOrCreateKingdom(params.getParameter("kd")) : kingdomDAO.getSelfKD();
                province = new Province(provinceName, kingdom, race, personality, user);
            } else {
                province.setName(provinceName);
                province.setProvinceOwner(user);
                province.setRace(race);
                province.setPersonality(personality);
            }
            provinceDAO.save(province);
            return CommandResponse.resultResponse("province", province);
        } catch (final DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
