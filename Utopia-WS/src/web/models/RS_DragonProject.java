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
import com.google.common.collect.Lists;
import com.sun.jersey.server.linking.Ref;
import database.models.DragonAction;
import database.models.DragonProject;
import database.models.DragonProjectType;
import tools.validation.IsValidEnumName;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;
import static com.google.common.base.Objects.firstNonNull;

@XmlRootElement(name = "DragonProject")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_DragonProject implements HasNumericId {
    /**
     * The id for this dragon project. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("dragons/projects/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The type of dragon project. Uses the same names as on IRC. Mandatory for add operations.
     * <p/>
     * Not updatable, so may be left out on update operations as it will be ignored anyway.
     */
    @NotNull(message = "Dragon project type must be specified", groups = {Add.class})
    @IsValidEnumName(enumType = DragonProjectType.class, message = "No such dragon project type", groups = {Add.class})
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * Tbe original health/cost of the project. Mandatory for add operations.
     * <p/>
     * Not updatable, so may be left out on update operations as it will be ignored anyway.
     */
    @NotNull(message = "Original status must not be null", groups = {Add.class})
    @Min(value = 1, message = "Original status must be a positive number", groups = {Add.class})
    @XmlElement(required = true, name = "OriginalStatus")
    private Integer originalStatus;

    /**
     * The current health/cost of the project.
     * <p/>
     * Updatable, so should be specified in each request or the data will be lost.
     */
    @NotNull(message = "Current status must not be null", groups = {Add.class, Update.class})
    @Min(value = 0, message = "Current status cannot be below 0", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "CurrentStatus")
    private Integer currentStatus;

    /**
     * The date when the project was first added.
     * <p/>
     * Not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Added")
    private Date added;

    /**
     * The date when the project status was last updated. Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     * <p/>
     * Updatable, so it needs to be specified on both all update operations (it's set automatically on add though). The date must be in the past.
     */
    @NotNull(message = "The last updated date must be specified", groups = {Update.class})
    @Past(message = "The last updated date must be in the past", groups = {Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, name = "Updated")
    private Date updated;

    /**
     * A list of dragon actions, meaning donations or killings. Only for display purposes and is ignored if sent in (there are seperate requests for
     * manipulating this information).
     */
    @XmlElementWrapper(name = "Actions")
    @XmlElement(name = "Action")
    private List<RS_DragonAction> actions;

    public RS_DragonProject() {
    }

    private RS_DragonProject(final Long id,
                             final String type,
                             final int originalStatus,
                             final int currentStatus,
                             final Date added,
                             final Date updated,
                             final List<RS_DragonAction> actions) {
        this.id = id;
        this.type = type;
        this.originalStatus = originalStatus;
        this.currentStatus = currentStatus;
        this.added = added;
        this.updated = updated;
        this.actions = actions;
    }

    public static RS_DragonProject fromDragonProject(final DragonProject project, final boolean full) {
        List<RS_DragonAction> actions = Lists.newArrayList();
        if (full) {
            for (DragonAction action : project.getActions()) {
                actions.add(RS_DragonAction.fromDragonAction(action));
            }
        }
        return new RS_DragonProject(project.getId(), project.getType().getName(), project.getOriginalStatus(), project.getStatus(),
                project.getCreated(), project.getUpdated(), actions);
    }

    public static void toDragonProject(final DragonProject project, final RS_DragonProject updatedProject) {
        project.setStatus(updatedProject.currentStatus);
        project.setUpdated(updatedProject.updated);
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public int getOriginalStatus() {
        return firstNonNull(originalStatus, 0);
    }

    public int getCurrentStatus() {
        return firstNonNull(currentStatus, 0);
    }

    public Date getAdded() {
        return added;
    }

    public Date getUpdated() {
        return updated;
    }

    public List<RS_DragonAction> getActions() {
        return toEmptyListIfNull(actions);
    }

}
