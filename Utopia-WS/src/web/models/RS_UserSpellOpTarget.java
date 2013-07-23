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
import database.models.Province;
import database.models.UserSpellOpTarget;
import tools.validation.ExistsInDB;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement(name = "UserSpellOpTarget")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_UserSpellOpTarget implements HasNumericId {
    /**
     * The id for this target. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("users/spelloptargets/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user. For display purposes only, so it never needs to be specified. On add operations the user is taken
     * from the context (who is logged in) automatically, since you can't post things for other users.
     */
    @XmlElement(name = "User")
    private RS_User user;

    /**
     * The target. You do not have to send in a whole province object here, as the id is the only value that will
     * be used. That id must, however, point to an actual province in the database.
     */
    @NotNull(message = "The target must not be null", groups = {Add.class, Update.class})
    @ExistsInDB(entity = Province.class, message = "No such target", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Target")
    private RS_Province target;

    public RS_UserSpellOpTarget() {
    }

    private RS_UserSpellOpTarget(final Long id, final RS_User user, final RS_Province target) {
        this.id = id;
        this.user = user;
        this.target = target;
    }

    public static RS_UserSpellOpTarget fromUserSpellOpTarget(final UserSpellOpTarget target) {
        return new RS_UserSpellOpTarget(target.getId(), RS_User.fromBotUser(target.getUser(), false),
                RS_Province.fromProvince(target.getTarget(), false));
    }

    public Long getId() {
        return id;
    }

    public RS_User getUser() {
        return user;
    }

    public RS_Province getTarget() {
        return target;
    }
}
