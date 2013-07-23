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
import database.models.Wait;
import tools.validation.ExistsInDB;
import web.validation.Add;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement(name = "Wait")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Wait implements HasNumericId {
    /**
     * The id for this wait. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("users/wait/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user who is waiting. For display purposes only as the user is taken from the context (who is logged in), so this can be safely left out
     * in requests.
     */
    @XmlElement(name = "User")
    private RS_User user;

    /**
     * The user being waited for.
     */
    @NotNull(message = "The waiting for user must not be null", groups = {Add.class})
    @ExistsInDB(entity = BotUser.class, message = "No such user", groups = {Add.class})
    @XmlElement(required = true, name = "WaitingFor")
    private RS_User waitingFor;

    public RS_Wait() {
    }

    private RS_Wait(final Long id, final RS_User user, final RS_User waitingFor) {
        this.id = id;
        this.user = user;
        this.waitingFor = waitingFor;
    }

    public static RS_Wait fromWait(final Wait wait) {
        return new RS_Wait(wait.getId(), RS_User.fromBotUser(wait.getUser(), false), RS_User.fromBotUser(wait.getWaitFor(), false));
    }

    public Long getId() {
        return id;
    }

    public RS_User getUser() {
        return user;
    }

    public RS_User getWaitingFor() {
        return waitingFor;
    }
}
