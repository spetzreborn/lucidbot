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
import events.SoTSavedEvent;
import filtering.filters.*;
import intel.ProvinceIntel;
import intel.ProvinceResourceProvider;
import intel.ProvinceResourceType;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "sot")
@NoArgsConstructor
@EqualsAndHashCode(of = "province")
@Getter
@Setter
public class SoT implements ProvinceIntel, Comparable<SoT>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province province;

    @Column(name = "peasants", nullable = false)
    private int peasants;

    @Column(name = "soldiers", nullable = false)
    private int soldiers;

    @Column(name = "off_specs", nullable = false)
    private int offSpecs;

    @Column(name = "def_specs", nullable = false)
    private int defSpecs;

    @Column(name = "elites", nullable = false)
    private int elites;

    @Column(name = "prisoners", nullable = false)
    private int prisoners;

    @Column(name = "war_horses", nullable = false)
    private int warHorses;

    @Column(name = "mod_offense", nullable = false)
    private int modOffense;

    @Column(name = "mod_defense", nullable = false)
    private int modDefense;

    @Column(name = "money", nullable = false)
    private int money;

    @Column(name = "food", nullable = false)
    private int food;

    @Column(name = "runes", nullable = false)
    private int runes;

    @Column(name = "building_efficiency", nullable = false)
    private int buildingEfficiency;

    @Column(name = "trade_balance", nullable = false)
    private int tradeBalance;

    @Column(name = "plagued", nullable = false)
    private boolean plagued;

    @Column(name = "hit_status", nullable = false, length = 200)
    private String hitStatus = "";

    @Column(name = "overpopulated", nullable = false)
    private boolean overpopulated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;

    @Column(name = "export_line", length = 200)
    private String exportLine;

    @Column(name = "saved_by", nullable = false, length = 200)
    private String savedBy;

    @Column(name = "is_angel_intel", nullable = false)
    private boolean isAngelIntel;

    @Column(name = "accuracy", nullable = false)
    private int accuracy = 100;

    public SoT(final Province province, final int peasants, final int soldiers, final int offSpecs, final int defSpecs, final int elites,
               final int prisoners, final int warHorses, final int money, final int food, final int runes, final int buildingEfficiency,
               final int tradeBalance, final boolean hasPlague, final String hitStatus, final boolean isOverpopulated,
               final Date lastUpdated, final String exportLine, final String savedBy, final int accuracy) {
        this.province = province;
        this.peasants = peasants;
        this.soldiers = soldiers;
        this.offSpecs = offSpecs;
        this.defSpecs = defSpecs;
        this.elites = elites;
        this.prisoners = prisoners;
        this.warHorses = warHorses;
        this.money = money;
        this.food = food;
        this.runes = runes;
        this.buildingEfficiency = buildingEfficiency;
        this.tradeBalance = tradeBalance;
        this.plagued = hasPlague;
        this.hitStatus = hitStatus;
        this.overpopulated = isOverpopulated;
        this.lastUpdated = new Date(lastUpdated.getTime());
        this.exportLine = exportLine;
        this.savedBy = savedBy;
        this.accuracy = accuracy;
    }

    @FilterEnabled(PeasantsFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.PEASANTS)
    public int getPeasants() {
        return peasants;
    }

    @ProvinceResourceProvider(ProvinceResourceType.PEASANTS_PER_ACRE)
    public double getPeasantsPerAcre() {
        return getPeasants() * 1.0 / getProvince().getLand();
    }

    @FilterEnabled(SoldiersFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.SOLDIERS)
    public int getSoldiers() {
        return soldiers;
    }

    @ProvinceResourceProvider(ProvinceResourceType.OFF_SPECS)
    public int getOffSpecs() {
        return offSpecs;
    }

    @ProvinceResourceProvider(ProvinceResourceType.OFF_SPECS_PER_ACRE)
    public double getOffSpecsPerAcre() {
        return getOffSpecs() * 1.0 / getProvince().getLand();
    }

    @ProvinceResourceProvider(ProvinceResourceType.DEF_SPECS)
    public int getDefSpecs() {
        return defSpecs;
    }

    @ProvinceResourceProvider(ProvinceResourceType.DEF_SPECS_PER_ACRE)
    public double getDefSpecsPerAcre() {
        return getDefSpecs() * 1.0 / getProvince().getLand();
    }

    @ProvinceResourceProvider(ProvinceResourceType.ELITES)
    public int getElites() {
        return elites;
    }

    @ProvinceResourceProvider(ProvinceResourceType.ELITES_PER_ACRE)
    public double getElitesPerAcre() {
        return getElites() * 1.0 / getProvince().getLand();
    }

    @FilterEnabled(HorsesFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.HORSES)
    public int getWarHorses() {
        return warHorses;
    }

    @FilterEnabled(OffenseFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.MOD_OFFENSE)
    public int getModOffense() {
        return modOffense;
    }

    @ProvinceResourceProvider(ProvinceResourceType.MOD_OFFENSE_PER_ACRE)
    public int getModOffensePerAcre() {
        return getModOffense() / getProvince().getLand();
    }

    public double getOffensiveME() {
        Race race = getRace();
        return getModOffense() * 1.0 / (getSoldiers() * race.getSoldierStrength() + getOffSpecs() * race.getOffSpecStrength() +
                getElites() * race.getEliteOffStrength() +
                Math.min(getOffSpecs() + getElites(), getWarHorses()) +
                (isAngelIntel() ? Math.min((getOffSpecs() + getElites()) / 5, getPrisoners()) * 3 : 0));
    }

    public Integer getPmo() {
        if (getRace() == null) return null;
        int elitesToUse = (int) (getElites() * getRace().getEliteSendoutPercentage() * 1.0 / 100.0);
        return maxOffense(elitesToUse);
    }

    int maxOffense(final int elitesToSend) {
        return (int) ((getOffSpecs() * getRace().getOffSpecStrength() + elitesToSend * getRace().getEliteOffStrength() +
                Math.min(getWarHorses(), getOffSpecs() + elitesToSend) +
                Math.min(getPrisoners(), (getOffSpecs() + elitesToSend) / 5) * 3) * getOffensiveME());
    }

    @FilterEnabled(DefenseFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.MOD_DEFENSE)
    public int getModDefense() {
        return modDefense;
    }

    @ProvinceResourceProvider(ProvinceResourceType.MOD_DEFENSE_PER_ACRE)
    public int getModDefensePerAcre() {
        return getModDefense() / getProvince().getLand();
    }

    public double getDefensiveME() {
        Race race = getRace();
        int rawDefensiveBonuses = getRawDefensiveBonuses();
        return getModDefense() * 1.0 /
                (getSoldiers() * race.getSoldierStrength() + rawDefensiveBonuses + getDefSpecs() * race.getDefSpecStrength() +
                        getElites() * race.getEliteDefStrength());
    }

    public int getRawDefensiveBonuses() {
        Race race = getRace();
        int twBonus = 0;
        for (SpellType spellType : race.getSpellbook()) {
            Bonus bonus = spellType.getBonus(BonusType.PEASANT_DEFENSE, BonusApplicability.DEFENSIVELY);
            twBonus = (int) (getPeasants() * bonus.getBonusValue());
        }
        return twBonus;
    }

    public Integer getPmd() {
        if (getRace() == null) return null;
        return (int) ((getDefSpecs() * getRace().getDefSpecStrength() +
                getElites() * (1 - getRace().getEliteSendoutPercentage() * 1.0 / 100.0) * getRace().getEliteDefStrength() +
                getRawDefensiveBonuses()) * getDefensiveME());
    }

    @FilterEnabled(MoneyFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.GC)
    public int getMoney() {
        return money;
    }

    @ProvinceResourceProvider(ProvinceResourceType.GC_PER_ACRE)
    public int getMoneyPerAcre() {
        return getMoney() / getProvince().getLand();
    }

    @FilterEnabled(FoodFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.FOOD)
    public int getFood() {
        return food;
    }

    @FilterEnabled(RunesFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.RUNES)
    public int getRunes() {
        return runes;
    }

    @ProvinceResourceProvider(ProvinceResourceType.BE)
    public int getBuildingEfficiency() {
        return buildingEfficiency;
    }

    @ProvinceResourceProvider(ProvinceResourceType.TB)
    public int getTradeBalance() {
        return tradeBalance;
    }

    /**
     * @param isWar if it's a war
     * @return 0 if there's no gb prot, otherwise some value between 0 and 1
     */
    public double getApproximateGBProt(final boolean isWar) {
        if (getHitStatus().contains("little")) {
            return 0.1;
        } else if (getHitStatus().contains("moderately")) {
            return isWar ? 0.2 : 0.3;
        } else if (getHitStatus().contains("heavily")) {
            return isWar ? 0.2 : 0.75;
        } else if (getHitStatus().contains("extremely")) {
            return isWar ? 0.2 : 0.5;
        }
        return 0.0;
    }

    @Override
    public Object newSavedEvent() {
        return new SoTSavedEvent(id);
    }

    @Override
    @FilterEnabled(AgeFilter.class)
    public Date getLastUpdated() {
        return lastUpdated;
    }

    @FilterEnabled(RaceFilter.class)
    public Race getRace() {
        return getProvince().getRace();
    }

    @FilterEnabled(PersonalityFilter.class)
    public Personality getPersonality() {
        return getProvince().getPersonality();
    }

    @Override
    public int compareTo(final SoT o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getProvince().compareTo(o.getProvince());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(400);
        sb.append(getIntelType());
        sb.append("{province=").append(getProvince().getName());
        sb.append(", peasants=").append(getPeasants());
        sb.append(", soldiers=").append(getSoldiers());
        sb.append(", offSpecs=").append(getOffSpecs());
        sb.append(", defSpecs=").append(getDefSpecs());
        sb.append(", elites=").append(getElites());
        sb.append(", prisoners=").append(getPrisoners());
        sb.append(", warHorses=").append(getWarHorses());
        sb.append(", money=").append(getMoney());
        sb.append(", food=").append(getFood());
        sb.append(", runes=").append(getRunes());
        sb.append(", buildingEfficiency=").append(getBuildingEfficiency());
        sb.append(", tradeBalance=").append(getTradeBalance());
        sb.append(", savedBy='").append(getSavedBy()).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String getDescription() {
        return getIntelType() + " of " + getProvince().getName();
    }

    @Override
    public String getIntelType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getKingdomLocation() {
        return province.getKingdom().getLocation();
    }

    @Override
    public boolean isUnsaved() {
        return id == null;
    }
}
