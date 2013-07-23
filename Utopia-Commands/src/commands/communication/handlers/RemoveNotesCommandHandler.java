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

package commands.communication.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.numbers.NumberUtil;
import api.tools.text.StringUtil;
import api.tools.time.DateUtil;
import database.daos.NoteDAO;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.time.UtopiaTime;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class RemoveNotesCommandHandler implements CommandHandler {
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final NoteDAO noteDAO;

    @Inject
    public RemoveNotesCommandHandler(final UtopiaTimeFactory utopiaTimeFactory, final NoteDAO noteDAO) {
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.noteDAO = noteDAO;
    }

    @Override
    public CommandResponse handleCommand(final IRCContext context, final Params params, final Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            if (params.containsKey("maxAgeHours")) {
                Date date = new Date(System.currentTimeMillis() - DateUtil.hoursToMillis(params.getIntParameter("maxAgeHours")));
                noteDAO.deleteAllOlderThan(date);
            } else if (params.containsKey("maxAgeUtotime")) {
                UtopiaTime maxAgeUtoTime = utopiaTimeFactory.newUtopiaTime(params.getParameter("maxAgeUtotime"));
                noteDAO.deleteAllOlderThan(maxAgeUtoTime.getDate());
            } else if (params.containsKey("ids")) {
                String[] idStrings = StringUtil.splitOnSpace(params.getParameter("ids"));
                Collection<Long> ids = new ArrayList<>(idStrings.length);
                for (String idString : idStrings) {
                    long id = NumberUtil.parseLong(idString);
                    ids.add(id);
                }
                noteDAO.deleteNotes(ids);
            } else if (params.containsKey("all")) {
                noteDAO.deleteAll();
            }
            return CommandResponse.resultResponse("result", "");
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
