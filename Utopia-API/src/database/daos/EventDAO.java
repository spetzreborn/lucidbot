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
import database.models.Event;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static api.tools.time.DateUtil.isAfter;

public class EventDAO extends AbstractDAO<Event> {
    @Inject
    public EventDAO(final Provider<Session> sessionProvider) {
        super(Event.class, sessionProvider);
    }

    @Transactional
    public Event getWave() {
        return get(Restrictions.eq("type", Event.EventType.WAVE));
    }

    @Transactional
    public Event getEvent(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public List<Event> getAllEvents() {
        return find();
    }

    @Transactional
    public List<Event> getExpiringEvents(final Date before) {
        return find(Restrictions.lt("eventTime", before), Restrictions.ne("type", Event.EventType.WAVE));
    }

    @Transactional
    public List<Event> getEventsForUser(final BotUser botUser, final BindingsManager bindingsManager) {
        List<Event> out = new ArrayList<>();
        for (Event event : find(Restrictions.eq("type", Event.EventType.EVENT), Order.asc("eventTime"))) {
            if (bindingsManager.matchesBindings(event.getBindings(), botUser)) out.add(event);
        }
        return out;
    }

    @Transactional
    public int countEventsForUserAddedAfter(final Date date, final BotUser botUser, final BindingsManager bindingsManager) {
        int counter = 0;
        for (Event event : getEventsForUser(botUser, bindingsManager)) {
            if (isAfter(event.getAdded(), date)) ++counter;
        }
        return counter;
    }

    @Transactional
    public boolean waveAddedAfter(final Date date) {
        Event wave = getWave();
        return wave != null && isAfter(wave.getAdded(), date);
    }
}
