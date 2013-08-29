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
import database.models.Survey;
import database.models.SurveyEntry;
import web.tools.ISODateTimeAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.*;

import static api.tools.collections.ListUtil.toEmptyListIfNull;
import static com.google.common.base.Objects.firstNonNull;

@XmlRootElement(name = "Survey")
@XmlType(name = "Survey")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Survey implements HasNumericId {
    @XmlElement(name = "ID")
    private Long id;

    @Ref("intel/surveys/{id}")
    @XmlElement(name = "Link")
    private URI link;

    @XmlElement(required = true, name = "Province")
    private RS_Province province;

    @XmlElementWrapper(nillable = true, name = "Entries")
    @XmlElement(required = true, name = "SurveyEntry")
    private List<RS_SurveyEntry> entries;

    @XmlElement(name = "Built")
    private Integer built;

    @XmlElement(name = "InProgress")
    private Integer inProgress;

    @XmlElement(name = "ExportLine")
    private String exportLine;

    @XmlElement(name = "SavedBy")
    private String savedBy;

    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastUpdated")
    private Date lastUpdated;

    @XmlElement(name = "Accuracy")
    private Integer accuracy;

    public RS_Survey() {
    }

    private RS_Survey(final Long id, final RS_Province province) {
        this.id = id;
        this.province = province;
    }

    private RS_Survey(final Survey survey) {
        this(survey.getId(), RS_Province.fromProvince(survey.getProvince(), false));
        this.entries = new ArrayList<>(mapEntries(survey));
        this.built = survey.getTotalBuilt();
        this.inProgress = survey.getTotalInProgress();
        this.exportLine = survey.getExportLine();
        this.savedBy = survey.getSavedBy();
        this.lastUpdated = survey.getLastUpdated();
        this.accuracy = survey.getAccuracy();
    }

    private Collection<RS_SurveyEntry> mapEntries(final Survey survey) {
        Map<String, RS_SurveyEntry> entryMap = new HashMap<>();
        for (SurveyEntry entry : survey.getBuildings()) {
            String buildingName = entry.getBuilding().getName();
            if (!entryMap.containsKey(buildingName)) {
                entryMap.put(buildingName, new RS_SurveyEntry(entry.getBuilding()));
            }
            RS_SurveyEntry sEntry = entryMap.get(buildingName);
            switch (entry.getType()) {
                case BUILT:
                    sEntry.setBuilt(entry.getValue());
                    break;
                case IN_PROGRESS:
                    sEntry.setInProgress(entry.getValue());
            }
        }
        return entryMap.values();
    }

    public static RS_Survey fromSurvey(final Survey survey, final boolean full) {
        return full ? new RS_Survey(survey) : new RS_Survey(survey.getId(), RS_Province.fromProvince(survey.getProvince(), false));
    }

    public Long getId() {
        return id;
    }

    public RS_Province getProvince() {
        return province;
    }

    public List<RS_SurveyEntry> getEntries() {
        return toEmptyListIfNull(entries);
    }

    public int getBuilt() {
        return firstNonNull(built, 0);
    }

    public int getInProgress() {
        return firstNonNull(inProgress, 0);
    }

    public String getExportLine() {
        return exportLine;
    }

    public String getSavedBy() {
        return savedBy;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public Integer getAccuracy() {
        return accuracy;
    }

}
