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

package database.models;

import api.common.HasNumericId;
import api.filters.FilterEnabled;
import filtering.filters.AgeFilter;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "attack")
@NoArgsConstructor
@EqualsAndHashCode(of = {"attacker", "timeOfAttack"})
@Getter
@Setter
public class Attack implements Comparable<Attack>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "attacker_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province attacker;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "target_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province target;

    @Column(name = "gain", nullable = false, length = 100)
    private String gain;

    @Column(name = "kills", nullable = false)
    private int kills;

    @Column(name = "offense_sent", nullable = false)
    private int offenseSent;

    @Column(name = "type", updatable = false, nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private AttackType type;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "time_of_attack", updatable = false, nullable = false)
    private Date timeOfAttack;

    @Column(name = "got_plagued", nullable = false)
    private boolean gotPlagued;

    @Column(name = "spread_plague", nullable = false)
    private boolean spreadPlague;

    public Attack(final Province attacker, final Province target, final AttackType type, final Date timeOfAttack) {
        this.attacker = attacker;
        this.target = target;
        this.type = type;
        this.timeOfAttack = timeOfAttack;
    }

    public Attack(final Province attacker, final Province target, final String gain, final int kills, final int offenseSent,
                  final AttackType type, final Date timeOfAttack) {
        this(attacker, target, type, timeOfAttack);
        this.gain = gain;
        this.kills = kills;
        this.offenseSent = offenseSent;
    }

    public Attack(final Province attacker, final Province target, final String gain, final AttackType type) {
        this(attacker, target, gain, 0, 0, type, new Date());
    }

    @FilterEnabled(AgeFilter.class)
    public Date getTimeOfAttack() {
        return new Date(timeOfAttack.getTime());
    }

    @Override
    public int compareTo(final Attack o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getTimeOfAttack().compareTo(o.getTimeOfAttack());
    }
}
