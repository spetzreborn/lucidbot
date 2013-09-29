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
import api.settings.PropertiesCollection;
import com.google.inject.Provider;
import database.models.Kingdom;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import tools.UtopiaPropertiesConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@ParametersAreNonnullByDefault
public class KingdomDAO extends AbstractDAO<Kingdom> {
    private final PropertiesCollection properties;

    @Inject
    public KingdomDAO(final Provider<Session> sessionProvider, final PropertiesCollection properties) {
        super(Kingdom.class, sessionProvider);
        this.properties = properties;
    }

    @Transactional
    public Kingdom getOrCreateKingdom(final String location) {
        Kingdom kingdom = get(Restrictions.eq("location", location));
        if (kingdom == null) {
            kingdom = new Kingdom(location);
            super.save(kingdom);
        }
        return kingdom;
    }

    @Transactional
    public Kingdom getSelfKD() {
        return getOrCreateKingdom(properties.get(UtopiaPropertiesConfig.INTRA_KD_LOC));
    }

    @Transactional
    public Kingdom getKingdom(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public List<Kingdom> getKingdoms(final List<Long> ids) {
        return find(Restrictions.in("id", ids));
    }

    @Transactional
    public Kingdom getKingdom(final String location) {
        return get(Restrictions.eq("location", location));
    }

    @Transactional
    public List<Kingdom> getKingdomsByNameOrComment(final String nameOrComment) {
        return find(Restrictions.or(Restrictions.like("name", '%' + nameOrComment + '%'),
                Restrictions.like("kdComment", '%' + nameOrComment + '%')));
    }

    @Transactional
    public List<Kingdom> getAllKingdoms() {
        return find();
    }

    @Transactional
    public int countNapsAddedAfter(final Date date) {
        return find(Restrictions.gt("napAdded", date)).size();
    }
}
