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
import api.tools.text.StringUtil;
import database.daos.UserCheckinDAO;
import database.models.Province;
import database.models.UserCheckIn;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class CheckedInCommandHandler implements CommandHandler {
    private final UserCheckinDAO checkinDAO;

    @Inject
    public CheckedInCommandHandler(final UserCheckinDAO checkinDAO) {
        this.checkinDAO = checkinDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Set<UserCheckIn> checkedIn = new TreeSet<>();
            if (params.isEmpty() && filters.isEmpty()) {
                if (context.getBotUser() == null) return CommandResponse.errorResponse("You're not registered");
                UserCheckIn checkIn = checkinDAO.getCheckinForUser(context.getBotUser());
                if (checkIn == null) return CommandResponse.errorResponse("You have not checked in");
                checkedIn.add(checkIn);
            } else if (params.containsKey("users")) {
                String[] nicks = StringUtil.splitOnSpace(params.getParameter("users"));
                for (UserCheckIn checkIn : checkinDAO.getAllCheckins()) {
                    if (checkIn.getUser().isOneOf(nicks)) checkedIn.add(checkIn);
                }
            } else if (params.containsKey("all")) {
                checkedIn.addAll(checkinDAO.getAllCheckins());
            } else {
                checkedIn.addAll(checkinDAO.getAllCheckins());
                FilterUtil.applyFilters(checkedIn, filters, Province.class);
            }
            if (checkedIn.isEmpty()) return CommandResponse.errorResponse("No results match the criterias");

            return CommandResponse.resultResponse("checkedin", checkedIn);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
