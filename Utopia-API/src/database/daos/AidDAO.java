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
import api.database.transactions.Transactional;
import com.google.inject.Provider;
import database.models.Aid;
import database.models.AidType;
import database.models.Province;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@ParametersAreNonnullByDefault
public class AidDAO extends AbstractDAO<Aid> {
    @Inject
    public AidDAO(final Provider<Session> sessionProvider) {
        super(Aid.class, sessionProvider);
    }

    @Transactional
    public Aid getAid(long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public List<Aid> getAllAid() {
        return find();
    }

    @Transactional
    public int countAidAddedAfter(final Date date) {
        return find(Restrictions.gt("added", date)).size();
    }

    @Transactional
    public Aid getAid(final Province province, final AidType type) {
        return get(Restrictions.eq("province", province), Restrictions.eq("type", type));
    }
}
