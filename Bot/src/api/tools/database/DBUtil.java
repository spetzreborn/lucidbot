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

package api.tools.database;

import api.database.NestedCriterion;
import lombok.extern.log4j.Log4j;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;

import javax.annotation.Nullable;

@Log4j
public class DBUtil {
    private DBUtil() {
    }

    /**
     * Closes the specified session, ignoring any exceptions
     *
     * @param session the session to close
     */
    public static void closeSilently(@Nullable final Session session) {
        if (session != null) {
            try {
                session.close();
            } catch (HibernateException e) {
                //ignore
            }
        }
    }

    /**
     * Resolves the criterion and add them to the criteria.
     * Currently supports the follow criterion types:
     * - Criterion
     * - Order
     * - Integer (used as limit)
     * - NestedCriterion
     *
     * @param criteria  the Criteria to add to
     * @param criterion the criterion to add
     * @return the Criteria that was sent in, with the criterion added
     */
    public static Criteria resolveAndAddCriterion(final Criteria criteria, @Nullable final Object... criterion) {
        if (criterion == null) return criteria;

        for (Object cr : criterion) {
            if (cr instanceof Criterion) {
                criteria.add((Criterion) cr);
            } else if (cr instanceof Order) {
                criteria.addOrder((Order) cr);
            } else if (cr instanceof Integer) {
                criteria.setMaxResults((Integer) cr);
            } else if (cr instanceof NestedCriterion) {
                NestedCriterion nc = (NestedCriterion) cr;
                Criteria subCriteria = criteria.createCriteria(nc.getAssociation());
                resolveAndAddCriterion(subCriteria, nc.getCriterion());
            } else if (cr instanceof Object[]) {
                resolveAndAddCriterion(criteria, cr);
            }
        }
        return criteria;
    }

    /**
     * Performs a rollback on the specified session
     *
     * @param session the session to roll back
     */
    public static void rollback(final Session session) {
        try {
            session.getTransaction().rollback();
        } catch (HibernateException e) {
            log.error("Rollback failed", e);
        }
    }
}
