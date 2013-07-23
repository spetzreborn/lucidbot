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
import api.database.models.BotUser;
import api.filters.FilterEnabled;
import api.filters.SortEnabled;
import filtering.filters.*;
import intel.IntelSourceProvider;
import intel.ProvinceResourceProvider;
import intel.ProvinceResourceType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.*;

import static api.tools.text.StringUtil.lowerCase;

@Entity
@Table(name = "province")
@NoArgsConstructor
@Getter
@Setter
public class Province implements Comparable<Province>, HasName, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "kingdom_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Kingdom kingdom;

    @ManyToOne
    @JoinColumn(name = "race_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Race race;

    @ManyToOne
    @JoinColumn(name = "personality_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Personality personality;

    @ManyToOne
    @JoinColumn(name = "honor_title_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HonorTitle honorTitle;

    @Column(name = "land", nullable = false)
    private int land;

    @Column(name = "networth", nullable = false)
    private int networth;

    @Column(name = "basic_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastUpdated = new Date();

    @Column(name = "wizards", nullable = false)
    private int wizards;

    @Column(name = "wizards_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date wizardsLastUpdated;

    @Column(name = "mana", nullable = false)
    private int mana = 100;

    @Column(name = "thieves", nullable = false)
    private int thieves;

    @Column(name = "thieves_last_updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date thievesLastUpdated;

    @Column(name = "stealth", nullable = false)
    private int stealth = 100;

    @Column(name = "generals", nullable = false)
    private int generals = 4;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    private BotUser provinceOwner;

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Aid> aid = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "province", optional = true)
    private SoT sot;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "province", optional = true)
    private SoM som;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "province", optional = true)
    private SoS sos;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "province", optional = true)
    private Survey survey;

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DurationSpell> durationSpells = new HashSet<>();

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InstantSpell> instantSpells = new HashSet<>();

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DurationOp> durationOps = new HashSet<>();

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InstantOp> instantOps = new HashSet<>();

    @OneToMany(mappedBy = "province")
    private Set<Army> armies = new HashSet<>();

    public Province(final String name, final Kingdom kingdom) {
        this.name = name;
        this.kingdom = kingdom;
        this.lastUpdated = new Date();
    }

    public Province(final String name,
                    final Kingdom kingdom,
                    final Race race,
                    final Personality personality,
                    final BotUser provinceOwner) {
        this.name = name;
        this.kingdom = kingdom;
        this.race = race;
        this.personality = personality;
        this.provinceOwner = provinceOwner;
        this.lastUpdated = new Date();
    }

    public void clearForRemoval() {
        aid.clear();
        durationSpells.clear();
        durationOps.clear();
        instantOps.clear();
        instantSpells.clear();
        sot = null;
        som = null;
        sos = null;
        survey = null;
    }

    @FilterEnabled(KingdomLocationFilter.class)
    public String getKingdomLocation() {
        return kingdom.getLocation();
    }

    @Override
    @SortEnabled("name")
    public String getName() {
        return name;
    }

    @FilterEnabled(RaceFilter.class)
    public Race getRace() {
        return race;
    }

    @FilterEnabled(PersonalityFilter.class)
    public Personality getPersonality() {
        return personality;
    }

    @SortEnabled("honor")
    public HonorTitle getHonorTitle() {
        return honorTitle;
    }

    @FilterEnabled(LandFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.LAND)
    @SortEnabled("land|acres")
    public int getLand() {
        return land;
    }

    @FilterEnabled(NetworthFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.NETWORTH)
    @SortEnabled("nw|networth")
    public int getNetworth() {
        return networth;
    }

    public int getNwpa() {
        return land == 0 ? 0 : networth / land;
    }

    @ProvinceResourceProvider(value = ProvinceResourceType.WIZARDS, lastUpdatedMethod = "getWizardsLastUpdated")
    @SortEnabled("wizards")
    public int getWizards() {
        return wizards;
    }

    @ProvinceResourceProvider(value = ProvinceResourceType.WIZARDS_PER_ACRE, lastUpdatedMethod = "getWizardsLastUpdated")
    public double getWizardsPerAcre() {
        return getLand() == 0 ? 0 : getWizards() * 1.0 / getLand();
    }

    @FilterEnabled(WpaFilter.class)
    @ProvinceResourceProvider(value = ProvinceResourceType.MOD_WIZARDS_PER_ACRE, lastUpdatedMethod = "getWizardsLastUpdated")
    @SortEnabled("wpa")
    public double getModWizardsPerAcre() {
        double base = getWizardsPerAcre();
        if (base == 0) return 0;

        return applyMultiplicativeBonuses(base, BonusType.WPA, BonusApplicability.OFFENSIVELY);
    }

    @ProvinceResourceProvider(value = ProvinceResourceType.THIEVES, lastUpdatedMethod = "getThievesLastUpdated")
    @SortEnabled("thieves")
    public int getThieves() {
        return thieves;
    }

    @ProvinceResourceProvider(value = ProvinceResourceType.THIEVES_PER_ACRE, lastUpdatedMethod = "getThievesLastUpdated")
    public double getThievesPerAcre() {
        return getLand() == 0 ? 0 : getThieves() * 1.0 / getLand();
    }

    @FilterEnabled(TpaFilter.class)
    @ProvinceResourceProvider(value = ProvinceResourceType.MOD_THIEVES_PER_ACRE, lastUpdatedMethod = "getThievesLastUpdated")
    @SortEnabled("tpa")
    public double getModThievesPerAcre() {
        double base = getThievesPerAcre();
        if (base == 0) return 0;

        return applyMultiplicativeBonuses(base, BonusType.TPA, BonusApplicability.OFFENSIVELY);
    }

    @ProvinceResourceProvider(ProvinceResourceType.MANA)
    public int getMana() {
        return mana;
    }

    @ProvinceResourceProvider(ProvinceResourceType.STEALTH)
    public int getStealth() {
        return stealth;
    }

    public void addAid(final Aid aid) {
        this.getAid().add(aid);
    }

    public void removeAid(final Aid aid) {
        this.getAid().remove(aid);
    }

    @IntelSourceProvider(SoT.class)
    public SoT getSot() {
        return sot;
    }

    @FilterEnabled(PeasantsFilter.class)
    @SortEnabled("peasants")
    public Integer getPeasants() {
        return getSot() == null ? null : getSot().getPeasants();
    }

    @FilterEnabled(SoldiersFilter.class)
    @SortEnabled("soldiers|solds")
    public Integer getSoldiers() {
        return getSot() == null ? null : getSot().getSoldiers();
    }

    @FilterEnabled(HorsesFilter.class)
    @SortEnabled("horses|war horses")
    public Integer getWarHorses() {
        return getSot() == null ? null : getSot().getWarHorses();
    }

    @FilterEnabled(OffenseFilter.class)
    @SortEnabled("mo|mod off")
    public Integer getModOffense() {
        return getSot() == null ? null : getSot().getModOffense();
    }

    @FilterEnabled(DefenseFilter.class)
    @SortEnabled("md|mod def")
    public Integer getModDefense() {
        return getSot() == null ? null : getSot().getModDefense();
    }

    @FilterEnabled(MoneyFilter.class)
    @SortEnabled("money|gc|gcs")
    public Integer getMoney() {
        return getSot() == null ? null : getSot().getMoney();
    }

    @FilterEnabled(FoodFilter.class)
    @SortEnabled("food|bushels")
    public Integer getFood() {
        return getSot() == null ? null : getSot().getFood();
    }

    @FilterEnabled(RunesFilter.class)
    @SortEnabled("runes")
    public Integer getRunes() {
        return getSot() == null ? null : getSot().getRunes();
    }

    @FilterEnabled(AgeFilter.class)
    @SortEnabled("age|old|hours")
    public Date getLastUpdated() {
        return new Date(lastUpdated.getTime());
    }

    @IntelSourceProvider(SoM.class)
    public SoM getSom() {
        return som;
    }

    @IntelSourceProvider(SoS.class)
    public SoS getSos() {
        return sos;
    }

    @FilterEnabled(ScienceBooksFilter.class)
    @SortEnabled("sci|science|books")
    public Integer getScience() {
        return getSos() == null ? null : getSos().getTotalBooks();
    }

    @FilterEnabled(BpaFilter.class)
    @SortEnabled("bpa")
    public Integer getBpa() {
        return getSos() == null ? null : getSos().getBooksPerAcre();
    }

    @IntelSourceProvider(Survey.class)
    public Survey getSurvey() {
        return survey;
    }

    @FilterEnabled(BuildingFilter.class)
    public Double getBuildingPercentage(final String building) {
        return getSurvey() == null ? null : getSurvey().getBuildingPercentage(building);
    }

    public double applyMultiplicativeBonuses(final double base,
                                             final BonusType bonusType,
                                             final BonusApplicability applicability) {
        double out = base;

        Bonus raceBonus = getRace() == null ? null : getRace().getBonus(bonusType, applicability);
        if (raceBonus != null) out = raceBonus.applyTo(out);

        Bonus persBonus = getPersonality() == null ? null : getPersonality().getBonus(bonusType, applicability);
        if (persBonus != null) out = persBonus.applyTo(out);

        if (getSurvey() != null) {
            out = getSurvey().applyAnyBonuses(bonusType, applicability, out);
        }

        if (getSos() != null) {
            out = getSos().applyAnyBonuses(bonusType, applicability, out);
        }

        Bonus honorBonus = getHonorTitle() == null ? null : getHonorTitle().getBonus(bonusType, applicability);
        if (honorBonus != null) {
            double honorBonusModifiers = 1;
            if (getRace() != null)
                honorBonusModifiers = getRace().getBonus(BonusType.HONOR, applicability).applyTo(honorBonusModifiers);
            if (getPersonality() != null)
                honorBonusModifiers = getPersonality().getBonus(BonusType.HONOR, applicability).applyTo(honorBonusModifiers);
            out = honorBonus.applyTo(out, honorBonusModifiers);
        }

        if (getRace() != null) {
            for (SpellType spellType : race.getSpellbook()) {
                Bonus bonus = spellType.getBonus(bonusType, applicability);
                if (bonus != null) out = bonus.applyTo(out);
            }
        }
        return out;
    }

    public DurationSpell getDurationSpell(final SpellType spellType) {
        for (DurationSpell spell : durationSpells) {
            if (spell.getType().equals(spellType)) return spell;
        }
        return null;
    }

    public DurationSpell addDurationSpell(final DurationSpell durationSpell) {
        DurationSpell existing = null;
        for (DurationSpell spell : durationSpells) {
            if (spell.getType().equals(durationSpell.getType())) {
                existing = spell;
                break;
            }
        }

        if (existing == null) durationSpells.add(durationSpell);
        else {
            existing.setCommitter(durationSpell.getCommitter());
            existing.setExpires(durationSpell.getExpires());
        }
        return existing == null ? durationSpell : existing;
    }

    public void removeDurationSpell(final SpellType spellType) {
        for (Iterator<DurationSpell> iter = durationSpells.iterator(); iter.hasNext(); ) {
            DurationSpell spell = iter.next();
            if (spell.getType().equals(spellType)) {
                iter.remove();
            }
        }
    }

    public void removeDurationSpells() {
        durationSpells.clear();
    }

    public Set<InstantSpell> getInstantSpells(final SpellType spellType) {
        Set<InstantSpell> out = new HashSet<>();
        for (InstantSpell spell : instantSpells) {
            if (spell.getType().equals(spellType)) out.add(spell);
        }
        return out;
    }

    public Set<InstantSpell> getInstantSpells() {
        return instantSpells;
    }

    public InstantSpell registerInstantSpell(final BotUser caster, final SpellType spellType, final int damage) {
        InstantSpell instantSpell = null;
        for (InstantSpell spell : instantSpells) {
            if (spell.getType().equals(spellType) && spell.getCommitter().equals(caster)) {
                instantSpell = spell;
                instantSpell.addCast(damage);
                break;
            }
        }
        if (instantSpell == null) {
            instantSpell = new InstantSpell(caster, this, damage, spellType);
            instantSpells.add(instantSpell);
        }
        return instantSpell;
    }

    public void clearInstantSpells(final SpellType spellType) {
        for (Iterator<InstantSpell> iter = instantSpells.iterator(); iter.hasNext(); ) {
            InstantSpell spell = iter.next();
            if (spell.getType().equals(spellType)) {
                iter.remove();
            }
        }
    }

    public void clearInstantSpells() {
        instantSpells.clear();
    }

    public DurationOp getDurationOp(final OpType opType) {
        for (DurationOp op : durationOps) {
            if (op.getType().equals(opType)) return op;
        }
        return null;
    }

    public DurationOp addDurationOp(final DurationOp durationOp) {
        DurationOp existing = null;
        for (DurationOp op : durationOps) {
            if (op.getType().equals(durationOp.getType())) {
                existing = op;
                break;
            }
        }

        if (existing == null) durationOps.add(durationOp);
        else {
            existing.setCommitter(durationOp.getCommitter());
            existing.setExpires(durationOp.getExpires());
        }
        return existing == null ? durationOp : existing;
    }

    public void removeDurationOp(final OpType opType) {
        for (Iterator<DurationOp> iter = durationOps.iterator(); iter.hasNext(); ) {
            DurationOp op = iter.next();
            if (op.getType().equals(opType)) {
                iter.remove();
                break;
            }
        }
    }

    public void removeDurationOps() {
        durationOps.clear();
    }

    public Set<InstantOp> getInstantOps(final OpType opType) {
        Set<InstantOp> out = new HashSet<>();
        for (InstantOp op : instantOps) {
            if (op.getType().equals(opType)) out.add(op);
        }
        return out;
    }

    public Set<InstantOp> getInstantOps() {
        return instantOps;
    }

    public InstantOp registerInstantOp(final BotUser committer, final OpType opType, final int damage) {
        InstantOp instantOp = null;
        for (InstantOp op : instantOps) {
            if (op.getType().equals(opType) && op.getCommitter().equals(committer)) {
                instantOp = op;
                instantOp.addCommit(damage);
                break;
            }
        }
        if (instantOp == null) {
            instantOp = new InstantOp(committer, this, damage, opType);
            instantOps.add(instantOp);
        }
        return instantOp;
    }

    public void clearInstantOps(final OpType opType) {
        for (Iterator<InstantOp> iter = instantOps.iterator(); iter.hasNext(); ) {
            InstantOp op = iter.next();
            if (op.getType().equals(opType)) {
                iter.remove();
            }
        }
    }

    public void clearInstantOps() {
        instantOps.clear();
    }

    @FilterEnabled(PmdFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.PRACTICAL_MOD_DEF)
    @SortEnabled("pmd")
    public Integer getPmd() {
        return getSot() == null ? null : getSot().getPmd();
    }

    @ProvinceResourceProvider(ProvinceResourceType.PRACTICAL_MOD_DEF_PER_ACRE)
    @SortEnabled("pmda")
    public Integer getPracticalModDefPerAcre() {
        Integer val = getPmd();
        return val == null || getLand() == 0 ? null : val / getLand();
    }

    @FilterEnabled(PmoFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.PRACTICAL_MOD_OFF)
    @SortEnabled("pmo")
    public Integer getPmo() {
        return getSot() == null ? null : getSot().getPmo();
    }

    @ProvinceResourceProvider(ProvinceResourceType.PRACTICAL_MOD_OFF_PER_ACRE)
    @SortEnabled("pmoa")
    public Integer getPracticalModOffPerAcre() {
        Integer val = getPmo();
        return val == null || getLand() == 0 ? null : val / getLand();
    }

    @ProvinceResourceProvider(ProvinceResourceType.ESTIMATED_CURRENT_DEFENSE)
    @FilterEnabled(CurrentDefenseFilter.class)
    public Integer getEstimatedCurrentDefense() {
        if (getSot() == null) return null;
        else if (getSom() == null) return getSot().getModDefense();
        else {
            int soldiersOut = 0;
            int elitesOut = 0;
            for (Army army : getSom().getArmiesOut()) {
                soldiersOut += army.getSoldiers();
                elitesOut += army.getElites();
            }
            return getSot().getModDefense() -
                    (int) ((soldiersOut * getRace().getSoldierStrength() + elitesOut * getRace().getEliteDefStrength() +
                            getSot().getRawDefensiveBonuses()) * getSot().getDefensiveME());
        }
    }

    @ProvinceResourceProvider(ProvinceResourceType.ESTIMATED_CURRENT_OFFENSE)
    @FilterEnabled(CurrentOffenseFilter.class)
    public Integer getEstimatedCurrentOffense() {
        if (getSot() == null) return null;
        else if (getSom() == null) return getPmo();
        else {
            int elitesOut = 0;
            for (Army army : getSom().getArmiesOut()) {
                elitesOut += army.getElites();
            }
            int maxElitesOut = (int) (getSot().getElites() * getRace().getEliteSendoutPercentage() * 1.0 / 100.0);
            int elitesHome = elitesOut > maxElitesOut ? 0 : maxElitesOut - elitesOut;
            return getSot().maxOffense(elitesHome);
        }
    }

    public int getGeneralsHome() {
        int gensOut = 0;

        boolean useSom = getSom() != null && !getSom().getArmiesOut().isEmpty();

        for (Army army : armies) {
            if (useSom && army.getType() == Army.ArmyType.ARMY_OUT) gensOut += army.getGenerals();
            else if (!useSom && army.getType() == Army.ArmyType.IRC_ARMY_OUT) gensOut += army.getGenerals();
        }
        return Math.max(0, getGenerals() - gensOut);
    }

    public void setOwner(final BotUser user) {
        this.provinceOwner = user;
    }

    @Override
    public int compareTo(final Province o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        if (getProvinceOwner() != null && o.getProvinceOwner() != null)
            return getProvinceOwner().compareTo(o.getProvinceOwner());
        int kdComp = getKingdom().compareTo(o.getKingdom());
        return kdComp == 0 ? getName().compareToIgnoreCase(o.getName()) : kdComp;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Province)) return false;

        Province other = (Province) obj;
        String provinceName = getName();
        if (getId() == null && provinceName == null) return false;
        return provinceName == null ? getId().equals(other.getId()) : provinceName.equalsIgnoreCase(other.getName());
    }

    @Override
    public int hashCode() {
        String provinceName = getName();
        return provinceName == null ? System.identityHashCode(this) : lowerCase(provinceName).hashCode();
    }
}
