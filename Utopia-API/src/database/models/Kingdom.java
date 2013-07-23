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
import events.KingdomSavedEvent;
import filtering.filters.AgeFilter;
import filtering.filters.KingdomLocationFilter;
import filtering.filters.LandFilter;
import filtering.filters.NetworthFilter;
import intel.Intel;
import intel.IntelSourceProvider;
import lombok.*;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "kingdom")
@NoArgsConstructor
@EqualsAndHashCode(of = "location")
@Getter
@Setter
public class Kingdom implements Intel, Comparable<Kingdom>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "location", updatable = false, nullable = false, unique = true, length = 7)
    private String location;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Lob
    @Column(name = "kd_comment", length = 500)
    private String kdComment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dragon_id")
    private Dragon dragon;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "nap_end_date")
    private Date napEndDate;

    @Lob
    @Column(name = "nap_description", length = 1000)
    private String napDescription;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "nap_added")
    private Date napAdded;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated", nullable = false)
    private Date lastUpdated;

    @OneToMany(mappedBy = "kingdom")
    private List<Province> provinces = new ArrayList<>();

    @Column(name = "saved_by", nullable = false, length = 200)
    private String savedBy = "";

    public Kingdom(final String location) {
        this.location = location;
        this.name = "";
        this.lastUpdated = new Date();
    }

    @IntelSourceProvider(Province.class)
    public Collection<Province> getProvinces() {
        return provinces;
    }

    public List<Province> getSortedProvinces() {
        List<Province> out = new ArrayList<>(provinces);
        Collections.sort(out);
        return out;
    }

    @FilterEnabled(LandFilter.class)
    public int getTotalLand() {
        int total = 0;
        for (Province province : getProvinces()) {
            total += province.getLand();
        }
        return total;
    }

    public int getAverageLand() {
        int land = 0;
        int contributors = 0;
        for (Province province : getProvinces()) {
            land += province.getLand();
            if (province.getLand() != 0) ++contributors;
        }
        return contributors == 0 ? 0 : land / contributors;
    }

    @FilterEnabled(NetworthFilter.class)
    public int getTotalNw() {
        int total = 0;
        for (Province province : getProvinces()) {
            total += province.getNetworth();
        }
        return total;
    }

    public int getAverageNw() {
        int nw = 0;
        int contributors = 0;
        for (Province province : getProvinces()) {
            nw += province.getNetworth();
            if (province.getNetworth() != 0) ++contributors;
        }
        return contributors == 0 ? 0 : nw / contributors;
    }

    @IntelSourceProvider(SoM.class)
    public List<SoM> getSoMs() {
        List<SoM> list = new ArrayList<>();
        for (Province province : getProvinces()) {
            if (province.getSom() != null) list.add(province.getSom());
        }
        return list;
    }

    @IntelSourceProvider(SoS.class)
    public List<SoS> getSoSs() {
        List<SoS> list = new ArrayList<>();
        for (Province province : getProvinces()) {
            if (province.getSos() != null) list.add(province.getSos());
        }
        return list;
    }

    @IntelSourceProvider(SoT.class)
    public List<SoT> getSoTs() {
        List<SoT> list = new ArrayList<>();
        for (Province province : getProvinces()) {
            if (province.getSot() != null) list.add(province.getSot());
        }
        return list;
    }

    @IntelSourceProvider(Survey.class)
    public List<Survey> getSurveys() {
        List<Survey> list = new ArrayList<>();
        for (Province province : getProvinces()) {
            if (province.getSurvey() != null) list.add(province.getSurvey());
        }
        return list;
    }

    public List<SetupInfo> getSetupInfo() {
        Map<String, Integer> honorTitles = new HashMap<>();
        Map<String, Integer> races = new HashMap<>();
        Map<String, Integer> personalities = new HashMap<>();

        Collection<Province> provinceCollection = getProvinces();
        for (Province province : provinceCollection) {
            HonorTitle honorTitle = province.getHonorTitle();
            if (honorTitle != null) {
                String honorTitleName = honorTitle.getName();
                if (!honorTitles.containsKey(honorTitleName)) honorTitles.put(honorTitleName, 1);
                else honorTitles.put(honorTitleName, honorTitles.get(honorTitleName) + 1);
            }

            Race race = province.getRace();
            if (race != null) {
                String raceName = race.getName();
                if (!races.containsKey(raceName)) races.put(raceName, 1);
                else races.put(raceName, races.get(raceName) + 1);
            }

            Personality personality = province.getPersonality();
            if (personality != null) {
                String personalityName = personality.getName();
                if (!personalities.containsKey(personalityName)) personalities.put(personalityName, 1);
                else personalities.put(personalityName, personalities.get(personalityName) + 1);
            }
        }

        List<SetupInfo> out = new ArrayList<>(3);
        int provs = provinceCollection.size();
        out.add(new SetupInfo("Titles", honorTitles, provs));
        out.add(new SetupInfo("Races", races, provs));
        out.add(new SetupInfo("Personalities", personalities, provs));
        return out;
    }

    @Override
    public Object newSavedEvent() {
        return new KingdomSavedEvent(id);
    }

    @Override
    @FilterEnabled(AgeFilter.class)
    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public int compareTo(final Kingdom o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getLocation().compareTo(o.getLocation());
    }

    @Override
    public String getDescription() {
        return getIntelType() + " page of " + getLocation();
    }

    @Override
    public String getIntelType() {
        return getClass().getSimpleName();
    }

    @Override
    @FilterEnabled(KingdomLocationFilter.class)
    public String getKingdomLocation() {
        return location;
    }

    @Override
    public boolean isUnsaved() {
        return id == null;
    }

    @Getter
    public static class SetupInfo {
        private final String title;
        private final Map<String, Integer> map = new HashMap<>();
        private final int unknowns;

        private SetupInfo(final String title, final Map<String, Integer> info, final int totalProvinces) {
            this.title = title;
            int temp = totalProvinces;
            for (Map.Entry<String, Integer> entry : info.entrySet()) {
                map.put(entry.getKey(), entry.getValue());
                temp -= entry.getValue();
            }
            this.unknowns = temp;
        }
    }
}
