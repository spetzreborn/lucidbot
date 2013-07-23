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
import database.models.ScienceType;

import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlType(name = "ScienceType")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_ScienceType implements HasNumericId {
    @XmlElement(name = "ID")
    private Long id;

    @Ref("sciences/{id}")
    @XmlElement(name = "Link")
    private URI link;

    @XmlElement(name = "Name")
    private String name;

    @XmlElement(name = "AngelName")
    private String angelName;

    @XmlElement(name = "Description")
    private String description;

    @XmlElement(name = "ResultFactor")
    private Double resultFactor;

    @XmlElementWrapper(name = "Bonuses")
    @XmlElement(name = "Bonus")
    private List<RS_Bonus> bonuses;

    public RS_ScienceType() {
    }

    private RS_ScienceType(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    private RS_ScienceType(final ScienceType scienceType) {
        this(scienceType.getId(), scienceType.getName());
        this.angelName = scienceType.getAngelName();
        this.description = scienceType.getDescription();
        this.resultFactor = scienceType.getResultFactor();
        this.bonuses = new ArrayList<>();
        for (Bonus bonus : scienceType.getBonuses()) {
            this.bonuses.add(RS_Bonus.fromBonus(bonus));
        }
    }

    public static RS_ScienceType fromScienceType(final ScienceType scienceType, final boolean full) {
        return full ? new RS_ScienceType(scienceType) : new RS_ScienceType(scienceType.getId(), scienceType.getName());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAngelName() {
        return angelName;
    }

    public String getDescription() {
        return description;
    }

    public Double getResultFactor() {
        return resultFactor;
    }

    public List<RS_Bonus> getBonuses() {
        return toEmptyListIfNull(bonuses);
    }
}
