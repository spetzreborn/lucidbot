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

package commands.news;

import database.models.AttackType;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class TypeToDualActivities implements Comparable<TypeToDualActivities> {
    private final AttackType attackType;
    private final AttackingActivities selfActivities;
    private final AttackingActivities enemyActivities;

    public TypeToDualActivities(final AttackType attackType, final AttackingActivities selfActivities,
                                final AttackingActivities enemyActivities) {
        this.attackType = attackType;
        this.selfActivities = selfActivities;
        this.enemyActivities = enemyActivities;
    }

    public AttackType getAttackType() {
        return attackType;
    }

    public AttackingActivities getSelfActivities() {
        return selfActivities;
    }

    public AttackingActivities getEnemyActivities() {
        return enemyActivities;
    }

    @Override
    public int compareTo(final TypeToDualActivities o) {
        return Integer.compare(getSelfActivities().getTotalHits() + getEnemyActivities().getTotalHits(),
                o.getSelfActivities().getTotalHits() + getEnemyActivities().getTotalHits());
    }
}
