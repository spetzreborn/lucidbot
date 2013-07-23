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

package api.database;

import api.tools.database.DBUtil;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A convenience base implementation for DAOs
 *
 * @param <E> the type of item this DAO handles
 */
@ParametersAreNonnullByDefault
public abstract class AbstractDAO<E> {
    private final Class<E> clazz;
    private final Provider<Session> sessionProvider;

    protected AbstractDAO(final Class<E> clazz, final Provider<Session> sessionProvider) {
        this.sessionProvider = sessionProvider;
        this.clazz = checkNotNull(clazz);
    }

    protected Session getSession() {
        return sessionProvider.get();
    }

    @Transactional
    public void delete(final E object) {
        delete(Lists.newArrayList(checkNotNull(object)));
    }

    @Transactional
    public void delete(final Collection<E> objects) {
        try {
            for (final E object : objects) {
                getSession().delete(object);
            }
        } catch (HibernateException e) {
            throw new DBException(e);
        }
    }

    @Transactional
    @Nullable
    protected E get(@Nullable final Object... criterion) {
        List<E> list = find(criterion);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    @Transactional
    @Nullable
    protected <T> T get(final Class<T> clazz, @Nullable final Object... criterion) {
        List<T> list = find(checkNotNull(clazz), criterion);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    @Transactional
    protected List<E> find(@Nullable final Object... criterion) {
        try {
            Criteria criteria = getSession().createCriteria(clazz).setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
            criteria = DBUtil.resolveAndAddCriterion(criteria, criterion);
            return listAndCast(criteria);
        } catch (Exception e) {
            throw new DBException(e);
        }
    }

    @Transactional
    protected <T> List<T> find(final Class<T> clazz, @Nullable final Object... criterion) {
        try {
            Criteria criteria = getSession().createCriteria(checkNotNull(clazz))
                    .setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
            criteria = DBUtil.resolveAndAddCriterion(criteria, criterion);
            return listAndCast(clazz, criteria);
        } catch (Exception e) {
            throw new DBException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected List<E> listAndCast(final Criteria criteria) {
        try {
            return criteria.list();
        } catch (HibernateException e) {
            throw new DBException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> List<T> listAndCast(final Class<T> clazz, final Criteria criteria) {
        try {
            return criteria.list();
        } catch (HibernateException e) {
            throw new DBException(e);
        }
    }

    @Transactional
    public E save(final E object) {
        try {
            getSession().saveOrUpdate(checkNotNull(object));
            return object;
        } catch (HibernateException e) {
            throw new DBException(e);
        }
    }

    @Transactional
    public Collection<E> save(final Collection<E> objects) {
        for (final E object : objects) {
            save(object);
        }
        return objects;
    }
}
