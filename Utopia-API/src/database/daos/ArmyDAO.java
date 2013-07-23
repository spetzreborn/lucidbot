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
import api.database.NestedCriterion;
import api.database.Transactional;
import api.database.models.BotUser;
import api.settings.PropertiesCollection;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import database.models.Army;
import database.models.Kingdom;
import database.models.Province;
import database.models.SoM;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import java.util.*;

import static tools.UtopiaPropertiesConfig.INTRA_KD_LOC;
import static tools.UtopiaPropertiesConfig.TIMERS_ANNOUNCE_ENEMY_ARMIES;

@Log4j
public class ArmyDAO extends AbstractDAO<Army> {
    private final PropertiesCollection properties;
    private final KingdomDAO kingdomDAO;
    private final ProvinceDAO provinceDAO;

    @Inject
    public ArmyDAO(final Provider<Session> sessionProvider,
                   final PropertiesCollection properties,
                   final KingdomDAO kingdomDAO,
                   final ProvinceDAO provinceDAO) {
        super(Army.class, sessionProvider);
        this.properties = properties;
        this.kingdomDAO = kingdomDAO;
        this.provinceDAO = provinceDAO;
    }

    @Transactional
    public List<Army> getAllArmies() {
        return find();
    }

    @Transactional
    public List<Army> getAllArmies(final Long... armyIds) {
        return find(Restrictions.in("id", armyIds));
    }

    @Transactional
    public List<Army> getAllArmiesOfType(final Army.ArmyType... armyType) {
        return find(Restrictions.in("type", armyType));
    }

    @Transactional
    public List<Army> getAllArmiesOutForKD(final String kingdomLocation) {
        return getArmiesOut(getKingdom(kingdomLocation));
    }

    @Transactional
    public List<Army> getArmiesOutForKD(final String kingdomLocation, final Integer limit) {
        return getArmiesOut(getKingdom(kingdomLocation), limit);
    }

    @Transactional
    public List<Army> getArmiesOutForKD(final String kingdomLocation, final Date limit) {
        return getArmiesOut(getKingdom(kingdomLocation), Restrictions.le("returningDate", limit));
    }

    private Kingdom getKingdom(final String kingdomLocation) {
        try {
            return kingdomDAO.getOrCreateKingdom(kingdomLocation);
        } catch (HibernateException e) {
            log.error("", e);
        }
        return null;
    }

    private List<Army> getArmiesOut(final Kingdom kd, final Object... restrictions) {
        if (kd == null) return Collections.emptyList();
        boolean isSelfKd = properties.get(INTRA_KD_LOC).equals(kd.getLocation());

        if (isSelfKd) {
            List<Army> armies = new ArrayList<>(100);
            Collection<Province> provsWithoutIRCArmies = new HashSet<>(kd.getProvinces());

            List<Object> rs = Lists.newArrayList((Object) Restrictions.in("province", kd.getProvinces()),
                    Restrictions.eq("type", Army.ArmyType.IRC_ARMY_OUT), Order.asc("returningDate"));
            if (restrictions != null) Collections.addAll(rs, restrictions);
            for (Army army : find(rs.toArray())) {
                provsWithoutIRCArmies.remove(army.getProvince());
                armies.add(army);
            }
            if (!provsWithoutIRCArmies.isEmpty()) {
                rs = Lists.newArrayList((Object) Restrictions.in("province", provsWithoutIRCArmies),
                        Restrictions.eq("type", Army.ArmyType.ARMY_OUT), Order.asc("returningDate"));
                if (restrictions != null) Collections.addAll(rs, restrictions);
                armies.addAll(find(rs.toArray()));
            }
            Collections.sort(armies);
            return armies;
        } else {
            List<Object> rs = Lists
                    .newArrayList((Object) Restrictions.in("province", kd.getProvinces()), Restrictions.eq("type", Army.ArmyType.ARMY_OUT),
                            Order.asc("returningDate"));
            if (restrictions != null) Collections.addAll(rs, restrictions);
            return find(rs.toArray());
        }
    }

    @Transactional
    public List<Province> getProvincesWithFullArmyHome(final String kingdomLocation) {
        Kingdom kd = getKingdom(kingdomLocation);
        if (kd == null) return Collections.emptyList();

        List<Province> provsWithoutArmiesOut = new ArrayList<>(kd.getProvinces());

        for (Army army : find(Restrictions.in("province", provsWithoutArmiesOut), Restrictions
                .or(Restrictions.eq("type", Army.ArmyType.ARMY_OUT), Restrictions.eq("type", Army.ArmyType.IRC_ARMY_OUT)))) {
            provsWithoutArmiesOut.remove(army.getProvince());
        }
        return provsWithoutArmiesOut;
    }

