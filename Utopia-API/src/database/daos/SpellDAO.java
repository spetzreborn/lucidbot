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

import api.database.NestedCriterion;
import api.database.Transactional;
import api.database.models.BotUser;
import api.tools.database.DBUtil;
import api.tools.text.StringUtil;
import com.google.inject.Provider;
import database.models.*;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static api.tools.collections.ArrayUtil.isEmpty;


public class SpellDAO {
    private final Provider<Session> sessionProvider;

    @Inject
    public SpellDAO(final Provider<Session> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    @Transactional
    public SpellType getSpellType(long id) {
        return get(SpellType.class, Restrictions.idEq(id));
    }

    @Transactional
    public SpellType getSpellType(String type) {
        return get(SpellType.class, Restrictions.or(Restrictions.ilike("name", type), Restrictions.ilike("shortName", type)));
    }

    @Transactional
    public List<SpellType> getSpellTypes(final Long... ids) {
        return find(SpellType.class, Restrictions.in("id", ids));
    }

    @Transactional
    public Collection<SpellType> getAllSpellTypes() {
        return find(SpellType.class);
    }

    @Transactional
    public String getSpellTypeGroup() {
        List<String> list = new ArrayList<>();
        for (SpellType spellType : getAllSpellTypes()) {
            list.add(spellType.getName());
            if (spellType.getShortName() != null) list.add(spellType.getShortName());
        }
        return StringUtil.merge(list, '|');
    }

    @Transactional
    public SpellType save(SpellType spellType) {
        for (Bonus bonus : spellType.getBonuses()) {
            sessionProvider.get().saveOrUpdate(bonus);
        }
        sessionProvider.get().saveOrUpdate(spellType);
        return spellType;
    }

    @Transactional
    public Collection<SpellType> save(Collection<SpellType> spellTypes) {
        for (SpellType spellType : spellTypes) {
            save(spellType);
        }
        return spellTypes;
    }

    @Transactional
    public InstantSpell save(final InstantSpell spell) {
        sessionProvider.get().saveOrUpdate(spell);
        return spell;
    }

    @Transactional
    public DurationSpell save(final DurationSpell spell) {
        sessionProvider.get().saveOrUpdate(spell);
        return spell;
    }

    @Transactional
    public void delete(InstantSpell spell) {
        sessionProvider.get().delete(spell);
    }

    @Transactional
    public void delete(DurationSpell spell) {
        sessionProvider.get().delete(spell);
    }

    @Transactional
    public void delete(SpellType spellType) {
        spellType.clear();
        sessionProvider.get().delete(spellType);
    }

    @Transactional
    public void delete(Collection<SpellType> spellTypes) {
        for (SpellType spellType : spellTypes) {
            delete(spellType);
        }
    }

    @Transactional
    public List<DurationSpell> getDurationSpells(Kingdom kingdom, SpellType... types) {
        NestedCriterion nestedCriterion = new NestedCriterion("province", new Object[]{Restrictions.eq("kingdom", kingdom)});
        return isEmpty(types) ? find(DurationSpell.class, nestedCriterion)
                : find(DurationSpell.class, nestedCriterion, Restrictions.in("type", types));
    }

    @Transactional
    public List<DurationSpell> getDurationSpells(SpellType... types) {
        return isEmpty(types) ? find(DurationSpell.class) : find(DurationSpell.class, Restrictions.in("type", types));
    }

    @Transactional
    public List<DurationSpell> getDurationSpellsCommittedByUser(final BotUser user, final SpellType... types) {
        return isEmpty(types) ? find(DurationSpell.class, Restrictions.eq("committer", user))
                : find(DurationSpell.class, Restrictions.eq("committer", user),
                Restrictions.in("type", types));
    }

    @Transactional
    public void deleteDurationSpells(SpellType... types) {
        for (DurationSpell spell : getDurationSpells(types)) {
            sessionProvider.get().delete(spell);
        }
    }

    @Transactional
    public void deleteDurationSpells(final Kingdom kingdom, final SpellType... types) {
        NestedCriterion nestedCriterion = new NestedCriterion("province", new Object[]{Restrictions.ne("kingdom", kingdom)});
        List<DurationSpell> spells = isEmpty(types) ? find(DurationSpell.class, nestedCriterion)
                : find(DurationSpell.class, nestedCriterion,
                Restrictions.in("type", types));
        for (DurationSpell spell : spells) {
            sessionProvider.get().delete(spell);
        }
    }

    @Transactional
    public InstantSpell getInstantSpell(final long id) {
        return get(InstantSpell.class, Restrictions.idEq(id));
    }

    @Transactional
    public List<InstantSpell> getInstantSpells(final SpellType... types) {
        return isEmpty(types) ? find(InstantSpell.class) : find(InstantSpell.class, Restrictions.in("type", types));
    }

    @Transactional
    public List<InstantSpell> getInstantSpellsCastByUser(BotUser user, SpellType... types) {
        return isEmpty(types) ? find(InstantSpell.class, Restrictions.eq("caster", user))
                : find(InstantSpell.class, Restrictions.eq("caster", user),
                Restrictions.in("type", types));
    }

    @Transactional
    public void deleteInstantSpells(Kingdom kingdom, SpellType... types) {
        NestedCriterion nestedCriterion = new NestedCriterion("province", new Object[]{Restrictions.ne("kingdom", kingdom)});
        List<InstantSpell> spells = isEmpty(types) ? find(InstantSpell.class, nestedCriterion)
                : find(InstantSpell.class, nestedCriterion,
                Restrictions.in("type", types));
        for (InstantSpell spell : spells) {
            sessionProvider.get().delete(spell);
        }
    }

    @Transactional
    public void deleteInstantSpells(SpellType... types) {
        List<InstantSpell> spells =
                isEmpty(types) ? find(InstantSpell.class) : find(InstantSpell.class, Restrictions.in("type", types));
        for (InstantSpell spell : spells) {
            sessionProvider.get().delete(spell);
        }
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

    @Transactional
    public List<DurationSpell> deleteDurationSpells(final Date before) {
        List<DurationSpell> expires = find(DurationSpell.class, Restrictions.le("expires", before));
        delete(expires);
        return expires;
    }

    private void delete(final Iterable<?> stuff) {
        for (Object o : stuff) {
            sessionProvider.get().delete(o);
        }
    }

    @Transactional
    public DurationSpell getDurationSpell(final long id) {
        return get(DurationSpell.class, Restrictions.idEq(id));
    }
}
