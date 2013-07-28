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
import database.models.PrivateMessage;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ExistsInDB;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "PrivateMessage")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_PrivateMessage implements HasNumericId {
    /**
     * The id for this pm. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("privatemessages/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The date and time at which the pm was sent.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Sent")
    private Date sent;

    /**
     * The recipient of the message
     */
    @NotNull(message = "The recipient user must not be null", groups = {Add.class})
    @ExistsInDB(entity = BotUser.class, message = "No such user", groups = {Add.class})
    @XmlElement(required = true, name = "Recipient")
    private RS_User recipient;

    /**
     * The sender of the pm.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlElement(name = "Sender")
    private String sender;

    /**
     * Whether the message has been read by the recipient
     */
    @XmlElement(name = "Read")
    private Boolean read;

    /**
     * Whether the recipient has archived this message
     */
    @XmlElement(name = "Archived")
    private Boolean archived;

    /**
     * The content of the message.
     */
    @NotEmpty(message = "The message must not be null or empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Message")
    private String message;

    public RS_PrivateMessage() {
    }

    private RS_PrivateMessage(final Long id,
                              final Date sent,
                              final RS_User recipient,
                              final String sender,
                              final boolean read,
                              final boolean archived,
                              final String message) {
        this.id = id;
        this.sent = sent;
        this.recipient = recipient;
        this.sender = sender;
        this.read = read;
        this.archived = archived;
        this.message = message;
    }

    public static RS_PrivateMessage fromPrivateMessage(final PrivateMessage pm) {
        return new RS_PrivateMessage(pm.getId(), pm.getSent(), RS_User.fromBotUser(pm.getRecipient(), false), pm.getSender(), pm.isRead(),
                pm.isArchived(), pm.getMessage());
    }

    public Long getId() {
        return id;
    }

    public Date getSent() {
        return sent;
    }

    public RS_User getRecipient() {
        return recipient;
    }

    public String getSender() {
        return sender;
    }

    public boolean isRead() {
        return read != null && read;
    }

    public boolean isArchived() {
        return archived != null && archived;
    }

    public String getMessage() {
        return message;
    }
}
