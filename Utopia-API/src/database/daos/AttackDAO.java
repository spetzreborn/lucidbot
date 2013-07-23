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
import com.google.inject.Provider;
import database.models.Attack;
import database.models.AttackType;
import database.models.Province;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;


public class AttackDAO extends AbstractDAO<Attack> {
    @Inject
    public AttackDAO(final Provider<Session> sessionProvider) {
        super(Attack.class, sessionProvider);
    }

    @Transactional
    public List<Attack> getLastHitsMade(final Province province, final int maxAmount) {
        if (maxAmount < 1) return Collections.emptyList();
        return find(Restrictions.eq("attacker", province), Restrictions.ne("type", AttackType.AMBUSH), Order.desc("timeOfAttack"),
                maxAmount);
    }

    @Transactional
    public Attack getLastHitMade(final Province province) {
        List<Attack> lastHitsMade = getLastHitsMade(province, 1);
        return lastHitsMade.isEmpty() ? null : lastHitsMade.get(0);
    }

    @Transactional
    public Attack getLastHitMadeByUser(final BotUser user) {
        NestedCriterion nc1 = new NestedCriterion("provinceOwner", new Object[]{Restrictions.eq("mainNick", user.getMainNick())});
        NestedCriterion nc2 = new NestedCriterion("attacker", new Object[]{nc1});
        List<Attack> attacks = find(nc2, Order.desc("timeOfAttack"), 1);
        return attacks.isEmpty() ? null : attacks.get(0);
    }

    @Transactional
    public List<Attack> getLastHitsReceived(final Province province, final int maxAmount) {
        if (maxAmount < 1) return Collections.emptyList();
        return find(Restrictions.eq("target", province), Restrictions.ne("type", AttackType.AMBUSH), Order.desc("timeOfAttack"), maxAmount);
    }

    @Transactional
    public Attack getLastHitReceived(final Province province) {
        List<Attack> lastHitsReceived = getLastHitsReceived(province, 1);
        return lastHitsReceived.isEmpty() ? null : lastHitsReceived.get(0);
    }

    @Transactional
    public Attack getAttack(final long id) {
        return get(Restrictions.idEq(id));
    }

    @Transactional
    public List<Attack> getAttacks(final Long... ids) {
        return find(Restrictions.in("id", ids));
    }

    @Transactional
    public List<Attack> getAllAttacks() {
        return find();
    }
}
