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
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.NoteDAO;
import database.daos.UserActivitiesDAO;
import database.models.Note;
import database.models.UserActivities;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BindingsManager;
import tools.RecentActivitiesFinder;
import tools.user_activities.RecentActivityType;
import tools.user_activities.UnseenInfo;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class NotesCommandHandler implements CommandHandler {
    private final NoteDAO noteDAO;
    private final UserActivitiesDAO userActivitiesDAO;
    private final BindingsManager bindingsManager;
    private final RecentActivitiesFinder recentActivitiesFinder;

    @Inject
    public NotesCommandHandler(final NoteDAO noteDAO, final UserActivitiesDAO userActivitiesDAO, final BindingsManager bindingsManager,
                               final RecentActivitiesFinder recentActivitiesFinder) {
        this.noteDAO = noteDAO;
        this.userActivitiesDAO = userActivitiesDAO;
        this.bindingsManager = bindingsManager;
        this.recentActivitiesFinder = recentActivitiesFinder;
    }

    @Override
    public CommandResponse handleCommand(final IRCContext context, final Params params, final Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        List<Note> notes = new ArrayList<>();
        BotUser botUser = context.getBotUser();
        UserActivities userActivities = userActivitiesDAO.getUserActivities(botUser);

        if (params.containsKey("all")) {
            notes.addAll(noteDAO.getAllNotesForUser(botUser, bindingsManager));
        } else if (params.containsKey("amount")) {
            int amount = params.getIntParameter("amount");
            for (Note note : noteDAO.getAllNotesForUser(botUser, bindingsManager)) {
                notes.add(note);
                if (notes.size() == amount) break;
            }
        } else {
            Date lastNotesCheck = userActivities.getLastNotesCheck();
            notes.addAll(noteDAO.getNewNotesForUser(lastNotesCheck, botUser, bindingsManager));
        }
        userActivities.setLastNotesCheck(new Date());
        List<UnseenInfo> unseenInfoOfInterest = recentActivitiesFinder.mapUnseenActivities(botUser, userActivities);
        return CommandResponse.resultResponse("notes", notes, "unseenInfoOfInterest", unseenInfoOfInterest, "thisActivityType",
                RecentActivityType.NOTES);
    }
}
