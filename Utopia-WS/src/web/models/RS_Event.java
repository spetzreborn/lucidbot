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
import database.models.AttendanceStatus;
import database.models.Event;
import tools.validation.IsValidEnumName;
import tools.validation.ValidBindings;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlRootElement(name = "Event")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Event implements HasNumericId {
    /**
     * The id for this event. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("events/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The event type (wave or event).
     * <p/>
     * Not updatable, so even though it's mandatory for add operations it may be left out completely for update operations (since it'll be ignored anyway).
     */
    @NotNull(message = "The event type must not be null", groups = {Add.class})
    @IsValidEnumName(enumType = Event.EventType.class, message = "No such event type", groups = {Add.class})
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * The event description, letting people know what the event is about.
     * <p/>
     * This value is updatable and is overwritten in update operations, so always send it in if you want to keep the data. Defaults to an empty String
     * if left out.
     */
    @XmlElement(name = "Description")
    private String description = "";

    /**
     * The date and time at which the event was added.
     * <p/>
     * Not updatable, and is set automatically for add operations, so this never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Added")
    private Date added;

    /**
     * The date and time at which the event will happen. Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request. Only a date & time that's in the future is valid.
     */
    @NotNull(message = "The event time must not be null", groups = {Add.class, Update.class})
    @Future(message = "The event time must be in the future", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, name = "EventTime")
    private Date eventTime;

    /**
     * A list with attendance information. Only for display purposes, and as such will be ignored if sent in. You use separate requests for manipulating
     * this data.
     */
    @XmlElementWrapper(name = "AttendanceList")
    @XmlElement(name = "AttendanceStatus")
    private List<RS_AttendanceStatus> attendance;

    /**
     * Bindings for the event, allowing you to specify which users (as an example) the event is for.
     * <p/>
     * Not updatable, so it will be ignored for update operations. Not mandatory for add operations either.
     */
    @ValidBindings(message = "Invalid bindings", nillable = true, groups = {Add.class})
    @XmlElement(nillable = true, required = true, name = "Bindings")
    private RS_Bindings bindings;

    public RS_Event() {
    }

    public RS_Event(final Long id, final String type) {
        this.id = id;
        this.type = type;
    }

    private RS_Event(final Event event) {
        this.id = event.getId();
        this.type = event.getType().getName();
        this.description = event.getDescription();
        this.added = event.getAdded();
        this.eventTime = event.getEventTime();
        this.attendance = Lists.newArrayList();
        for (AttendanceStatus status : event.getAttendanceInformation()) {
            attendance.add(RS_AttendanceStatus.fromAttendanceStatus(status));
        }
        this.bindings = RS_Bindings.fromBindings(event.getBindings());
    }

    public static RS_Event fromEvent(final Event event, final boolean full) {
        return full ? new RS_Event(event) : new RS_Event(event.getId(), event.getType().getName());
    }

    public static void toEvent(final Event event, final RS_Event updatedEvent) {
        event.setEventTime(updatedEvent.eventTime);
        event.setDescription(updatedEvent.description);
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Date getAdded() {
        return added;
    }

    public Date getEventTime() {
        return eventTime;
    }

    public List<RS_AttendanceStatus> getAttendance() {
        return toEmptyListIfNull(attendance);
    }

    public RS_Bindings getBindings() {
        return bindings;
    }
}
