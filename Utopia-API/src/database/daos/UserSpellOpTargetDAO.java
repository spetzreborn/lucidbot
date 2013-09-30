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
import api.database.models.BotUser;
import api.database.transactions.Transactional;
import com.google.inject.Provider;
import database.models.UserSpellOpTarget;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.Collection;

@ParametersAreNonnullByDefault
public class UserSpellOpTargetDAO extends AbstractDAO<UserSpellOpTarget> {
    @Inject
    public UserSpellOpTargetDAO(final Provider<Session> sessionProvider) {
        super(UserSpellOpTarget.class, sessionProvider);
    }

    @Transactional
    public UserSpellOpTarget getUserSpellOpTarget(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public UserSpellOpTarget getUserSpellOpTarget(final BotUser user) {
        return get(Restrictions.eq("user", user));
    }

    @Transactional
    public Collection<UserSpellOpTarget> getAllUserSpellOpTargets() {
        return find();
    }
}
