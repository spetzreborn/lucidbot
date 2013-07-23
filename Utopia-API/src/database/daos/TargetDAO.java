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
import database.models.Province;
import database.models.Target;
import database.models.Target.TargetType;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.*;

import static api.tools.time.DateUtil.isAfter;

public class TargetDAO extends AbstractDAO<Target> {
    @Inject
    public TargetDAO(final Provider<Session> sessionProvider) {
        super(Target.class, sessionProvider);
    }

    @Transactional
    public Target getTarget(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public Target saveOrOverwrite(Target target, Collection<TargetType> types) {
        Target alreadyExisting = get(Restrictions.eq("province", target.getProvince()), Restrictions.in("type", types));
        if (alreadyExisting == null) return save(target);
        else {
            alreadyExisting.setDetails(target.getDetails());
            alreadyExisting.setAdded(new Date());
            alreadyExisting.setBindings(target.getBindings());
            alreadyExisting.clear();
            return save(alreadyExisting);
        }
    }

    @Transactional
    public Target getTarget(Province province, TargetType type) {
        return get(Restrictions.eq("province", province), Restrictions.eq("type", type));
    }

    @Transactional
    public List<Target> getAllTargets() {
        return find();
    }

    @Transactional
    public List<Target> getTargetsOfType(TargetType first, TargetType... more) {
        return find(Restrictions.in("type", EnumSet.of(first, more)));
    }

    @Transactional
    public List<Target> getTargetsForUser(final BotUser user, final BindingsManager bindingsManager) {
        List<Target> builds = new ArrayList<>();
        for (Target target : getTargetsOfType(TargetType.GENERATED_TARGET, TargetType.MANUAL_TARGET)) {
            if (bindingsManager.matchesBindings(target.getBindings(), user) || target.containsUserAsHitter(user)) builds.add(target);
        }
        return builds;
    }

    @Transactional
    public int countTargetForUserAddedAfter(final Date date,
                                            final BotUser botUser,
                                            final BindingsManager bindingsManager) {
        int counter = 0;
        for (Target target : getTargetsForUser(botUser, bindingsManager)) {
            if (isAfter(target.getAdded(), date)) ++counter;
        }
        return counter;
    }

    @Transactional
    @Override
    public void delete(final Target target) {
        target.clear();
        super.delete(target);
    }
}
