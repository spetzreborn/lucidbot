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
import database.daos.ProvinceDAO;
import database.daos.UserCheckinDAO;
import database.models.Province;
import database.models.UserCheckIn;
import events.UserCheckinEvent;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;

public class CheckInCommandHandler implements CommandHandler {
    private final UserCheckinDAO checkinDAO;
    private final ProvinceDAO provinceDAO;
    private final BotUserDAO botUserDAO;

    @Inject
    public CheckInCommandHandler(final UserCheckinDAO checkinDAO, final ProvinceDAO provinceDAO, final BotUserDAO botUserDAO) {
        this.checkinDAO = checkinDAO;
        this.provinceDAO = provinceDAO;
        this.botUserDAO = botUserDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            BotUser user = null;
            String details = params.getParameter("details").trim();

            int firstSpace = details.indexOf(' ');
            if (firstSpace != -1) {
                String possibleNick = details.substring(0, firstSpace);
                user = botUserDAO.getUser(possibleNick);
                if (user != null) details = details.substring(firstSpace).trim();
            }
            if (user == null) user = context.getBotUser();

            Province province = provinceDAO.getProvinceForUser(user);
            if (province == null) return CommandResponse.errorResponse("You can't check in unless you're registered to a province");
            UserCheckIn checkIn = checkinDAO.getCheckinForUser(user);
            if (checkIn == null) checkIn = checkinDAO.save(new UserCheckIn(user, province, details));
            else {
                checkIn.setCheckedIn(details);
                checkIn.setCheckInTime(new Date());
            }
            delayedEventPoster.enqueue(new UserCheckinEvent(checkIn.getId(), context));
            return CommandResponse.resultResponse("checkin", checkIn);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
