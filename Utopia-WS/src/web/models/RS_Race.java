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

package web.models;

import api.common.HasNumericId;
import com.sun.jersey.server.linking.Ref;
import database.models.Bonus;
import database.models.Race;

import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlRootElement(name = "Race")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Race implements HasNumericId {
    @XmlElement(name = "ID")
    private Long id;

    @Ref("races/{id}")
    @XmlElement(name = "Link")
    private URI link;

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "ShortName")
    private String shortName;

    @XmlElement(name = "SoldierStrength")
    private Integer soldierStrength;

    @XmlElement(name = "SoldierNetworth")
    private Double soldierNetworth;

    @XmlElement(name = "OffSpecName")
    private String offSpecName;

    @XmlElement(name = "OffSpecStrength")
    private Integer offSpecStrength;

    @XmlElement(name = "DefSpecName")
    private String defSpecName;

    @XmlElement(name = "DefSpecStrength")
    private Integer defSpecStrength;

    @XmlElement(name = "EliteName")
    private String eliteName;

    @XmlElement(name = "EliteOffStrength")
    private Integer eliteOffStrength;

    @XmlElement(name = "EliteDefStrength")
    private Integer eliteDefStrength;

    @XmlElement(name = "EliteNetworth")
    private Double eliteNetworth;

    @XmlElement(name = "EliteSendoutPercentage")
    private Double eliteSendoutPercentage;

    @XmlElement(name = "IntelAccuracySpecification")
    private String intelAccuracySpecification;

    @XmlElement(name = "Pros")
    private String pros;

    @XmlElement(name = "Cons")
    private String cons;

    @XmlElementWrapper(name = "Bonuses")
    @XmlElement(name = "Bonus")
    private List<RS_Bonus> bonuses;

    public RS_Race() {
    }

    private RS_Race(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    private RS_Race(final Race race) {
        this(race.getId(), race.getName());
        this.shortName = race.getShortName();
        this.soldierStrength = race.getSoldierStrength();
        this.soldierNetworth = race.getSoldierNetworth();
        this.offSpecName = race.getOffSpecName();
        this.offSpecStrength = race.getOffSpecStrength();
        this.defSpecName = race.getDefSpecName();
        this.defSpecStrength = race.getDefSpecStrength();
        this.eliteName = race.getEliteName();
        this.eliteOffStrength = race.getEliteOffStrength();
        this.eliteDefStrength = race.getEliteDefStrength();
        this.eliteNetworth = race.getEliteNetworth();
        this.eliteSendoutPercentage = race.getEliteSendoutPercentage() / 100.0;
        this.intelAccuracySpecification = race.getIntelAccuracySpecification().getName();
        this.pros = race.getPros();
        this.cons = race.getCons();
        this.bonuses = new ArrayList<>();
        for (Bonus bonus : race.getBonuses()) {
            this.bonuses.add(RS_Bonus.fromBonus(bonus));
        }
    }

    public static RS_Race fromRace(final Race race, final boolean full) {
        return full ? new RS_Race(race) : new RS_Race(race.getId(), race.getName());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public Integer getSoldierStrength() {
        return soldierStrength;
    }

    public Double getSoldierNetworth() {
        return soldierNetworth;
    }

    public String getOffSpecName() {
        return offSpecName;
    }

    public Integer getOffSpecStrength() {
        return offSpecStrength;
    }

    public String getDefSpecName() {
        return defSpecName;
    }

    public Integer getDefSpecStrength() {
        return defSpecStrength;
    }

    public String getEliteName() {
        return eliteName;
    }

    public Integer getEliteOffStrength() {
        return eliteOffStrength;
    }

    public Integer getEliteDefStrength() {
        return eliteDefStrength;
    }

    public Double getEliteNetworth() {
        return eliteNetworth;
    }

    public Double getEliteSendoutPercentage() {
        return eliteSendoutPercentage;
    }

    public String getIntelAccuracySpecification() {
        return intelAccuracySpecification;
    }

    public String getPros() {
        return pros;
    }

    public String getCons() {
        return cons;
    }

    public List<RS_Bonus> getBonuses() {
        return toEmptyListIfNull(bonuses);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RS_Race rs_race = (RS_Race) o;

        if (id != null ? !id.equals(rs_race.id) : rs_race.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
