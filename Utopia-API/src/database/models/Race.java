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
@Table(name = "race")
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
@Getter
@Setter
public class Race implements HasName, Comparable<Race>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", updatable = false, nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "short_name", nullable = false, unique = true, length = 2)
    private String shortName;

    @Column(name = "soldier_strength", nullable = false)
    private int soldierStrength;

    @Column(name = "soldier_networth", nullable = false)
    private double soldierNetworth;

    @Column(name = "off_spec_name", nullable = false, length = 100)
    private String offSpecName;

    @Column(name = "off_spec_strength", nullable = false)
    private int offSpecStrength;

    @Column(name = "def_spec_name", nullable = false, length = 100)
    private String defSpecName;

    @Column(name = "def_spec_strength", nullable = false)
    private int defSpecStrength;

    @Column(name = "elite_name", nullable = false, length = 100)
    private String eliteName;

    @Column(name = "elite_off_strength", nullable = false)
    private int eliteOffStrength;

    @Column(name = "elite_def_strength", nullable = false)
    private int eliteDefStrength;

    @Column(name = "elite_networth", nullable = false)
    private double eliteNetworth;

    @Column(name = "elite_sendout_percentage", nullable = false)
    private int eliteSendoutPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "intel_accuracy", nullable = false, length = 100)
    private IntelAccuracySpecification intelAccuracySpecification = IntelAccuracySpecification.NEVER;

    @Column(name = "dragon_immune", nullable = false)
    private boolean dragonImmune;

    @Lob
    @Column(name = "pros", length = 5000)
    private String pros;

    @Lob
    @Column(name = "cons", length = 5000)
    private String cons;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pk.race", orphanRemoval = true)
    private Set<RaceBonus> bonuses = new HashSet<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pk.race", orphanRemoval = true)
    private Set<RaceSpellType> spellBook = new HashSet<>();

    public Race(final String name, final String shortName, final String offSpecName, final String defSpecName, final String eliteName) {
        this.name = name;
        this.shortName = shortName;
        this.offSpecName = offSpecName;
        this.defSpecName = defSpecName;
        this.eliteName = eliteName;
    }

    public Race(final String name, final String shortName, final int soldierStrength, final double soldierNetworth,
                final String offSpecName, final int offSpecStrength, final String defSpecName, final int defSpecStrength,
                final String eliteName, final int eliteOffStrength, final int eliteDefStrength, final double eliteNetworth,
                final int eliteSendoutPercentage, final IntelAccuracySpecification intelAccuracySpecification, final boolean isDragonImmune,
                final String pros, final String cons, final Set<Bonus> bonuses, final List<SpellType> spellbook) {
        this.name = name;
        this.shortName = shortName;
        this.soldierStrength = soldierStrength;
        this.soldierNetworth = soldierNetworth;
        this.offSpecName = offSpecName;
        this.offSpecStrength = offSpecStrength;
        this.defSpecName = defSpecName;
        this.defSpecStrength = defSpecStrength;
        this.eliteName = eliteName;
        this.eliteOffStrength = eliteOffStrength;
        this.eliteDefStrength = eliteDefStrength;
        this.eliteNetworth = eliteNetworth;
        this.eliteSendoutPercentage = eliteSendoutPercentage;
        this.intelAccuracySpecification = intelAccuracySpecification;
        this.dragonImmune = isDragonImmune;
        this.pros = pros;
        this.cons = cons;
        for (Bonus bonus : bonuses) {
            this.bonuses.add(new RaceBonus(this, bonus));
        }
        for (SpellType spellType : spellbook) {
            this.spellBook.add(new RaceSpellType(this, spellType));
        }
    }

    public void clear() {
        bonuses.clear();
        spellBook.clear();
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
        for (RaceBonus join : bonuses) {
            Bonus bonus = join.getBonus();
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
        for (RaceBonus join : bonuses) {
            out.add(join.getBonus());
        }
        return out;
    }

    public List<SpellType> getSpellbook() {
        List<SpellType> out = new ArrayList<>(spellBook.size());
        for (RaceSpellType join : spellBook) {
            out.add(join.getSpellType());
        }
        return out;
    }

    public void setBonuses(final Collection<Bonus> bonuses) {
        this.bonuses.clear();
        for (Bonus bonus : bonuses) {
            this.bonuses.add(new RaceBonus(this, bonus));
        }
    }

    public void setSpellbook(final Collection<SpellType> spells) {
        this.spellBook.clear();
        for (SpellType spell : spells) {
            this.spellBook.add(new RaceSpellType(this, spell));
        }
    }

    @Override
    public int compareTo(final Race o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getName().compareToIgnoreCase(o.getName());
    }
}
