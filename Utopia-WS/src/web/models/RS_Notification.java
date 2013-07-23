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
import database.models.Notification;
import database.models.NotificationMethod;
import database.models.NotificationType;
import tools.validation.ExistsInDB;
import tools.validation.IsValidEnumName;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement(name = "Notification")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Notification implements HasNumericId {
    /**
     * The id for this notification. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("notifications/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user this notification is for. Only needs to be sent in for situations where an admin is adding notifications for another user,
     * otherwise this is resolved from the context (who is logged in). The user object only needs to be populated with the id, nothing else.
     */
    @ExistsInDB(entity = BotUser.class, optional = true, message = "No such user")
    @XmlElement(name = "User")
    private RS_User user;

    /**
     * The type of notification. Works the same as on IRC.
     * <p/>
     * This value is not updatable, so new type => new notification.
     */
    @NotNull(message = "The type must not be null")
    @IsValidEnumName(enumType = NotificationType.class, message = "No such notification type")
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * The method of notification. Works the same as on IRC.
     * <p/>
     * This value is not updatable, so new method => new notification.
     */
    @NotNull(message = "The method must not be null")
    @IsValidEnumName(enumType = NotificationMethod.class, message = "No such notification method")
    @XmlElement(required = true, name = "Method")
    private String method;

    public RS_Notification() {
    }

    private RS_Notification(final Long id, final RS_User user, final String type, final String method) {
        this.id = id;
        this.user = user;
        this.type = type;
        this.method = method;
    }

    public static RS_Notification fromNotification(final Notification notification) {
        return new RS_Notification(notification.getId(), RS_User.fromBotUser(notification.getUser(), false),
                notification.getType().getName(), notification.getMethod().getName());
    }

    public Long getId() {
        return id;
    }

    public RS_User getUser() {
        return user;
    }

    public String getType() {
        return type;
    }

    public String getMethod() {
        return method;
    }
}