    @Transactional
    public List<Province> getProvincesWithSomeArmyHome(final String kingdomLocation) {
        List<Province> out = new ArrayList<>();
        for (Province province : kingdomDAO.getOrCreateKingdom(kingdomLocation).getProvinces()) {
            int generalsHome = province.getGeneralsHome();
            if (generalsHome > 0) out.add(province);
        }
        return out;
    }

    @Transactional
    public List<Army> getAllArmiesForUser(final BotUser user) {
        Province prov = provinceDAO.getProvinceForUser(user);
        return prov == null ? Collections.<Army>emptyList() : new ArrayList<>(prov.getArmies());
    }

    @Transactional
    public List<Army> getIRCArmiesForUser(final BotUser user) {
        Province prov = provinceDAO.getProvinceForUser(user);
        if (prov != null)
            return find(Restrictions.eq("province", prov), Restrictions.eq("type", Army.ArmyType.IRC_ARMY_OUT));
        return Collections.emptyList();
    }

    @Transactional
    public List<Army> getIntelArmiesOutForUser(final BotUser user) {
        Province prov = provinceDAO.getProvinceForUser(user);
        if (prov != null)
            return find(Restrictions.eq("province", prov), Restrictions.eq("type", Army.ArmyType.ARMY_OUT));
        return Collections.emptyList();
    }

    @Transactional
    public Army getArmy(final BotUser user, final int armyNumber) {
        Province prov = provinceDAO.getProvinceForUser(user);
        if (prov != null) return get(Restrictions.eq("province", prov), Restrictions.eq("armyNumber", armyNumber));
        return null;
    }

    @Transactional
    public Army getArmy(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public void deleteIntelArmy(final Army army) {
        Army home = get(Restrictions.eq("province", army.getProvince()), Restrictions.eq("type", Army.ArmyType.ARMY_HOME));
        if (home != null) {
            home.setGenerals(home.getGenerals() + army.getGenerals());
            home.setSoldiers(home.getSoldiers() + army.getSoldiers());
            home.setOffSpecs(home.getOffSpecs() + army.getOffSpecs());
            home.setElites(home.getElites() + army.getElites());
        }
        SoM som = army.getSom();
        if (som != null) som.getArmies().remove(army);
        delete(army);
    }

    @Transactional
    public List<Army> getArmiesForTimerAdding() {
        boolean includeEnemyArmies = properties.getBoolean(TIMERS_ANNOUNCE_ENEMY_ARMIES);
        List<Army> armies = new ArrayList<>();
        armies.addAll(find(Restrictions.eq("type", Army.ArmyType.IRC_ARMY_OUT)));
        if (includeEnemyArmies) {
            Kingdom kingdom = getKingdom(properties.get(INTRA_KD_LOC));
            armies.addAll(find(Restrictions.eq("type", Army.ArmyType.ARMY_OUT),
                    new NestedCriterion("province", new Object[]{Restrictions.ne("kingdom", kingdom)})));
        }
        return armies;
    }

    @Transactional
    public int getFirstAvailableArmyNo(final Province province) {
        int first = 1;
        for (Army army : find(Restrictions.eq("province", province), Restrictions.eq("type", Army.ArmyType.IRC_ARMY_OUT),
                Order.asc("armyNumber"))) {
            if (army.getArmyNumber() != first) return first;
            ++first;
        }
        return first;
    }

    @Transactional
    public List<Army> getReturningArmies(final Date before) {
        Date after = new Date();
        List<Army> armies = new ArrayList<>(
                find(Restrictions.eq("type", Army.ArmyType.IRC_ARMY_OUT), Restrictions.between("returningDate", after, before)));

        Set<Province> provsWithIRCArmies = new HashSet<>();
        for (Army army : armies) {
            provsWithIRCArmies.add(army.getProvince());
        }

        if (provsWithIRCArmies.isEmpty()) {
            armies.addAll(find(Restrictions.eq("type", Army.ArmyType.ARMY_OUT), Restrictions.between("returningDate", after, before)));
        } else {
            armies.addAll(find(Restrictions.eq("type", Army.ArmyType.ARMY_OUT), Restrictions.between("returningDate", after, before),
                    Restrictions.not(Restrictions.in("province", provsWithIRCArmies))));
        }
        Collections.sort(armies);
        return armies;
    }

    @Transactional
    public void clearReturnedArmies() {
        List<Army> expiredArmies = new ArrayList<>();

        for (Army army : find(Restrictions.le("returningDate", new Date()),
                Restrictions.in("type", EnumSet.of(Army.ArmyType.ARMY_OUT, Army.ArmyType.IRC_ARMY_OUT)))) {
            SoM som = army.getSom();
            if (som != null) som.getArmies().remove(army);
            expiredArmies.add(army);
        }
        delete(expiredArmies);
    }
}
