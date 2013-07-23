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

package commands.army.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.NicknameDAO;
import api.database.models.Nickname;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.AttackDAO;
import database.daos.ProvinceDAO;
import database.models.Attack;
import database.models.Province;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;

public class LastHitCommandHandler implements CommandHandler {
    private final ProvinceDAO provinceDAO;
    private final NicknameDAO nicknameDAO;
    private final AttackDAO attackDAO;

    @Inject
    public LastHitCommandHandler(final AttackDAO attackDAO,
                                 final NicknameDAO nicknameDAO,
                                 final ProvinceDAO provinceDAO) {
        this.attackDAO = attackDAO;
        this.nicknameDAO = nicknameDAO;
        this.provinceDAO = provinceDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            String userOrProvince = params.getParameter("target");
            Nickname bestUserMatch = params.containsKey("location") ? null : nicknameDAO.getClosestMatch(userOrProvince);
            Province bestProvMatch = provinceDAO.getClosestMatch(userOrProvince);
            Province usersProvince = bestUserMatch == null ? null : provinceDAO.getProvinceForUser(bestUserMatch.getUser());

            if ((bestUserMatch == null || usersProvince == null) && bestProvMatch == null)
                return CommandResponse.errorResponse("No user or province found");

            Attack attack;
            if (bestProvMatch == null) {
                attack = attackDAO.getLastHitMade(usersProvince);
            } else if (bestUserMatch == null || usersProvince == null) {
                attack = attackDAO.getLastHitReceived(bestProvMatch);
            } else {
                attack = bestUserMatch.getName().length() <= bestProvMatch.getName().length() ? attackDAO.getLastHitMade(usersProvince) :
                        attackDAO.getLastHitReceived(bestProvMatch);
            }
            return attack == null ? CommandResponse.errorResponse("No hit found") : CommandResponse.resultResponse("attack", attack);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
