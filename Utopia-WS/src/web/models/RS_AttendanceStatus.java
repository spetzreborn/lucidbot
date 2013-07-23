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

import api.database.models.BotUser;
import database.models.AttendanceStatus;
import database.models.AttendanceType;
import tools.validation.ExistsInDB;
import tools.validation.IsValidEnumName;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AttendanceStatus")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_AttendanceStatus {
    /**
     * The user the attendance is for. Only id needs to be specified in the user object, not a complete object.
     * <p/>
     * Not updatable, but is required anyway so that updates can identify which user it's for.
     */
    @NotNull(message = "The user must not be null", groups = {Add.class, Update.class})
    @ExistsInDB(entity = BotUser.class, message = "No such user", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "User")
    private RS_User user;

    /**
     * The event the attendance is for. For display purposes only, as the event id is specified in the URL, so this never needs to be specified.
     */
    @XmlElement(required = true, name = "Event")
    private RS_Event event;

    /**
     * The attendance type for the user (attending, late etc.). Works the same as on IRC.
     * <p/>
     * Updatable, so it always needs to be specified as it's always being overwritten.
     */
    @NotNull(message = "The attendance type must not be null", groups = {Add.class, Update.class})
    @IsValidEnumName(enumType = AttendanceType.class, message = "No such attendance type", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * Any extra details about the attendance, for example used to specify exactly how late the user will be.
     * <p/>
     * Updatable, so it always needs to be specified as it's always being overwritten. May be left out completely if no details need to be added.
     */
    @XmlElement(name = "Details")
    private String details = "";

    public RS_AttendanceStatus() {
    }

    private RS_AttendanceStatus(final RS_User user, final RS_Event event, final String type, final String details) {
        this.user = user;
        this.event = event;
        this.type = type;
        this.details = details;
    }

    public static RS_AttendanceStatus fromAttendanceStatus(final AttendanceStatus status) {
        return new RS_AttendanceStatus(RS_User.fromBotUser(status.getUser(), false), RS_Event.fromEvent(status.getEvent(), false),
                status.getType().getName(), status.getDetails());
    }

    public RS_User getUser() {
        return user;
    }

    public RS_Event getEvent() {
        return event;
    }

    public String getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }
}
