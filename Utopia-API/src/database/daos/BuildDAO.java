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
import api.database.models.BotUser;
import com.google.inject.Provider;
import database.models.Build;
import database.models.Personality;
import database.models.Race;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static api.tools.text.StringUtil.isNullOrEmpty;
import static api.tools.time.DateUtil.isAfter;

public class BuildDAO extends AbstractDAO<Build> {
    @Inject
    public BuildDAO(final Provider<Session> sessionProvider) {
        super(Build.class, sessionProvider);
    }

    @Transactional
    public Build getBuild(long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public List<Build> getBuilds(Race race, Personality personality, String type) {
        List<Build> builds = isNullOrEmpty(type) ? getAllBuilds() : find(Restrictions.eq("type", type));
        List<Build> out = new ArrayList<>();
        for (Build build : builds) {
            if ((race == null || build.getBindings().getRaces().contains(race)) &&
                    (personality == null || build.getBindings().getPersonalities().contains(personality))) {
                out.add(build);
            }
        }
        return out;
    }

    @Transactional
    public List<Build> getBuildsForUser(final BotUser user, final BindingsManager bindingsManager) {
        List<Build> builds = new ArrayList<>();
        for (Build build : find()) {
            if (bindingsManager.matchesBindings(build.getBindings(), user)) builds.add(build);
        }
        return builds;
    }

    @Transactional
    public List<Build> getAllBuilds() {
        return find();
    }

    @Transactional
    public List<Build> getBuilds(final Long... ids) {
        return find(Restrictions.in("id", ids));
    }

    @Transactional
    public List<Build> getBuilds(final String type) {
        return find(Restrictions.eq("type", type));
    }

    @Transactional
    public int countBuildsForUserAddedAfter(final Date date, final BotUser botUser, final BindingsManager bindingsManager) {
        int counter = 0;
        for (Build build : getBuildsForUser(botUser, bindingsManager)) {
            if (isAfter(build.getAdded(), date)) ++counter;
        }
        return counter;
    }
}
