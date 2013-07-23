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

package database.daos;

import api.database.AbstractDAO;
import api.database.Transactional;
import api.database.models.BotUser;
import com.google.inject.Provider;
import database.models.Note;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class NoteDAO extends AbstractDAO<Note> {
    @Inject
    protected NoteDAO(final Provider<Session> sessionProvider) {
        super(Note.class, sessionProvider);
    }

    @Transactional
    public List<Note> getAllNotes() {
        return find(Order.asc("added"));
    }

    @Transactional
    public List<Note> getAllNotesForUser(final BotUser botUser, final BindingsManager bindingsManager) {
        List<Note> out = new ArrayList<>();
        for (Note note : find(Order.asc("added"))) {
            if (bindingsManager.matchesBindings(note.getBindings(), botUser)) out.add(note);
        }
        return out;
    }

    @Transactional
    public List<Note> getNewNotesForUser(final Date lastCheck, final BotUser botUser, final BindingsManager bindingsManager) {
        List<Note> out = new ArrayList<>();
        for (Note note : find(Restrictions.gt("added", lastCheck), Order.asc("added"))) {
            if (bindingsManager.matchesBindings(note.getBindings(), botUser)) out.add(note);
        }
        return out;
    }

    @Transactional
    public int countNotesAddedForUserAfter(final Date lastCheck, final BotUser botUser, final BindingsManager bindingsManager) {
        return getNewNotesForUser(lastCheck, botUser, bindingsManager).size();
    }

    @Transactional
    public Note getNote(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public void deleteAll() {
        delete(find());
    }

    @Transactional
    public void deleteNotes(final Collection<Long> ids) {
        delete(find(Restrictions.in("id", ids)));
    }

    @Transactional
    public void deleteAllOlderThan(final Date date) {
        delete(find(Restrictions.lt("added", date)));
    }

    @Transactional
    public void deleteAllExceptLast(final int amountToKeep) {
        List<Note> toRemove = new ArrayList<>();
        int i = 0;
        for (Note note : find(Order.desc("added"))) {
            if (i >= amountToKeep) toRemove.add(note);
            ++i;
        }
        delete(toRemove);
    }
}
