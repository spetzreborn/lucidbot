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
import com.google.inject.Provider;
import database.models.DragonProject;
import database.models.DragonProjectType;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.List;

import static api.tools.collections.CollectionUtil.isEmpty;

@ParametersAreNonnullByDefault
public class DragonProjectDAO extends AbstractDAO<DragonProject> {
    @Inject
    public DragonProjectDAO(final Provider<Session> sessionProvider) {
        super(DragonProject.class, sessionProvider);
    }

    @Transactional
    public DragonProject getProject(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public List<DragonProject> getAllProjects() {
        return find();
    }

    @Transactional
    public DragonProject getProjectOfType(final DragonProjectType type) {
        List<DragonProject> dragonProjects = find(Restrictions.eq("type", type), Order.desc("created"));
        return isEmpty(dragonProjects) ? null : dragonProjects.get(0);
    }
}
