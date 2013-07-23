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
import database.models.SoSEntry.SoSEntryType;
import events.SoSSavedEvent;
import filtering.filters.*;
import intel.ProvinceIntel;
import intel.ProvinceResourceProvider;
import intel.ProvinceResourceType;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "sos")
@NoArgsConstructor
@EqualsAndHashCode(of = "province")
@Getter
@Setter
public class SoS implements ProvinceIntel, Comparable<SoS>, HasNumericId {
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
    private List<SoSEntry> sciences = new ArrayList<>();

    @Column(name = "total_books", nullable = false)
    private int totalBooks;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;

    @Column(name = "export_line", length = 200)
    private String exportLine;

    @Column(name = "saved_by", nullable = false, length = 200)
    private String savedBy;

    @Column(name = "accuracy", nullable = false)
    private int accuracy = 100;

    public SoS(final Province province, final List<SoSEntry> sciences, final Date lastUpdated, final String exportLine,
               final String savedBy, final int accuracy) {
        this.province = province;
        this.sciences = new ArrayList<>(sciences);
        for (SoSEntry entry : sciences) {
            if (entry.getType() == SoSEntryType.BOOKS) {
                totalBooks += entry.getValue();
            }
        }
        this.lastUpdated = new Date(lastUpdated.getTime());
        this.exportLine = exportLine;
        this.savedBy = savedBy;
        this.accuracy = accuracy;
    }

    public void setSciences(final List<SoSEntry> sciences) {
        this.sciences = sciences;
    }

    public void calcTotalBooks() {
        totalBooks = 0;
        for (SoSEntry entry : sciences) {
            if (entry.getType() == SoSEntryType.BOOKS) {
                totalBooks += entry.getValue();
            }
        }
    }

    @FilterEnabled(ScienceBooksFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.BOOKS)
    public int getTotalBooks() {
        return totalBooks;
    }

    @FilterEnabled(BpaFilter.class)
    @ProvinceResourceProvider(ProvinceResourceType.BPA)
    public int getBooksPerAcre() {
        int land = getProvince().getLand();
        return land == 0 ? 0 : getTotalBooks() / land;
    }

    @Override
    public Object newSavedEvent() {
        return new SoSSavedEvent(id);
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

    public Map<String, SciInfo> getSciInfo() {
        Map<String, SciInfo> map = new HashMap<>();
        for (SoSEntry soSEntry : getSciences()) {
            String typeName = soSEntry.getScienceType().getName();
            if (!map.containsKey(typeName)) map.put(typeName, new SciInfo());
            map.get(typeName).setValue(soSEntry.getType(), soSEntry.getValue());
        }
        return map;
    }

    public double applyAnyBonuses(final BonusType bonusType, final BonusApplicability applicability, final double value) {
        double out = value;
        for (SoSEntry science : sciences) {
            ScienceType scienceType = science.getScienceType();
            if (science.getType() == SoSEntryType.EFFECT && scienceType.hasBonus(bonusType, applicability)) {
                Bonus bonus = scienceType.getBonus(bonusType, applicability);
                Bonus calcedBonus = new Bonus("", bonusType, applicability, bonus.isIncreasing(), science.getValue() / 100);
                out = calcedBonus.applyTo(out);
            }
        }
        return out;
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
    public static class SciInfo {
        private double effect;
        private int books;
        private int progress;

        private void setValue(final SoSEntryType entryType, final double val) {
            switch (entryType) {
                case EFFECT:
                    effect = val;
                    break;
                case BOOKS:
                    books = (int) val;
                    break;
                case BOOKS_IN_PROGRESS:
                    progress = (int) val;
            }
        }
    }

    @Override
    public int compareTo(final SoS o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getProvince().compareTo(o.getProvince());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(200);
        sb.append(getIntelType());
        sb.append("{province=").append(getProvince().getName());
        sb.append(", sciences=").append(getSciences());
        sb.append(", totalBooks=").append(getTotalBooks());
        sb.append(", savedBy='").append(getSavedBy()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
