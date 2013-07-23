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

import api.database.JDBCWorkExecutor;
import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.tools.common.CleanupUtil;
import api.tools.database.DBUtil;
import com.google.inject.Provider;
import database.models.*;
import intel.Intel;
import lombok.extern.log4j.Log4j;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.ReturningWork;

import javax.inject.Inject;
import java.sql.*;
import java.util.*;
import java.util.Date;

@Log4j
public class IntelDAO {
    public static final String ID = "id";
    private final Provider<Session> sessionProvider;
    private final KingdomDAO kingdomDAO;
    private final BotUserDAO botUserDAO;

    @Inject
    public IntelDAO(final Provider<Session> sessionProvider, final KingdomDAO kingdomDAO, final BotUserDAO botUserDAO) {
        this.sessionProvider = sessionProvider;
        this.kingdomDAO = kingdomDAO;
        this.botUserDAO = botUserDAO;
    }

    @Transactional
    public int clearIntel(final Date everythingBefore, final JDBCWorkExecutor jdbcWorkExecutor) throws SQLException {
        return jdbcWorkExecutor.workWithJDBCConnection(new ReturningWork<Integer>() {
            @Override
            public Integer execute(final Connection connection) throws SQLException {
                int removed = 0;

                try (PreparedStatement sotPS = connection.prepareStatement("DELETE FROM sot WHERE last_updated < ?")) {
                    long maxAgeTimeInMillis = everythingBefore.getTime();

                    removed += removeIntelWithDependents(connection, "som", "army", "som_id", maxAgeTimeInMillis);

                    removed += removeIntelWithDependents(connection, "sos", "sos_entry", "parent_id", maxAgeTimeInMillis);

                    sotPS.setTimestamp(1, new Timestamp(maxAgeTimeInMillis));
                    removed += sotPS.executeUpdate();

                    removed += removeIntelWithDependents(connection, "survey", "survey_entry", "parent_id", maxAgeTimeInMillis);
                } catch (SQLException e) {
                    throw e;
                }
                return removed;
            }
        });
    }

    private static int removeIntelWithDependents(final Connection connection, final String table, final String subTable,
                                                 final String subTableWhereParam, final long time) throws SQLException {
        ResultSet rs = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM " + table + " WHERE last_updated < ?");
             PreparedStatement deletePS = connection.prepareStatement("DELETE FROM " + table + " WHERE id = ?");
             PreparedStatement subDeletePS = connection
                     .prepareStatement("DELETE FROM " + subTable + " WHERE " + subTableWhereParam + " = ?")) {
            List<Long> ids = new LinkedList<>();

            ps.setTimestamp(1, new Timestamp(time));
            rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getLong(ID));
            }
            for (Long id : ids) {
                deletePS.setLong(1, id);
                deletePS.addBatch();
                subDeletePS.setLong(1, id);
                subDeletePS.addBatch();
            }
            subDeletePS.executeBatch();
            int out = 0;
            for (int i : deletePS.executeBatch()) {
                out += i;
            }
            return out;
        } finally {
            CleanupUtil.closeSilently(rs);
        }
    }

    @Transactional
    public SoM getSoM(final long id) {
        return get(SoM.class, Restrictions.idEq(id));
    }

    @Transactional
    public List<SoM> getSoMsForKD(final String location) {
        Kingdom kingdom = getKingdom(location);
        if (kingdom == null) return null;
        Collection<Province> provinces = kingdom.getProvinces();
        List<SoM> soms = new ArrayList<>(provinces.size());
        for (Province province : provinces) {
            SoM som = province.getSom();
            if (som != null) soms.add(province.getSom());
        }
        return soms;
    }

    @Transactional
    public SoS getSoS(final long id) {
        return get(SoS.class, Restrictions.idEq(id));
    }

    @Transactional
    public List<SoS> getSoSsForKD(final String location) {
        Kingdom kingdom = getKingdom(location);
        if (kingdom == null) return null;
        Collection<Province> provinces = kingdom.getProvinces();
        List<SoS> soss = new ArrayList<>(provinces.size());
        for (Province province : provinces) {
            SoS sos = province.getSos();
            if (sos != null) soss.add(province.getSos());
        }
        return soss;
    }

    @Transactional
    public SoT getSoT(final long id) {
        return get(SoT.class, Restrictions.idEq(id));
    }

    @Transactional
    public List<SoT> getSoTsForKD(final String location) {
        Kingdom kingdom = getKingdom(location);
        if (kingdom == null) return null;
        Collection<Province> provinces = kingdom.getProvinces();
        List<SoT> sots = new ArrayList<>(provinces.size());
        for (Province province : provinces) {
            SoT sot = province.getSot();
            if (sot != null) sots.add(province.getSot());
        }
        return sots;
    }

    @Transactional
    public Survey getSurvey(final long id) {
        return get(Survey.class, Restrictions.idEq(id));
    }

    @Transactional
    public List<Survey> getSurveysForKD(final String location) {
        Kingdom kingdom = getKingdom(location);
        if (kingdom == null) return null;
        Collection<Province> provinces = kingdom.getProvinces();
        List<Survey> surveys = new ArrayList<>(provinces.size());
        for (Province province : provinces) {
            Survey survey = province.getSurvey();
            if (survey != null) surveys.add(province.getSurvey());
        }
        return surveys;
    }

    @Transactional
    public boolean saveIntel(final Intel intel, final long userId, final DelayedEventPoster delayedEventPoster) {
        if (intel.isUnsaved()) {
            sessionProvider.get().save(intel);
        } else {
            sessionProvider.get().merge(intel);
        }
        delayedEventPoster.enqueue(intel.newSavedEvent());
        if (!intel.getKingdomLocation().equals(kingdomDAO.getSelfKD().getLocation())) {
            BotUser user = botUserDAO.getUser(userId);
            user.incrementStat(intel.getIntelType(), 1);
        }
        return true;
    }

    @Transactional
    public void saveIntel(final Collection<Intel> intel, final BotUser user, final DelayedEventPoster delayedEventPoster) {
        for (Intel item : intel) {
            saveIntel(item, user.getId(), delayedEventPoster);
        }
    }

    @Transactional
    public void deleteIntel(final Intel intel) {
        sessionProvider.get().delete(intel);
    }

    @Transactional
    public void deleteIntelRelatedObject(final Object intelRelatedObject) {
        sessionProvider.get().delete(intelRelatedObject);
    }

    private Kingdom getKingdom(final String location) {
        return kingdomDAO.getKingdom(location);
    }

    private <E> E get(Class<E> clazz, Object... criterion) {
        List<E> list = find(clazz, criterion);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private <E> List<E> find(Class<E> clazz, final Object... criterion) {
        Criteria criteria = sessionProvider.get().createCriteria(clazz).setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        criteria = DBUtil.resolveAndAddCriterion(criteria, criterion);
        return listAndCast(clazz, criteria);
    }

    @SuppressWarnings("unchecked")
    private static <E> List<E> listAndCast(Class<E> clazz, final Criteria criteria) {
        return criteria.list();
    }
}
