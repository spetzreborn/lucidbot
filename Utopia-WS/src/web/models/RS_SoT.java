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
import database.models.SoT;
import web.tools.ISODateTimeAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

import static api.tools.text.StringUtil.isNullOrEmpty;

@XmlRootElement(name = "SoT")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_SoT implements HasNumericId {
    @XmlElement(name = "ID")
    private Long id;

    @Ref("intel/sots/{id}")
    @XmlElement(name = "Link")
    private URI link;

    @XmlElement(required = true, name = "Province")
    private RS_Province province;

    @XmlElement(name = "Peasants")
    private Integer peasants;

    @XmlElement(name = "Soldiers")
    private Integer soldiers;

    @XmlElement(name = "OffSpecs")
    private Integer offSpecs;

    @XmlElement(name = "DefSpecs")
    private Integer defSpecs;

    @XmlElement(name = "Elites")
    private Integer elites;

    @XmlElement(name = "Prisoners")
    private Integer prisoners;

    @XmlElement(name = "WarHorses")
    private Integer warHorses;

    @XmlElement(name = "ModOffense")
    private Integer modOffense;

    @XmlElement(name = "PracticalModOffense")
    private Integer practicalModOffense;

    @XmlElement(name = "OffensiveME")
    private Double offensiveME;

    @XmlElement(name = "ModDefense")
    private Integer modDefense;

    @XmlElement(name = "PracticalModDefense")
    private Integer practicalModDefense;

    @XmlElement(name = "DefensiveME")
    private Double defensiveME;

    @XmlElement(name = "RawDefenseBonus")
    private Integer rawDefenseBonus;

    @XmlElement(name = "Money")
    private Integer money;

    @XmlElement(name = "Food")
    private Integer food;

    @XmlElement(name = "Runes")
    private Integer runes;

    @XmlElement(name = "BuildingEfficiency")
    private Integer buildingEfficiency;

    @XmlElement(name = "TradeBalance")
    private Integer tradeBalance;

    @XmlElement(name = "Plagued")
    private Boolean plagued;

    @XmlElement(name = "HitStatus")
    private String hitStatus;

    @XmlElement(name = "Overpopulated")
    private Boolean overpopulated;

    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastUpdated")
    private Date lastUpdated;

    @XmlElement(name = "ExportLine")
    private String exportLine;

    @XmlElement(name = "SavedBy")
    private String savedBy;

    @XmlElement(name = "IsAngelIntel")
    private Boolean isAngelIntel;

    @XmlElement(name = "Accuracy")
    private Integer accuracy;

    public RS_SoT() {
    }

    private RS_SoT(final Long id, final RS_Province province) {
        this.id = id;
        this.province = province;
    }

    private RS_SoT(final SoT sot) {
        this(sot.getId(), RS_Province.fromProvince(sot.getProvince(), false));
        this.peasants = sot.getPeasants();
        this.soldiers = sot.getSoldiers();
        this.offSpecs = sot.getOffSpecs();
        this.defSpecs = sot.getDefSpecs();
        this.elites = sot.getElites();
        this.prisoners = sot.getPrisoners();
        this.warHorses = sot.getWarHorses();
        this.modOffense = sot.getModOffense();
        this.practicalModOffense = sot.getPmo();
        this.offensiveME = sot.getOffensiveME();
        this.modDefense = sot.getModDefense();
        this.practicalModDefense = sot.getPmd();
        this.defensiveME = sot.getDefensiveME();
        this.rawDefenseBonus = sot.getRawDefensiveBonuses();
        this.money = sot.getMoney();
        this.food = sot.getFood();
        this.runes = sot.getRunes();
        this.buildingEfficiency = sot.getBuildingEfficiency();
        this.tradeBalance = sot.getTradeBalance();
        this.plagued = sot.isPlagued();
        this.hitStatus = isNullOrEmpty(sot.getHitStatus()) ? null : sot.getHitStatus();
        this.overpopulated = sot.isOverpopulated();
        this.lastUpdated = sot.getLastUpdated();
        this.exportLine = sot.getExportLine();
        this.savedBy = sot.getSavedBy();
        this.isAngelIntel = sot.isAngelIntel();
        this.accuracy = sot.getAccuracy();
    }

    public static RS_SoT fromSoT(final SoT sot, final boolean full) {
        return full ? new RS_SoT(sot) : new RS_SoT(sot.getId(), RS_Province.fromProvince(sot.getProvince(), false));
    }

    public Long getId() {
        return id;
    }

    public RS_Province getProvince() {
        return province;
    }

    public Integer getPeasants() {
        return peasants;
    }

    public Integer getSoldiers() {
        return soldiers;
    }

    public Integer getOffSpecs() {
        return offSpecs;
    }

    public Integer getDefSpecs() {
        return defSpecs;
    }

    public Integer getElites() {
        return elites;
    }

    public Integer getPrisoners() {
        return prisoners;
    }

    public Integer getWarHorses() {
        return warHorses;
    }

    public Integer getModOffense() {
        return modOffense;
    }

    public Integer getPracticalModOffense() {
        return practicalModOffense;
    }

    public Double getOffensiveME() {
        return offensiveME;
    }

    public Integer getModDefense() {
        return modDefense;
    }

    public Integer getPracticalModDefense() {
        return practicalModDefense;
    }

    public Double getDefensiveME() {
        return defensiveME;
    }

    public Integer getRawDefenseBonus() {
        return rawDefenseBonus;
    }

    public Integer getMoney() {
        return money;
    }

    public Integer getFood() {
        return food;
    }

    public Integer getRunes() {
        return runes;
    }

    public Integer getBuildingEfficiency() {
        return buildingEfficiency;
    }

    public Integer getTradeBalance() {
        return tradeBalance;
    }

    public Boolean getPlagued() {
        return plagued != null && plagued;
    }

    public String getHitStatus() {
        return hitStatus;
    }

    public Boolean getOverpopulated() {
        return overpopulated != null && overpopulated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getExportLine() {
        return exportLine;
    }

    public String getSavedBy() {
        return savedBy;
    }

    public Boolean getAngelIntel() {
        return isAngelIntel != null && isAngelIntel;
    }

    public Integer getAccuracy() {
        return accuracy;
    }
}
