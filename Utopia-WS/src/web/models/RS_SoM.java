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
import database.models.Army;
import database.models.SoM;
import web.tools.ISODateTimeAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlRootElement(name = "SoM")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_SoM implements HasNumericId {
    @XmlElement(name = "ID")
    private Long id;

    @Ref("intel/soms/{id}")
    @XmlElement(name = "Link")
    private URI link;

    @XmlElement(required = true, name = "Province")
    private RS_Province province;

    @XmlElement(name = "NetDefense")
    private Integer netDefense;

    @XmlElement(name = "NetOffense")
    private Integer netOffense;

    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastUpdated")
    private Date lastUpdated;

    @XmlElement(name = "ExportLine")
    private String exportLine;

    @XmlElement(name = "SavedBy")
    private String savedBy;

    @XmlElement(name = "Accuracy")
    private Integer accuracy;

    @XmlElementWrapper(nillable = true, name = "Armies")
    @XmlElement(required = true, name = "Army")
    private List<RS_Army> armies;

    public RS_SoM() {
    }

    private RS_SoM(final Long id, final RS_Province province) {
        this.id = id;
        this.province = province;
    }

    private RS_SoM(final SoM som) {
        this(som.getId(), RS_Province.fromProvince(som.getProvince(), false));
        this.netDefense = som.getNetDefense();
        this.netOffense = som.getNetOffense();
        this.lastUpdated = som.getLastUpdated();
        this.exportLine = som.getExportLine();
        this.savedBy = som.getSavedBy();
        this.accuracy = som.getAccuracy();
        this.armies = new ArrayList<>();
        for (Army army : som.getArmies()) {
            this.armies.add(RS_Army.fromArmy(army, false));
        }
    }

    public static RS_SoM fromSoM(final SoM som, final boolean full) {
        return full ? new RS_SoM(som) : new RS_SoM(som.getId(), RS_Province.fromProvince(som.getProvince(), false));
    }

    public Long getId() {
        return id;
    }

    public RS_Province getProvince() {
        return province;
    }

    public Integer getNetDefense() {
        return netDefense;
    }

    public Integer getNetOffense() {
        return netOffense;
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

    public Integer getAccuracy() {
        return accuracy;
    }

    public List<RS_Army> getArmies() {
        return toEmptyListIfNull(armies);
    }
}
