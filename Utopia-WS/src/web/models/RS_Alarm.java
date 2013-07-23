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
import api.database.models.BotUser;
import com.sun.jersey.server.linking.Ref;
import database.models.Alarm;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ExistsInDB;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "Alarm")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Alarm implements HasNumericId {
    /**
     * The id for this aid alarm. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("alarms/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user. You do not have to send in a whole user object here, as the id is the only value that will
     * be used. That id must, however, point to an actual user in the database.
     * <p/>
     * This value is not updatable. That means you can leave it out when only doing update requests. On add requests it's mandatory.
     */
    @NotNull(message = "You must specify a user", groups = {Add.class})
    @ExistsInDB(entity = BotUser.class, message = "There's no such user", groups = {Add.class})
    @XmlElement(required = true, name = "User")
    private RS_User user;

    /**
     * The date and time at which the alarm will go off. Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. you will always have to specify it, and it must be
     * a date & time in the future.
     */
    @NotNull(message = "You must specify an expiry date", groups = {Add.class, Update.class})
    @Future(message = "The expiry date must be in the future", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, name = "Expires")
    private Date expires;

    /**
     * The alarm message that will be displayed when the alarm goes off.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. you will always have to specify it, and it must not be
     * empty or null.
     */
    @NotEmpty(message = "You must specify an alarm message", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Message")
    private String message;

    public RS_Alarm() {
    }

    private RS_Alarm(final Long id, final RS_User user, final Date expires, final String message) {
        this.id = id;
        this.user = user;
        this.expires = expires;
        this.message = message;
    }

    public static RS_Alarm fromAlarm(final Alarm alarm) {
        return new RS_Alarm(alarm.getId(), RS_User.fromBotUser(alarm.getUser(), false), alarm.getAlarmTime(), alarm.getMessage());
    }

    public static void toAlarm(final Alarm alarm, final RS_Alarm updatedAlarm) {
        alarm.setAlarmTime(updatedAlarm.expires);
        alarm.setMessage(updatedAlarm.message);
    }

    @Override
    public Long getId() {
        return id;
    }

    public RS_User getUser() {
        return user;
    }

    public Date getExpires() {
        return expires;
    }

    public String getMessage() {
        return message;
    }
}
