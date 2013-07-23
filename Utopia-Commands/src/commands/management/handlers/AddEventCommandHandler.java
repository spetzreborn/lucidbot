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

package commands.management.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.time.TimeUtil;
import database.daos.EventDAO;
import database.models.Bindings;
import database.models.Event;
import events.EventAddedEvent;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BindingsManager;
import tools.time.UtopiaTime;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;

public class AddEventCommandHandler implements CommandHandler {
    private final BindingsManager bindingsManager;
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final EventDAO eventDAO;

    @Inject
    public AddEventCommandHandler(final EventDAO eventDAO, final UtopiaTimeFactory utopiaTimeFactory,
                                  final BindingsManager bindingsManager) {
        this.eventDAO = eventDAO;
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.bindingsManager = bindingsManager;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Bindings bindings = bindingsManager.parseBindings(params.getParameter("bindings"));
            UtopiaTime utoTime = utopiaTimeFactory.newUtopiaTime(params.getParameter("utoTime"));
            Date date = utoTime.getDate();
            if (date.getTime() < System.currentTimeMillis()) return CommandResponse.errorResponse("Can't set a event in the past");

            Event event = new Event(Event.EventType.EVENT, params.getParameter("description"), date);
            event.setBindings(bindings);
            event = eventDAO.save(event);
            delayedEventPoster.enqueue(new EventAddedEvent(event.getId(), context));
            return CommandResponse.resultResponse("event", event, "utodate", utoTime.formattedUT(), "timeLeft",
                    TimeUtil.compareDateToCurrent(event.getEventTime()));
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
