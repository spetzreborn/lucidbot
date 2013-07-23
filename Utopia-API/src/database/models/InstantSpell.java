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
import api.database.models.BotUser;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "instant_spell", uniqueConstraints = @UniqueConstraint(columnNames = {"bot_user_id", "province_id", "spell_type_id"}))
@NoArgsConstructor
@EqualsAndHashCode(of = {"committer", "province", "type"})
@Getter
@Setter
public class InstantSpell implements Comparable<InstantSpell>, SpellsOpsDamageInfo, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_user_id", updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BotUser committer;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province province;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "damage", nullable = false)
    private int damage;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "spell_type_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SpellType type;

    public InstantSpell(final BotUser caster, final Province province, final int damage, final SpellType spellType) {
        this(caster, province, damage, 1, spellType);
    }

    public InstantSpell(final BotUser caster, final Province province, final int damage, final int amount, final SpellType spellType) {
        this.committer = caster;
        this.province = province;
        this.amount = amount;
        this.damage = damage;
        this.type = spellType;
    }

    public void addCast(final int damage) {
        ++amount;
        this.damage += damage;
    }

    @Override
    public int compareTo(final InstantSpell o) {
        int provCompare = getProvince().compareTo(o.getProvince());
        if (provCompare != 0) return provCompare;
        return getType().compareTo(o.getType());
    }
}
