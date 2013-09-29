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
import api.database.DBException;
import api.database.models.BotUser;
import api.database.transactions.Transactional;
import api.timers.TimerManager;
import api.tools.files.FilterUtil;
import api.tools.text.StringUtil;
import com.google.inject.Provider;
import database.models.Aid;
import database.models.Army;
import database.models.Kingdom;
import database.models.Province;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import spi.filters.Filter;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.*;

import static api.tools.text.StringUtil.lowerCase;

@ParametersAreNonnullByDefault
public class ProvinceDAO extends AbstractDAO<Province> {
    private static final int MAX_BATCH = 40;

    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<TimerManager> timerManagerProvider;

    @Inject
    public ProvinceDAO(final Provider<Session> sessionProvider, final Provider<KingdomDAO> kingdomDAOProvider,
                       final Provider<TimerManager> timerManagerProvider) {
        super(Province.class, sessionProvider);
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.timerManagerProvider = timerManagerProvider;
    }

    @Transactional
    @Override
    public void delete(final Province province) {
        province.clearForRemoval();
        TimerManager timerManager = timerManagerProvider.get();
        try {
            for (Army army : province.getArmies()) {
                getSession().delete(army);
                timerManager.cancelTimer(Army.class, army.getId());
            }
            for (Aid aid : province.getAid()) {
                timerManager.cancelTimer(Aid.class, aid.getId());
            }
            getSession().delete(province);
        } catch (HibernateException e) {
            throw new DBException(e);
        }
    }

    @Transactional
    @Override
    public void delete(final Collection<Province> provinces) {
        for (Province province : provinces) {
            delete(province);
        }
    }

    @Transactional
    public List<Province> getAllProvinces() {
        return find();
    }

    @Transactional
    public Province getProvince(final String name) {
        return get(Restrictions.ilike("name", name));
    }

    @Transactional
    public Province getProvince(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public List<Province> getProvinces(final Long... ids) {
        return find(Restrictions.in("id", ids));
    }

    @Transactional
    public Province getProvinceForUser(final BotUser user) {
        return get(Restrictions.eq("provinceOwner", user));
    }

    @Transactional
    public void removeProvinceForUser(final BotUser user) {
        Province province = get(Restrictions.eq("provinceOwner", user));
        if (province != null) province.setOwner(null);
    }

    @Transactional
    public Province getClosestMatch(final String name) {
        return getBestMatch(find(Restrictions.ilike("name", '%' + name + '%')), name);
    }

    @Transactional
    public Province getClosestMatch(final String name, final Kingdom kingdom) {
        return getBestMatch(kingdom.getProvinces(), name);
    }

    @Transactional
    public Province getClosestMatchWithOwner(final String name) {
        return getBestMatch(find(Restrictions.ilike("name", '%' + name + '%'), Restrictions.isNotNull("provinceOwner")), name);
    }

    private static Province getBestMatch(final Collection<Province> provinces, final String name) {
        Province bestMatch = null;
        int bestMatchDistance = -1;
        for (Province prov : provinces) {
            if (lowerCase(prov.getName()).contains(lowerCase(name))) {
                int distance = StringUtil.getLevenshteinDistance(name, prov.getName());
                if (bestMatchDistance < 0 || distance < bestMatchDistance) {
                    bestMatch = prov;
                    bestMatchDistance = distance;
                }
            }
        }

        return bestMatch;
    }

    @Transactional
    public Province getOrCreateProvince(final String name, final String kdLoc) {
        Province existing = getProvince(name);
        if (existing != null) return existing;

        Kingdom kingdom = kingdomDAOProvider.get().getOrCreateKingdom(kdLoc);
        Province province = new Province(name, kingdom);
        return save(province);
    }

    @Transactional
    public List<Province> getProvincesPassingFilters(final Collection<Filter<?>> filters) {
        try {
            Criteria criteria = getSession().createCriteria(Province.class).setMaxResults(MAX_BATCH);

            Set<Province> provinces = new HashSet<>(100);
            boolean done = false;
            for (int counter = 0; ; counter += MAX_BATCH) {
                criteria.setFirstResult(counter);
                List<Province> list = listAndCast(criteria);
                if (list.size() < MAX_BATCH) done = true;
                FilterUtil.applyFilters(list, filters);
                provinces.addAll(list);
                if (done) break;
                getSession().clear();
            }
            return new ArrayList<>(provinces);
        } catch (Exception e) {
            throw new DBException(e);
        }
    }
}
