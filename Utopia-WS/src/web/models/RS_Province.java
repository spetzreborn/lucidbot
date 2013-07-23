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
import database.models.BonusApplicability;
import database.models.BonusType;
import database.models.Kingdom;
import database.models.Province;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ExistsInDB;
import web.tools.ISODateTimeAdapter;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "Province")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Province implements HasNumericId {
    @XmlElement(name = "ID")
    private Long id;

    @Ref("provinces/{id}")
    @XmlElement(name = "Link")
    private URI link;

    @NotEmpty(message = "The name must not be null or empty")
    @XmlElement(name = "Name")
    private String name;

    @NotNull(message = "The kingdom must not be null")
    @ExistsInDB(entity = Kingdom.class, message = "No such kingdom")
    @XmlElement(name = "Kingdom")
    private RS_Kingdom kingdom;

    @XmlElement(name = "Race")
    private RS_Race race;

    @XmlElement(name = "Personality")
    private RS_Personality personality;

    @XmlElement(name = "HonorTitle")
    private RS_HonorTitle honorTitle;

    @XmlElement(name = "Land")
    private Integer land;

    @XmlElement(name = "Networth")
    private Integer networth;

    @XmlElement(name = "Wizards")
    private Integer wizards;

    @XmlElement(name = "WizardryModifiers")
    private Double wizardryModifiers;

    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "WizardsLastUpdated")
    private Date wizardsLastUpdated;

    @XmlElement(name = "Mana")
    private Integer mana;

    @XmlElement(name = "Thieves")
    private Integer thieves;

    @XmlElement(name = "ThieveryModifiers")
    private Double thieveryModifiers;

    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "ThievesLastUpdated")
    private Date thievesLastUpdated;

    @XmlElement(name = "Stealth")
    private Integer stealth;

    @XmlElement(name = "GeneralsTotal")
    private Integer generalsTotal;

    @XmlElement(name = "GeneralsHome")
    private Integer generalsHome;

    @XmlElement(name = "EstimatedCurrentOffense")
    private Integer estimatedCurrentOffense;

    @XmlElement(name = "EstimatedCurrentDefense")
    private Integer estimatedCurrentDefense;

    @XmlElement(name = "Owner")
    private RS_User owner;

    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastUpdated")
    private Date lastUpdated;

    @XmlElement(name = "SoM")
    private RS_SoM som;

    @XmlElement(name = "SoS")
    private RS_SoS sos;

    @XmlElement(name = "SoT")
    private RS_SoT sot;

    @XmlElement(name = "Survey")
    private RS_Survey survey;

    public RS_Province() {
    }

    private RS_Province(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    private RS_Province(final Province province) {
        this(province.getId(), province.getName());
        this.kingdom = RS_Kingdom.fromKingdom(province.getKingdom(), false);
        this.race = province.getRace() == null ? null : RS_Race.fromRace(province.getRace(), false);
        this.personality = province.getPersonality() == null ? null : RS_Personality.fromPersonality(province.getPersonality(), false);
        this.honorTitle = province.getHonorTitle() == null ? null : RS_HonorTitle.fromHonorTitle(province.getHonorTitle(), false);
        this.land = province.getLand();
        this.networth = province.getNetworth();
        this.wizards = province.getWizards();
        this.wizardryModifiers = province.applyMultiplicativeBonuses(1.0, BonusType.WPA, BonusApplicability.OFFENSIVELY);
        this.wizardsLastUpdated = province.getWizardsLastUpdated();
        this.mana = province.getMana();
        this.thieves = province.getThieves();
        this.thieveryModifiers = province.applyMultiplicativeBonuses(1.0, BonusType.TPA, BonusApplicability.OFFENSIVELY);
        this.thievesLastUpdated = province.getThievesLastUpdated();
        this.stealth = province.getStealth();
        this.generalsTotal = province.getGenerals();
        this.generalsHome = province.getGeneralsHome();
        this.estimatedCurrentOffense = province.getEstimatedCurrentOffense();
        this.estimatedCurrentDefense = province.getEstimatedCurrentDefense();
        this.owner = province.getProvinceOwner() == null ? null : RS_User.fromBotUser(province.getProvinceOwner(), false);
        this.lastUpdated = province.getLastUpdated();
        this.som = province.getSom() == null ? null : RS_SoM.fromSoM(province.getSom(), false);
        this.sos = province.getSos() == null ? null : RS_SoS.fromSoS(province.getSos(), false);
        this.sot = province.getSot() == null ? null : RS_SoT.fromSoT(province.getSot(), false);
        this.survey = province.getSurvey() == null ? null : RS_Survey.fromSurvey(province.getSurvey(), false);
    }

    public static RS_Province fromProvince(final Province province, final boolean full) {
        return full ? new RS_Province(province) : new RS_Province(province.getId(), province.getName());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RS_Kingdom getKingdom() {
        return kingdom;
    }

    public RS_Race getRace() {
        return race;
    }

    public RS_Personality getPersonality() {
        return personality;
    }

    public RS_HonorTitle getHonorTitle() {
        return honorTitle;
    }

    public Integer getLand() {
        return land;
    }

    public Integer getNetworth() {
        return networth;
    }

    public Integer getWizards() {
        return wizards;
    }

    public Double getWizardryModifiers() {
        return wizardryModifiers;
    }

    public Date getWizardsLastUpdated() {
        return wizardsLastUpdated;
    }

    public Integer getMana() {
        return mana;
    }

    public Integer getThieves() {
        return thieves;
    }

    public Double getThieveryModifiers() {
        return thieveryModifiers;
    }

    public Date getThievesLastUpdated() {
        return thievesLastUpdated;
    }

    public Integer getStealth() {
        return stealth;
    }

    public Integer getGeneralsTotal() {
        return generalsTotal;
    }

    public Integer getGeneralsHome() {
        return generalsHome;
    }

    public Integer getEstimatedCurrentOffense() {
        return estimatedCurrentOffense;
    }

    public Integer getEstimatedCurrentDefense() {
        return estimatedCurrentDefense;
    }

    public RS_User getOwner() {
        return owner;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public RS_SoM getSom() {
        return som;
    }

    public RS_SoS getSos() {
        return sos;
    }

    public RS_SoT getSot() {
        return sot;
    }

    public RS_Survey getSurvey() {
        return survey;
    }
}
