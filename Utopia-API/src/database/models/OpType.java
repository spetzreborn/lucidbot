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

import api.common.HasName;
import api.common.HasNumericId;
import lombok.*;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "op_type")
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
@Getter
@Setter
public class OpType implements HasName, Comparable<OpType>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "short_name", length = 100)
    private String shortName;

    @Lob
    @Column(name = "effects", length = 1000)
    private String effects;

    @Lob
    @Column(name = "op_regex", length = 2000)
    private String opRegex;

    @Lob
    @Column(name = "news_regex", length = 2000)
    private String newsRegex;

    @Column(name = "op_character", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private SpellOpCharacter opCharacter;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pk.opType", orphanRemoval = true)
    private Set<OpTypeBonus> bonuses = new HashSet<>();

    public OpType(final String name, final String effects) {
        this.name = name;
        this.effects = effects;
    }

    public OpType(final String name, final String shortName, final String effects, final String opRegex, final String newsRegex,
                  final SpellOpCharacter opCharacter, final Set<Bonus> bonuses) {
        this.name = name;
        this.shortName = shortName;
        this.effects = effects;
        this.opRegex = opRegex;
        this.newsRegex = newsRegex;
        this.opCharacter = opCharacter;
        for (Bonus bonus : bonuses) {
            this.bonuses.add(new OpTypeBonus(this, bonus));
        }
    }

    public void clear() {
        bonuses.clear();
    }

    /**
     * Gets the bonus of the specified type and applicability
     *
     * @param type          the type of bonus, may not be null
     * @param applicability the applicability rule. May be null, but usually shouldn't be
     * @return a Bonus matching the specified criteria. Never returns null, so if the specified Bonus
     *         doesn't actually exist, and empty Bonus is returned (meaning the actual bonus of it is 0, so using
     *         it in calculations will just return the value you send in)
     */
    public Bonus getBonus(final BonusType type, final BonusApplicability applicability) {
        for (Bonus bonus : getBonuses()) {
            if (bonus.getType() == type && bonus.getApplicability().is(applicability)) return bonus;
        }
        return Bonus.noBonus(type, applicability);
    }

    /**
     * Gets the bonus of the specified type
     *
     * @param type the type of bonus, may not be null
     * @return a Bonus matching the specified criteria. Never returns null, so if the specified Bonus
     *         doesn't actually exist, and empty Bonus is returned (meaning the actual bonus of it is 0, so using
     *         it in calculations will just return the value you send in)
     */
    public Bonus getBonus(final BonusType type) {
        return getBonus(type, null);
    }

    public List<Bonus> getBonuses() {
        List<Bonus> out = new ArrayList<>(bonuses.size());
        for (OpTypeBonus join : bonuses) {
            out.add(join.getBonus());
        }
        return out;
    }

    public void setBonuses(final Collection<Bonus> bonuses) {
        this.bonuses.clear();
        for (Bonus bonus : bonuses) {
            this.bonuses.add(new OpTypeBonus(this, bonus));
        }
    }

    @Override
    public int compareTo(final OpType o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getName().compareToIgnoreCase(o.getName());
    }
}
