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
import api.database.Transactional;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import database.models.NewsItem;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Restrictions;
import tools.parsing.NonAttackNewsTypes;

import javax.inject.Inject;
import java.util.*;

import static api.tools.collections.CollectionUtil.isNotEmpty;
import static com.google.common.base.Preconditions.checkArgument;

public class NewsItemDAO extends AbstractDAO<NewsItem> {
    @Inject
    public NewsItemDAO(final Provider<Session> sessionProvider) {
        super(NewsItem.class, sessionProvider);
    }

    @Transactional
    public NewsItem getLastWarStart() {
        TreeSet<NewsItem> treeSet = new TreeSet<>(find(Restrictions.in("newsType", Lists.newArrayList(
                NonAttackNewsTypes.OUTGOING_WAR_DECLARED,
                NonAttackNewsTypes.INCOMING_WAR_DECLARED,
                NonAttackNewsTypes.MAX_HOSTILITY_AUTO_WAR_DECLARATION
        ))));
        if (treeSet.isEmpty()) return null;
        return treeSet.last();
    }

    @Transactional
    public NewsItem getLastWarStart(String kingdom) {
        TreeSet<NewsItem> treeSet = new TreeSet<>(find(Restrictions.in("newsType", Lists.newArrayList(
                NonAttackNewsTypes.OUTGOING_WAR_DECLARED,
                NonAttackNewsTypes.INCOMING_WAR_DECLARED,
                NonAttackNewsTypes.MAX_HOSTILITY_AUTO_WAR_DECLARATION
        ))));
        if (treeSet.isEmpty()) return null;
        while (!treeSet.isEmpty()) {
            NewsItem item = treeSet.pollLast();
            if (kingdom.equals(item.getSource()) || kingdom.equals(item.getTarget())) return item;
        }
        return null;
    }

    @Transactional
    public NewsItem getLastWarEnd() {
        String[] values = {NonAttackNewsTypes.INCOMING_MUTUAL_PEACE_ACCEPTED, NonAttackNewsTypes.OUTGOING_MUTUAL_PEACE_ACCEPTED, NonAttackNewsTypes.INCOMING_WITHDRAWAL, NonAttackNewsTypes.OUTGOING_WITHDRAWAL};
        TreeSet<NewsItem> treeSet = new TreeSet<>(find(Restrictions.in("newsType", values)));
        if (treeSet.isEmpty()) return null;
        return treeSet.last();
    }

    @Transactional
    public NewsItem getLastWarEnd(String kingdom) {
        String[] values = {NonAttackNewsTypes.INCOMING_MUTUAL_PEACE_ACCEPTED, NonAttackNewsTypes.OUTGOING_MUTUAL_PEACE_ACCEPTED, NonAttackNewsTypes.INCOMING_WITHDRAWAL, NonAttackNewsTypes.OUTGOING_WITHDRAWAL};
        TreeSet<NewsItem> treeSet = new TreeSet<>(find(Restrictions.in("newsType", values)));
        if (treeSet.isEmpty()) return null;
        while (!treeSet.isEmpty()) {
            NewsItem item = treeSet.pollLast();
            if (kingdom.equals(item.getSource()) || kingdom.equals(item.getTarget())) return item;
        }
        return null;
    }

    @Transactional
    public List<NewsItem> getNewsBetween(Date start, Long startId, Date end, Long endId) {
        List<Criterion> criteria = new ArrayList<>(2);

        if (start != null || startId != null) {
            //Clarification: it either starts on a later date, or it starts on the same date but has a higher id
            LogicalExpression or = Restrictions.or(Restrictions.gt("realDate", start), Restrictions
                    .and(Restrictions.eq("realDate", start), Restrictions.gt("id", startId)));
            Criterion greaterThanStart = startId == null ? Restrictions.ge("realDate", start)
                    : start == null ? Restrictions.gt("id", startId) : or;
            criteria.add(greaterThanStart);
        }

        if (end != null || endId != null) {
            //Clarification: it either starts on an earlier date, or it starts on the same date but has a lower id
            LogicalExpression or = Restrictions.or(Restrictions.lt("realDate", end), Restrictions
                    .and(Restrictions.eq("realDate", end), Restrictions.lt("id", endId)));
            Criterion lessThanEnd =
                    endId == null ? Restrictions.le("realDate", end) : end == null ? Restrictions.lt("id", endId) : or;
            criteria.add(lessThanEnd);
        }
        return criteria.isEmpty() ? find() : find(criteria.toArray());
    }

    @Transactional
    public List<NewsItem> getNewsBetween(Date start, Date end) {
        return getNewsBetween(start, null, end, null);
    }

    @Transactional
    @Override
    public Collection<NewsItem> save(final Collection<NewsItem> objects) {
        checkArgument(isNotEmpty(objects));
        List<NewsItem> sorted = new LinkedList<>(objects);
        Collections.sort(sorted);

        NewsItem first = sorted.get(0);
        NewsItem last = sorted.get(sorted.size() - 1);

        Map<String, Integer> existing = new HashMap<>();
        for (NewsItem item : getNewsBetween(first.getRealDate(), last.getRealDate())) {
            String uniqueKey = item.getUniqueKey();
            Integer currentAmount = existing.get(uniqueKey);
            if (currentAmount == null) existing.put(uniqueKey, 1);
            else existing.put(uniqueKey, currentAmount + 1);
        }

        for (Iterator<NewsItem> iter = sorted.iterator(); iter.hasNext(); ) {
            NewsItem item = iter.next();
            String key = item.getUniqueKey();
            Integer alreadyExisting = existing.get(key);
            if (alreadyExisting != null) {
                --alreadyExisting;
                if (alreadyExisting < 1)
                    existing.remove(key);
                else existing.put(key, alreadyExisting);
                iter.remove();
            }
        }

        return super.save(sorted);
    }

    @Transactional
    public NewsItem getNewsItem(final long id) {
        return get(Restrictions.idEq(id));
    }
}
