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

package commands.activity.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.EventDAO;
import database.models.AttendanceStatus;
import database.models.AttendanceType;
import database.models.Event;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.*;

public class AttendanceCommandHandler implements CommandHandler {
    private final EventDAO eventDAO;

    @Inject
    public AttendanceCommandHandler(final EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    @Override
    public CommandResponse handleCommand(final IRCContext context, final Params params, final Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Event event = params.containsKey("wave") ? eventDAO.getWave() : eventDAO.getEvent(params.getLongParameter("id"));
            if (event == null) return CommandResponse.errorResponse("No event found");

            Map<String, List<AttendanceStatus>> map = new HashMap<>();
            for (AttendanceType type : AttendanceType.values()) {
                map.put(type.name(), new ArrayList<AttendanceStatus>());
            }
            for (AttendanceStatus status : event.getAttendanceInformation()) {
                map.get(status.getType().name()).add(status);
            }

            return CommandResponse.resultResponse("attendance", map);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
