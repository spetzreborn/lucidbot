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
import database.models.SoS;
import database.models.SoSEntry;
import web.tools.ISODateTimeAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlRootElement(name = "SoS")
@XmlType(name = "SoS")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_SoS implements HasNumericId {
    @XmlElement(name = "ID")
    private Long id;

    @Ref("intel/soss/{id}")
    @XmlElement(name = "Link")
    private URI link;

    @XmlElement(required = true, name = "Province")
    private RS_Province province;

    @XmlElementWrapper(nillable = true, name = "Entries")
    @XmlElement(required = true, name = "SoSEntry")
    private List<RS_SoSEntry> entries;

    @XmlElement(name = "Books")
    private Integer books;

    @XmlElement(name = "ExportLine")
    private String exportLine;

    @XmlElement(name = "SavedBy")
    private String savedBy;

    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastUpdated")
    private Date lastUpdated;

    @XmlElement(name = "Accuracy")
    private Integer accuracy;

    public RS_SoS() {
    }

    private RS_SoS(final Long id, final RS_Province province) {
        this.id = id;
        this.province = province;
    }

    private RS_SoS(final SoS sos) {
        this(sos.getId(), RS_Province.fromProvince(sos.getProvince(), false));
        this.entries = new ArrayList<>();
        for (SoSEntry entry : sos.getSciences()) {
            this.entries.add(new RS_SoSEntry(entry));
        }
        this.books = sos.getTotalBooks();
        this.exportLine = sos.getExportLine();
        this.savedBy = sos.getSavedBy();
        this.lastUpdated = sos.getLastUpdated();
        this.accuracy = sos.getAccuracy();
    }

    public static RS_SoS fromSoS(final SoS sos, final boolean full) {
        return full ? new RS_SoS(sos) : new RS_SoS(sos.getId(), RS_Province.fromProvince(sos.getProvince(), false));
    }

    public Long getId() {
        return id;
    }

    public RS_Province getProvince() {
        return province;
    }

    public List<RS_SoSEntry> getEntries() {
        return toEmptyListIfNull(entries);
    }

    public Integer getBooks() {
        return books;
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
