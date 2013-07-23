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
import database.models.UserActivities;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.Past;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "UserActivities")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_UserActivities implements HasNumericId {
    /**
     * The id for this activities object. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("users/activities/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user whose activites this is. For display purposes only, need not be specified in requests.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "User")
    private RS_User user;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastActivity")
    private Date lastActivity;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastSeen")
    private Date lastSeen;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastBuildCheck")
    private Date lastBuildCheck;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastOrdersCheck")
    private Date lastOrdersCheck;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastAidCheck")
    private Date lastAidCheck;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastEventsCheck")
    private Date lastEventsCheck;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastWaveCheck")
    private Date lastWaveCheck;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastTargetsCheck")
    private Date lastTargetsCheck;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastNapsCheck")
    private Date lastNapsCheck;

    /**
     * Tracks user activity. Leave out if you don't want to update it (null values are ignored during update operations)
     */
    @Past(message = "Date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastNotesCheck")
    private Date lastNotesCheck;

    public RS_UserActivities() {
    }

    private RS_UserActivities(final Long id,
                              final RS_User user,
                              final Date lastActivity,
                              final Date lastSeen,
                              final Date lastBuildCheck,
                              final Date lastOrdersCheck,
                              final Date lastAidCheck,
                              final Date lastEventsCheck,
                              final Date lastWaveCheck,
                              final Date lastTargetsCheck,
                              final Date lastNapsCheck,
                              final Date lastNotesCheck) {
        this.id = id;
        this.user = user;
        this.lastActivity = lastActivity;
        this.lastSeen = lastSeen;
        this.lastBuildCheck = lastBuildCheck;
        this.lastOrdersCheck = lastOrdersCheck;
        this.lastAidCheck = lastAidCheck;
        this.lastEventsCheck = lastEventsCheck;
        this.lastWaveCheck = lastWaveCheck;
        this.lastTargetsCheck = lastTargetsCheck;
        this.lastNapsCheck = lastNapsCheck;
        this.lastNotesCheck = lastNotesCheck;
    }

    public static RS_UserActivities fromUserActivities(final UserActivities activities) {
        return new RS_UserActivities(activities.getId(), RS_User.fromBotUser(activities.getUser(), false), activities.getLastActivity(),
                activities.getLastSeen(), activities.getLastBuildCheck(), activities.getLastOrdersCheck(),
                activities.getLastAidCheck(), activities.getLastEventsCheck(), activities.getLastWaveCheck(),
                activities.getLastTargetsCheck(), activities.getLastNapsCheck(), activities.getLastNotesCheck());
    }

    public static void toUserActivities(final UserActivities activities, final RS_UserActivities updatedActivities) {
        if (updatedActivities.lastActivity != null) activities.setLastActivity(updatedActivities.lastActivity);
        if (updatedActivities.lastAidCheck != null) activities.setLastAidCheck(updatedActivities.lastAidCheck);
        if (updatedActivities.lastBuildCheck != null) activities.setLastBuildCheck(updatedActivities.lastBuildCheck);
        if (updatedActivities.lastEventsCheck != null) activities.setLastEventsCheck(updatedActivities.lastEventsCheck);
        if (updatedActivities.lastNapsCheck != null) activities.setLastNapsCheck(updatedActivities.lastNapsCheck);
        if (updatedActivities.lastNotesCheck != null) activities.setLastNotesCheck(updatedActivities.lastNotesCheck);
        if (updatedActivities.lastOrdersCheck != null) activities.setLastOrdersCheck(updatedActivities.lastOrdersCheck);
        if (updatedActivities.lastSeen != null) activities.setLastSeen(updatedActivities.lastSeen);
        if (updatedActivities.lastTargetsCheck != null)
            activities.setLastTargetsCheck(updatedActivities.lastTargetsCheck);
        if (updatedActivities.lastWaveCheck != null) activities.setLastWaveCheck(updatedActivities.lastWaveCheck);
    }

    public Long getId() {
        return id;
    }

    public RS_User getUser() {
        return user;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public Date getLastBuildCheck() {
        return lastBuildCheck;
    }

    public Date getLastOrdersCheck() {
        return lastOrdersCheck;
    }

    public Date getLastAidCheck() {
        return lastAidCheck;
    }

    public Date getLastEventsCheck() {
        return lastEventsCheck;
    }

    public Date getLastWaveCheck() {
        return lastWaveCheck;
    }

    public Date getLastTargetsCheck() {
        return lastTargetsCheck;
    }

    public Date getLastNapsCheck() {
        return lastNapsCheck;
    }

    public Date getLastNotesCheck() {
        return lastNotesCheck;
    }
}
