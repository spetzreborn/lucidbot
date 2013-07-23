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
import api.database.Transactional;
import api.tools.text.StringUtil;
import com.google.inject.Provider;
import database.models.Bonus;
import database.models.HonorTitle;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HonorTitleDAO extends AbstractDAO<HonorTitle> {
    @Inject
    public HonorTitleDAO(final Provider<Session> sessionProvider) {
        super(HonorTitle.class, sessionProvider);
    }

    @Transactional
    public String getHonorTitleGroup() {
        Collection<HonorTitle> titles = getAllHonorTitles();
        List<String> list = new ArrayList<>(titles.size() * 3);
        for (HonorTitle honorTitle : titles) {
            list.add(honorTitle.getName());
            if (StringUtil.isNotNullOrEmpty(honorTitle.getAlias())) list.add(honorTitle.getAlias());
        }
        return StringUtil.merge(list, '|');
    }

    @Transactional
    public HonorTitle getHonorTitle(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public HonorTitle getHonorTitle(final String nameOrAlias) {
        return get(Restrictions.or(Restrictions.eq("name", nameOrAlias), Restrictions.eq("alias", nameOrAlias)));
    }

    @Transactional
    public HonorTitle getLowestRankingHonorTitle() {
        return get(Order.asc("lowerBound"));
    }

    @Transactional
    public Collection<HonorTitle> getAllHonorTitles() {
        return find();
    }

    @Transactional
    @Override
    public HonorTitle save(final HonorTitle title) {
        try {
            for (Bonus bonus : title.getBonuses()) {
                getSession().saveOrUpdate(bonus);
            }
            return super.save(title);
        } catch (HibernateException e) {
            throw new DBException(e);
        }
    }

    @Transactional
    @Override
    public void delete(final HonorTitle title) {
        title.clear();
        super.delete(title);
    }
}
