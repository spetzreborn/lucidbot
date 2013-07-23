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
import database.models.Build;
import database.models.BuildEntry;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ValidBindings;
import web.tools.ISODateTimeAdapter;
import web.tools.MinimumOneRaceOrOnePersonalityBindingsResolver;
import web.validation.Add;
import web.validation.Update;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;
import static com.google.common.base.Objects.firstNonNull;

@XmlRootElement(name = "Build")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Build implements HasNumericId {
    /**
     * The id for this build. The id is set by the database, so clients will only use it in the URL's.
     * <p/>
     * The server will simply ignore this value if you send it in with some request.
     */
    @XmlElement(name = "ID")
    private Long id;

    /**
     * A convenience link to this entity. Only used for navigation.
     * <p/>
     * The server will simply ignore this value if you send it in with some request.
     */
    @Ref("builds/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The type of this build. This is basically just a name, so you can call it whatever that you feel describe when it will be used.
     * <p/>
     * Not updatable, so if you want to change it, remove this build and add a new one. As such you can leave this value out when doing
     * updates, but it's mandatory for adds.
     */
    @NotEmpty(message = "The build type must not be null or empty", groups = {Add.class})
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * The land target for the build.
     * <p/>
     * Updatable and is always overwritten, so if you want existing values to remain, send them in again. Leaving this out is the same as
     * specifying the value 0.
     */
    @Min(value = 0, groups = {Add.class, Update.class})
    @XmlElement(name = "OSPA")
    private Integer land;

    /**
     * The OSPA target for the build.
     * <p/>
     * Updatable and is always overwritten, so if you want existing values to remain, send them in again. Leaving this out is the same as
     * specifying the value 0.
     */
    @DecimalMin(value = "0", groups = {Add.class, Update.class})
    @XmlElement(name = "OSPA")
    private BigDecimal ospa = new BigDecimal(0);

    /**
     * The DSPA target for the build.
     * <p/>
     * Updatable and is always overwritten, so if you want existing values to remain, send them in again. Leaving this out is the same as
     * specifying the value 0.
     */
    @DecimalMin(value = "0", groups = {Add.class, Update.class})
    @XmlElement(name = "DSPA")
    private BigDecimal dspa = new BigDecimal(0);

    /**
     * The EPA target for the build.
     * <p/>
     * Updatable and is always overwritten, so if you want existing values to remain, send them in again. Leaving this out is the same as
     * specifying the value 0.
     */
    @DecimalMin(value = "0", groups = {Add.class, Update.class})
    @XmlElement(name = "EPA")
    private BigDecimal epa = new BigDecimal(0);

    /**
     * The TPA target for the build.
     * <p/>
     * Updatable and is always overwritten, so if you want existing values to remain, send them in again. Leaving this out is the same as
     * specifying the value 0.
     */
    @DecimalMin(value = "0", groups = {Add.class, Update.class})
    @XmlElement(name = "TPA")
    private BigDecimal tpa = new BigDecimal(0);

    /**
     * The WPA target for the build.
     * <p/>
     * Updatable and is always overwritten, so if you want existing values to remain, send them in again. Leaving this out is the same as
     * specifying the value 0.
     */
    @DecimalMin(value = "0", groups = {Add.class, Update.class})
    @XmlElement(name = "WPA")
    private BigDecimal wpa = new BigDecimal(0);

    /**
     * The BPA target for the build.
     * <p/>
     * Updatable and is always overwritten, so if you want existing values to remain, send them in again. Leaving this out is the same as
     * specifying the value 0.
     */
    @DecimalMin(value = "0", groups = {Add.class, Update.class})
    @XmlElement(name = "BPA")
    private BigDecimal bpa = new BigDecimal(0);

    /**
     * The building percentages for this build.
     * <p/>
     * This is updatable, so always send in all of them unless you want them to disappear on updates.
     */
    @Valid
    @XmlElementWrapper(required = true, name = "Buildings")
    @XmlElement(name = "BuildingEntry")
    private List<RS_BuildEntry> buildings;

    /**
     * The date the build was added.
     * <p/>
     * Not updatable and is set automatically on add operations, so never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Added")
    private Date added;

    /**
     * Who added the build. Is mandatory for add operations.
     * <p/>
     * Not updatable, and therefore ignored on update requests.
     */
    @NotEmpty(message = "AddedBy must not be null or empty", groups = {Add.class})
    @XmlElement(name = "AddedBy")
    private String addedBy;

    /**
     * The bindings for the build. Requires at least one race or one personality to be bound, or it's considered invalid. Mandatory for add operations.
     * <p/>
     * Not updatable, and therefore ignored on update requests.
     */
    @NotNull(message = "Bindings may not be null", groups = {Add.class})
    @ValidBindings(minimumEntityBindingsResolver = MinimumOneRaceOrOnePersonalityBindingsResolver.class, message = "Invalid bindings",
            groups = {Add.class})
    @XmlElement(required = true, name = "Bindings")
    private RS_Bindings bindings;

    public RS_Build() {
    }

    private RS_Build(final Long id,
                     final String type,
                     final int land,
                     final double ospa,
                     final double dspa,
                     final double epa,
                     final double tpa,
                     final double wpa,
                     final double bpa,
                     final List<RS_BuildEntry> buildings,
                     final Date added,
                     final String addedBy,
                     final RS_Bindings bindings) {
        this.id = id;
        this.type = type;
        this.land = land;
        this.ospa = BigDecimal.valueOf(ospa);
        this.dspa = BigDecimal.valueOf(dspa);
        this.epa = BigDecimal.valueOf(epa);
        this.tpa = BigDecimal.valueOf(tpa);
        this.wpa = BigDecimal.valueOf(wpa);
        this.bpa = BigDecimal.valueOf(bpa);
        this.buildings = buildings;
        this.added = added;
        this.addedBy = addedBy;
        this.bindings = bindings;
    }

    public static RS_Build fromBuild(final Build build) {
        List<RS_BuildEntry> entries = new ArrayList<>(build.getBuildings().size());
        for (BuildEntry buildEntry : build.getBuildings()) {
            entries.add(RS_BuildEntry.fromBuildEntry(buildEntry));
        }
        return new RS_Build(build.getId(), build.getType(), build.getLand(), build.getOspa(), build.getDspa(), build.getEpa(), build.getTpa(),
                build.getWpa(), build.getBpa(), entries, build.getAdded(), build.getAddedBy(),
                RS_Bindings.fromBindings(build.getBindings()));
    }

    public static void toBuild(final Build build, final RS_Build updatedBuild) {
        build.setBpa(updatedBuild.getBpa().doubleValue());
        build.setDspa(updatedBuild.getDspa().doubleValue());
        build.setEpa(updatedBuild.getEpa().doubleValue());
        build.setOspa(updatedBuild.getOspa().doubleValue());
        build.setTpa(updatedBuild.getTpa().doubleValue());
        build.setWpa(updatedBuild.getWpa().doubleValue());
        build.setLand(updatedBuild.getLand());
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getLand() {
        return firstNonNull(land, 0);
    }

    public BigDecimal getOspa() {
        return firstNonNull(ospa, BigDecimal.ZERO);
    }

    public BigDecimal getDspa() {
        return firstNonNull(dspa, BigDecimal.ZERO);
    }

    public BigDecimal getEpa() {
        return firstNonNull(epa, BigDecimal.ZERO);
    }

    public BigDecimal getTpa() {
        return firstNonNull(tpa, BigDecimal.ZERO);
    }

    public BigDecimal getWpa() {
        return firstNonNull(wpa, BigDecimal.ZERO);
    }

    public BigDecimal getBpa() {
        return firstNonNull(bpa, BigDecimal.ZERO);
    }

    public List<RS_BuildEntry> getBuildings() {
        return toEmptyListIfNull(buildings);
    }

    public Date getAdded() {
        return added;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public RS_Bindings getBindings() {
        return bindings;
    }

}
