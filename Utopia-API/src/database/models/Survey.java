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

import api.filters.FilterEnabled;
import database.models.SurveyEntry.SurveyEntryType;
import events.SurveySavedEvent;
import filtering.filters.AgeFilter;
import filtering.filters.BuildingFilter;
import filtering.filters.PersonalityFilter;
import filtering.filters.RaceFilter;
import intel.ProvinceIntel;
import intel.ProvinceResourceProvider;
import intel.ProvinceResourceType;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import tools.GameMechanicCalculator;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "survey")
@NoArgsConstructor
@EqualsAndHashCode(of = "province")
@Getter
@Setter
public class Survey implements ProvinceIntel, Comparable<Survey> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province province;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurveyEntry> buildings = new ArrayList<>();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;

    @Column(name = "export_line", length = 200)
    private String exportLine;

    @Column(name = "saved_by", nullable = false, length = 200)
    private String savedBy;

    @Column(name = "accuracy", nullable = false)
    private int accuracy = 100;

    @Transient
    private int totalBuilt;

    @Transient
    private int totalInProgress;

    public Survey(final Province province, final List<SurveyEntry> buildings, final Date lastUpdated, final String exportLine,
                  final String savedBy, final int accuracy) {
        this.province = province;
        this.buildings = new ArrayList<>(buildings);
        this.lastUpdated = new Date(lastUpdated.getTime());
        this.exportLine = exportLine;
        this.savedBy = savedBy;
        this.accuracy = accuracy;
    }

    public int getTotalBuilt() {
        if (totalBuilt == 0) totalBuilt = calcTotalBuilt();
        return totalBuilt;
    }

    private int calcTotalBuilt() {
        if (getBuildings().isEmpty()) return 0;
        int totalBuilt = 0;
        for (SurveyEntry entry : getBuildings()) {
            if (entry.getType() == SurveyEntryType.BUILT) {
                totalBuilt += entry.getValue();
            }
        }
        return totalBuilt;
    }

    public int getTotalInProgress() {
        if (totalInProgress == 0) totalInProgress = calcTotalInProgress();
        return totalInProgress;
    }

    private int calcTotalInProgress() {
        if (getBuildings().isEmpty()) return 0;
        int totalInProgress = 0;
        for (SurveyEntry entry : getBuildings()) {
            if (entry.getType() == SurveyEntryType.IN_PROGRESS) {
                totalInProgress += entry.getValue();
            }
        }
        return totalInProgress;
    }

    public double applyAnyBonuses(final BonusType bonusType, final BonusApplicability applicability, final double value) {
        double out = value;
        for (SurveyEntry entry : getBuildings()) {
            if (entry.getType() != SurveyEntryType.BUILT) continue;

            Building building = entry.getBuilding();
            for (BuildingFormula formula : building.getFormulas()) {
                if (formula.hasBonus(bonusType, applicability)) {
                    String buildingName = formula.getBuilding().getName();
                    int amount = getBuildingAmount(buildingName);
                    double percentage = getBuildingPercentage(buildingName);
                    int be = province.getSot() == null ? 100 : province.getSot().getBuildingEfficiency();

                    Double result = GameMechanicCalculator.performBuildingEffectCalculation(formula, percentage, (double) amount, be);
                    if (result != null) {
                        Bonus buildingBonus = new Bonus("", BonusType.GAIN, applicability, formula.getBonus().isIncreasing(), result / 100);
                        out = buildingBonus.applyTo(out);
                    }
                }
            }
        }
        return out;
    }

    @Override
    public Object newSavedEvent() {
        return new SurveySavedEvent(id);
    }

    @Override
    @FilterEnabled(AgeFilter.class)
    public Date getLastUpdated() {
        return new Date(lastUpdated.getTime());
    }

    @FilterEnabled(RaceFilter.class)
    public Race getRace() {
        return getProvince().getRace();
    }

    @FilterEnabled(PersonalityFilter.class)
    public Personality getPersonality() {
        return getProvince().getPersonality();
    }

    @ProvinceResourceProvider(ProvinceResourceType.BUILDING_AMOUNT)
    public int getBuildingAmount(final String building) {
        for (SurveyEntry surveyEntry : getBuildings()) {
            if ((surveyEntry.getBuilding().getName().equalsIgnoreCase(building) ||
                    surveyEntry.getBuilding().getShortName().equalsIgnoreCase(building)) && surveyEntry.getType() == SurveyEntryType.BUILT)
                return surveyEntry.getValue();
        }
        return 0;
    }

    @FilterEnabled(BuildingFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.BUILDING_PERCENTAGE)
    public double getBuildingPercentage(final String building) {
        return getBuildingAmount(building) * 1.0 / (getTotalBuilt() + getTotalInProgress()) * 100.0;
    }

    @Override
    public int compareTo(final Survey o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getProvince().compareTo(o.getProvince());
    }

    public Map<String, BuildingInfo> getBuildingInfo() {
        Map<String, BuildingInfo> map = new HashMap<>();
        for (SurveyEntry surveyEntry : getBuildings()) {
            String typeName = surveyEntry.getBuilding().getName();
            if (!map.containsKey(typeName)) map.put(typeName, new BuildingInfo());
            map.get(typeName).setValue(surveyEntry.getType(), surveyEntry.getValue(), getProvince().getLand());
        }
        return map;
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

    @NoArgsConstructor
    @Getter
    public static class BuildingInfo {
        private double percent;
        private int amount;
        private int progress;
        private double progressPercent;

        private void setValue(final SurveyEntryType entryType, final int val, final int land) {
            switch (entryType) {
                case BUILT:
                    amount = val;
                    percent = val * 100.0 / land;
                    break;
                case IN_PROGRESS:
                    progress = val;
                    progressPercent = val * 100.0 / land;
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(200);
        sb.append(getIntelType());
        sb.append("{province=").append(getProvince().getName());
        sb.append(", buildings=").append(getBuildings());
        sb.append(", savedBy='").append(getSavedBy()).append('\'');
        sb.append(", totalBuilt=").append(getTotalBuilt());
        sb.append(", totalInProgress=").append(getTotalInProgress());
        sb.append('}');
        return sb.toString();
    }
}
