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
import events.SoMSavedEvent;
import filtering.filters.AgeFilter;
import filtering.filters.PersonalityFilter;
import filtering.filters.RaceFilter;
import intel.Intel;
import intel.ProvinceIntel;
import intel.ProvinceResourceProvider;
import intel.ProvinceResourceType;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "som")
@NoArgsConstructor
@EqualsAndHashCode(of = "province")
@Getter
@Setter
public class SoM implements ProvinceIntel, Comparable<SoM>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province province;

    @Column(name = "net_defense")
    private Integer netDefense;

    @Column(name = "net_offense")
    private Integer netOffense;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;

    @Column(name = "export_line", length = 200)
    private String exportLine;

    @Column(name = "saved_by", nullable = false, length = 200)
    private String savedBy;

    @OneToMany(mappedBy = "som", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Army> armies = new HashSet<>();

    @Column(name = "armies_out_when_posted", nullable = false)
    private int armiesOutWhenPosted = 0;

    @Column(name = "accuracy", nullable = false)
    private int accuracy = 100;

    public SoM(final Province province, final int netDefense, final int netOffense, final Date lastUpdated, final String exportLine,
               final String savedBy, final Collection<Army> armies, final int accuracy) {
        this.province = province;
        this.netDefense = netDefense;
        this.netOffense = netOffense;
        this.lastUpdated = lastUpdated;
        this.exportLine = exportLine;
        this.savedBy = savedBy;
        this.armies = new HashSet<>(armies);
        this.armiesOutWhenPosted = armies.size();
        this.accuracy = accuracy;
    }

    @Override
    public Object newSavedEvent() {
        return new SoMSavedEvent(id);
    }

    @Override
    @FilterEnabled(AgeFilter.class)
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public Army getArmyHome() {
        for (Army army : getArmies()) {
            if (army.getType() == Army.ArmyType.ARMY_HOME) return army;
        }
        return null;
    }

    public Army getArmyInTraining() {
        for (Army army : getArmies()) {
            if (army.getType() == Army.ArmyType.ARMY_TRAINING) return army;
        }
        return null;
    }

    public Army getArmyOut(final int armyNo) {
        for (Army army : getArmies()) {
            if (army.getType() == Army.ArmyType.ARMY_OUT && army.getArmyNumber() == armyNo) return army;
        }
        return null;
    }

    public List<Army> getArmiesOut() {
        List<Army> out = new ArrayList<>();
        for (Army army : getArmies()) {
            if (army.getType() == Army.ArmyType.ARMY_OUT) out.add(army);
        }
        return out;
    }

    @ProvinceResourceProvider(ProvinceResourceType.ELITES_OUT)
    public int getElitesOut() {
        int sum = 0;
        for (Army army : getArmies()) {
            if (army.getType() == Army.ArmyType.ARMY_OUT) sum += army.getElites();
        }
        return sum;
    }

    @FilterEnabled(RaceFilter.class)
    public Race getRace() {
        return getProvince().getRace();
    }

    @FilterEnabled(PersonalityFilter.class)
    public Personality getPersonality() {
        return getProvince().getPersonality();
    }

    public List<Army> getSortedArmies() {
        List<Army> armies = new ArrayList<>(getArmies());
        Collections.sort(armies);
        return armies;
    }

    @Override
    public int compareTo(final SoM o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getProvince().compareTo(o.getProvince());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(200);
        sb.append(getIntelTypeName());
        sb.append("{province=").append(getProvince().getName());
        sb.append(", netDefense=").append(getNetDefense());
        sb.append(", netOffense=").append(getNetOffense());
        sb.append(", savedBy='").append(getSavedBy()).append('\'');
        sb.append(", armies=").append(getArmies());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String getDescription() {
        return getIntelTypeName() + " of " + getProvince().getName();
    }

    @Override
    public String getIntelTypeName() {
        return getClass().getSimpleName();
    }

    @Override
    public Class<? extends Intel> getIntelType() {
        return getClass();
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
