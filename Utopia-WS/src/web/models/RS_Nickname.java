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
import api.database.models.Nickname;
import com.sun.jersey.server.linking.Ref;
import org.hibernate.validator.constraints.NotEmpty;
import web.validation.Add;
import web.validation.Update;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement(name = "Nickname")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Nickname implements HasNumericId {
    /**
     * The id for this nickname The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("users/nicknames/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The nickname.
     * <p/>
     * This value is not updatable, so you can't actually edit existing nicknames (delete first and add a new one instead).
     */
    @NotEmpty(message = "The name must not be null or empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Name")
    private String name;

    /**
     * The user the nickname belongs to. For display purposes only, as the user is resolved from the context (who is logged in). No need
     * to send this in at all.
     */
    @XmlElement(name = "User")
    private RS_User user;

    public RS_Nickname() {
    }

    private RS_Nickname(final Long id, final String name, final RS_User user) {
        this.id = id;
        this.name = name;
        this.user = user;
    }

    public static RS_Nickname fromNickname(final Nickname nickname) {
        return new RS_Nickname(nickname.getId(), nickname.getName(), RS_User.fromBotUser(nickname.getUser(), false));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RS_User getUser() {
        return user;
    }
}
