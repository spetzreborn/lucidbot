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
import database.models.TickChannelMessage;
import org.hibernate.validator.constraints.NotEmpty;
import web.validation.Add;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement(name = "TickChannelMessage")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_TickChannelMessage implements HasNumericId {
    /**
     * The id for this channel message. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("tickmessages/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The channel the message is for.
     * <p/>
     * This value is not updatable, so it can safely be left out on update operations.
     */
    @NotEmpty(message = "Channel must not be null or empty", groups = {Add.class})
    @XmlElement(required = true, name = "Channel")
    private String channel;

    /**
     * The actual tick message.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @XmlElement(required = true, nillable = true, name = "Message")
    private String message;

    public RS_TickChannelMessage() {
    }

    private RS_TickChannelMessage(final Long id,
                                  final String channel,
                                  final String message) {
        this.id = id;
        this.channel = channel;
        this.message = message;
    }

    public static RS_TickChannelMessage fromTickChannelMessage(final TickChannelMessage message) {
        return new RS_TickChannelMessage(message.getId(), message.getChannel().getName(), message.getMessage());
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }
}
