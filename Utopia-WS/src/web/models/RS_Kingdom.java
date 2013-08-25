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
import database.models.Kingdom;
import database.models.Province;
import web.tools.ISODateTimeAdapter;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

@XmlRootElement(name = "Kingdom")
@XmlType(name = "Kingdom")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Kingdom implements HasNumericId {
    @XmlElement(name = "ID")
    private Long id;

    @Ref("kingdoms/{id}")
    @XmlElement(name = "Link")
    private URI link;

    @XmlElement(name = "Location")
    private String location;

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "Land")
    private Integer land;

    @XmlElement(name = "Networth")
    private Integer networth;

    @XmlElement(name = "Comment")
    private String comment;

    @XmlElement(name = "Dragon")
    private RS_Dragon dragon;

    @XmlElement(name = "NAP")
    private RS_Nap nap;

    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastUpdated")
    private Date lastUpdated;

    @XmlElementWrapper(nillable = true, name = "Provinces")
    @XmlElement(required = true, name = "Province")
    private List<RS_Province> provinces;

    public RS_Kingdom() {
    }

    private RS_Kingdom(final Long id, final String location) {
        this.id = id;
        this.location = location;
    }

    private RS_Kingdom(final Kingdom kingdom) {
        this(kingdom.getId(), kingdom.getLocation());
        this.name = kingdom.getName();
        this.land = kingdom.getTotalLand();
        this.networth = kingdom.getTotalNw();
        this.comment = kingdom.getKdComment();
        this.dragon = kingdom.getDragon() == null ? null : RS_Dragon.fromDragon(kingdom.getDragon(), false);
        this.nap = RS_Nap.fromKingdom(kingdom);
        this.lastUpdated = kingdom.getLastUpdated();
        this.provinces = new ArrayList<>();
        for (Province province : kingdom.getProvinces()) {
            this.provinces.add(RS_Province.fromProvince(province, false));
        }
    }

    /**
     * Constructs a RS_Kingdom from the supplied Kingdom
     *
     * @param kingdom the kingdom to base this RS_Kingdom on
     * @param full    whether to include all info, or just the minimal amount
     * @return a new RS_Kingdom
     */
    public static RS_Kingdom fromKingdom(@Nonnull final Kingdom kingdom, final boolean full) {
        checkNotNull(kingdom);
        return full ? new RS_Kingdom(kingdom) : new RS_Kingdom(kingdom.getId(), kingdom.getLocation());
    }

    public Long getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public int getLand() {
        return firstNonNull(land, 0);
    }

    public int getNetworth() {
        return firstNonNull(networth, 0);
    }

    public String getComment() {
        return comment;
    }

    public RS_Dragon getDragon() {
        return dragon;
    }

    public RS_Nap getNap() {
        return nap;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public List<RS_Province> getProvinces() {
        return toEmptyListIfNull(provinces);
    }
}
