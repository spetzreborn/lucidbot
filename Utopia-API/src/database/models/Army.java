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
import api.tools.text.StringUtil;
import filtering.filters.ExpiringFilter;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Comparator;
import java.util.Date;

@Entity
@Table(name = "army", uniqueConstraints = @UniqueConstraint(columnNames = {"province_id", "army_number", "type"}))
@NoArgsConstructor
@EqualsAndHashCode(of = {"province", "armyNumber", "type"})
@Getter
@Setter
public class Army implements Comparable<Army>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province province;

    @ManyToOne
    @JoinColumn(name = "som_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SoM som;

    @Column(name = "army_number", updatable = false, nullable = false)
    private int armyNumber = -1;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", updatable = false, nullable = false, length = 100)
    private ArmyType type;

    @Column(name = "soldiers", nullable = false)
    private int soldiers;

    @Column(name = "off_specs", nullable = false)
    private int offSpecs;

    @Column(name = "def_specs", nullable = false)
    private int defSpecs;

    @Column(name = "elites", nullable = false)
    private int elites;

    @Column(name = "war_horses", nullable = false)
    private int warHorses;

    @Column(name = "thieves", nullable = false)
    private int thieves;

    @Column(name = "mod_offense", nullable = false)
    private int modOffense;

    @Column(name = "mod_defense", nullable = false)
    private int modDefense;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "returning_date")
    private Date returningDate;

    @Column(name = "land_gained", nullable = false)
    private int landGained;

    @Column(name = "generals", nullable = false)
    private int generals = 4;

    public Army(final Province province,
                final int armyNumber,
                final ArmyType type,
                final int soldiers,
                final int offSpecs,
                final int defSpecs,
                final int elites,
                final int warHorses,
                final int modOffense,
                final int modDefense,
                final Date returningDate,
                final int landGained,
                final int generalsUsed) {
        this.province = province;
        this.armyNumber = armyNumber;
        this.type = type;
        this.soldiers = soldiers;
        this.offSpecs = offSpecs;
        this.defSpecs = defSpecs;
        this.elites = elites;
        this.warHorses = warHorses;
        this.modOffense = modOffense;
        this.modDefense = modDefense;
        this.returningDate = returningDate;
        this.landGained = landGained;
        generals = generalsUsed;
    }

    public Army(final Province province,
                final Integer armyNumber,
                final ArmyType type,
                final Date returningDate,
                final int landGained) {
        this.province = province;
        this.armyNumber = armyNumber;
        this.type = type;
        this.returningDate = returningDate;
        this.landGained = landGained;
    }

    public Kingdom getKingdom() {
        return getProvince().getKingdom();
    }

    /**
     * @return the amount of offense it would take to ambush this army
     */
    public int calculateAmbush() {
        if (getType() != ArmyType.ARMY_OUT)
            throw new UnsupportedOperationException("Can only calculate for armies out from a SoM");
        Race race = getProvince().getRace();
        if (race == null) throw new IllegalStateException("No race registered");
        return (int) (0.8 * (getSoldiers() * race.getSoldierStrength() + getOffSpecs() * race.getDefSpecStrength() +
                getElites() * race.getEliteDefStrength()));
    }

    @FilterEnabled(ExpiringFilter.class)
    public Date getReturningDate() {
        return returningDate;
    }

    public enum ArmyType {
        ARMY_HOME, ARMY_TRAINING, ARMY_OUT, IRC_ARMY_OUT {
            @Override
            public String getSource() {
                return "IRC";
            }
        };

        public String getTypeName() {
            return StringUtil.prettifyEnumName(this);
        }

        public String getSource() {
            return "SoM";
        }

        public static ArmyType fromName(final String name) {
            for (ArmyType armyType : values()) {
                if (armyType.getTypeName().equalsIgnoreCase(name)) return armyType;
            }
            return null;
        }
    }

    public static final Comparator<Army> RETURN_TIME_COMPARATOR = new ReturnTimeComparator();

    private static class ReturnTimeComparator implements Comparator<Army> {
        @Override
        public int compare(final Army o1, final Army o2) {
            if (o1.getType() == ArmyType.ARMY_HOME) return -1;
            else if (o1.getType() == ArmyType.ARMY_TRAINING)
                return o2.getType() == ArmyType.ARMY_HOME ? 1 : -1;
            else if (o2.getType() == ArmyType.ARMY_HOME || o2.getType() == ArmyType.ARMY_TRAINING) return 1;
            return o1.getReturningDate().compareTo(o2.getReturningDate());
        }
    }

    @Override
    public int compareTo(final Army o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return RETURN_TIME_COMPARATOR.compare(this, o);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(200);
        sb.append("Army");
        sb.append("{armyNumber=").append(getArmyNumber());
        sb.append(", type=").append(getType());
        sb.append(", soldiers=").append(getSoldiers());
        sb.append(", offSpecs=").append(getOffSpecs());
        sb.append(", defSpecs=").append(getDefSpecs());
        sb.append(", elites=").append(getElites());
        sb.append(", warHorses=").append(getWarHorses());
        sb.append(", thieves=").append(getThieves());
        sb.append(", landGained=").append(getLandGained());
        sb.append(", generals=").append(getGenerals());
        sb.append('}');
        return sb.toString();
    }
}
