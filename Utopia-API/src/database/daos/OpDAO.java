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


public class OpDAO {
    private final Provider<Session> sessionProvider;

    @Inject
    public OpDAO(final Provider<Session> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    @Transactional
    public OpType getOpType(final long id) {
        return get(OpType.class, Restrictions.idEq(id));
    }

    @Transactional
    public List<OpType> getOpTypes(final Long... ids) {
        return find(OpType.class, Restrictions.in("id", ids));
    }

    @Transactional
    public OpType getOpType(final String type) {
        return get(OpType.class, Restrictions.or(Restrictions.ilike("name", type), Restrictions.ilike("shortName", type)));
    }

    @Transactional
    public Collection<OpType> getAllOpTypes() {
        return find(OpType.class);
    }

    @Transactional
    public String getOpTypeGroup() {
        List<String> list = new ArrayList<>();
        for (OpType opType : getAllOpTypes()) {
            list.add(opType.getName());
            if (opType.getShortName() != null) list.add(opType.getShortName());
        }
        return StringUtil.merge(list, '|');
    }

    @Transactional
    public OpType save(final OpType opType) {
        for (Bonus bonus : opType.getBonuses()) {
            sessionProvider.get().saveOrUpdate(bonus);
        }
        sessionProvider.get().saveOrUpdate(opType);
        return opType;
    }

    @Transactional
    public Collection<OpType> save(final Collection<OpType> opTypes) {
        for (OpType opType : opTypes) {
            save(opType);
        }
        return opTypes;
    }

    @Transactional
    public DurationOp save(final DurationOp op) {
        sessionProvider.get().saveOrUpdate(op);
        return op;
    }

    @Transactional
    public InstantOp save(final InstantOp op) {
        sessionProvider.get().saveOrUpdate(op);
        return op;
    }

    @Transactional
    public void delete(final OpType opType) {
        opType.clear();
        sessionProvider.get().delete(opType);
    }

    @Transactional
    public void delete(final InstantOp op) {
        sessionProvider.get().delete(op);
    }

    @Transactional
    public void delete(final DurationOp op) {
        sessionProvider.get().delete(op);
    }

    @Transactional
    public void delete(final Collection<OpType> opTypes) {
        for (OpType opType : opTypes) {
            delete(opType);
        }
    }

    @Transactional
    public DurationOp getDurationOp(final long id) {
        return get(DurationOp.class, Restrictions.idEq(id));
    }

    @Transactional
    public List<DurationOp> getDurationOps(final OpType... types) {
        return isEmpty(types) ? find(DurationOp.class) : find(DurationOp.class, Restrictions.in("type", types));
    }

    @Transactional
    public List<DurationOp> getDurationOps(final Kingdom kingdom, final OpType... types) {
        NestedCriterion nestedCriterion = new NestedCriterion("province", new Object[]{Restrictions.eq("kingdom", kingdom)});
        return isEmpty(types) ? find(DurationOp.class, nestedCriterion)
                : find(DurationOp.class, nestedCriterion, Restrictions.in("type", types));
    }

    @Transactional
    public List<DurationOp> getDurationOpsCommittedByUser(final BotUser user, final OpType... types) {
        return isEmpty(types) ? find(DurationOp.class, Restrictions.eq("committer", user))
                : find(DurationOp.class, Restrictions.eq("committer", user),
                Restrictions.in("type", types));
    }

    @Transactional
    public void deleteDurationOps(final OpType... types) {
        for (DurationOp op : getDurationOps(types)) {
            sessionProvider.get().delete(op);
        }
    }

    @Transactional
    public void deleteDurationOps(final Kingdom kingdom, final OpType... types) {
        NestedCriterion nestedCriterion = new NestedCriterion("province", new Object[]{Restrictions.ne("kingdom", kingdom)});
        List<DurationOp> ops = isEmpty(types) ? find(DurationOp.class, nestedCriterion)
                : find(DurationOp.class, nestedCriterion, Restrictions.in("type", types));
        for (DurationOp op : ops) {
            sessionProvider.get().delete(op);
        }
    }

    @Transactional
    public List<InstantOp> getInstantOpsCommittedByUser(final BotUser user, final OpType... types) {
        return isEmpty(types) ? find(InstantOp.class, Restrictions.eq("committer", user))
                : find(InstantOp.class, Restrictions.eq("committer", user),
                Restrictions.in("type", types));
    }

    @Transactional
    public InstantOp getInstantOp(final long id) {
        return get(InstantOp.class, Restrictions.idEq(id));
    }

    @Transactional
    public List<InstantOp> getInstantOps(final OpType... types) {
        return isEmpty(types) ? find(InstantOp.class) : find(InstantOp.class, Restrictions.in("type", types));
    }

    @Transactional
    public void deleteInstantOps(final Kingdom kingdom, final OpType... types) {
        NestedCriterion nestedCriterion = new NestedCriterion("province", new Object[]{Restrictions.ne("kingdom", kingdom)});
        List<InstantOp> ops = isEmpty(types) ? find(InstantOp.class, nestedCriterion)
                : find(InstantOp.class, nestedCriterion, Restrictions.in("type", types));
        for (InstantOp op : ops) {
            sessionProvider.get().delete(op);
        }
    }

    @Transactional
    public void deleteInstantOps(final OpType... types) {
        List<InstantOp> ops =
                isEmpty(types) ? find(InstantOp.class) : find(InstantOp.class, Restrictions.in("type", types));
        for (InstantOp op : ops) {
            sessionProvider.get().delete(op);
        }
    }

    private <E> E get(final Class<E> clazz, final Object... criterion) {
        List<E> list = find(clazz, criterion);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private <E> List<E> find(final Class<E> clazz, final Object... criterion) {
        Criteria criteria = sessionProvider.get().createCriteria(clazz).setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        criteria = DBUtil.resolveAndAddCriterion(criteria, criterion);
        return listAndCast(clazz, criteria);
    }

    @SuppressWarnings("unchecked")
    private static <E> List<E> listAndCast(final Class<E> clazz, final Criteria criteria) {
        return criteria.list();
    }

    @Transactional
    public List<DurationOp> deleteDurationOps(final Date before) {
        List<DurationOp> expires = find(DurationOp.class, Restrictions.le("expires", before));
        delete(expires);
        return expires;
    }

    private void delete(final Iterable<?> stuff) {
        for (Object o : stuff) {
            sessionProvider.get().delete(o);
        }
    }
}
