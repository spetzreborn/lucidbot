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
import database.models.Wait;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import java.util.List;

public class WaitDAO extends AbstractDAO<Wait> {
    @Inject
    public WaitDAO(final Provider<Session> sessionProvider) {
        super(Wait.class, sessionProvider);
    }

    /**
     * Returns a list of Wait objects for the specified user, meaning the waits that user has added
     *
     * @param user the user
     * @return .
     */
    @Transactional
    public List<Wait> getWaitsForUser(BotUser user) {
        return find(Restrictions.eq("user", user));
    }

    /**
     * Returns a list of Wait objects based on others waiting for the specified user
     *
     * @param user the user
     * @return .
     */
    @Transactional
    public List<Wait> getWaitingFor(BotUser user) {
        return find(Restrictions.eq("waitFor", user));
    }

    @Transactional
    public Wait getWait(BotUser user, BotUser waitFor) {
        return get(Restrictions.eq("user", user), Restrictions.eq("waitFor", waitFor));
    }

    @Transactional
    public Wait getWait(long id) {
        return get(Restrictions.idEq(id));
    }
}
