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
import database.models.DurationOp;
import database.models.OpType;
import database.models.Province;
import tools.validation.ExistsInDB;
import web.tools.ISODateTimeAdapter;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "DurationOp")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_DurationOp implements HasNumericId {
    /**
     * The id for this op. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("ops/duration/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user doing the op. Only the id needs to be populated on the user object.
     */
    @NotNull(message = "The committing user must not be null")
    @ExistsInDB(entity = BotUser.class, message = "No such user")
    @XmlElement(required = true, name = "Committer")
    private RS_User committer;

    /**
     * The target province. Only the id needs to be populated on the province object.
     */
    @NotNull(message = "The target province must not be null")
    @ExistsInDB(entity = Province.class, message = "No such province")
    @XmlElement(required = true, name = "Province")
    private RS_Province province;

    /**
     * The date and time at which the op will expire. Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     */
    @NotNull(message = "The expiry date must not be null")
    @Future(message = "The expiry date must be in the future")
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, name = "Expires")
    private Date expires;

    /**
     * The type of op this is. Only the id needs to be populated on the op type object.
     */
    @NotNull(message = "The op type must not be null")
    @ExistsInDB(entity = OpType.class, message = "No such op type")
    @XmlElement(required = true, name = "Type")
    private RS_OpType type;

    public RS_DurationOp() {
    }

    private RS_DurationOp(final Long id,
                          final RS_User committer,
                          final RS_Province province,
                          final Date expires,
                          final RS_OpType type) {
        this.id = id;
        this.committer = committer;
        this.province = province;
        this.expires = expires;
        this.type = type;
    }

    public static RS_DurationOp fromDurationOp(final DurationOp op) {
        return new RS_DurationOp(op.getId(), RS_User.fromBotUser(op.getCommitter(), false),
                RS_Province.fromProvince(op.getProvince(), false), op.getExpires(),
                RS_OpType.fromOpType(op.getType(), false));
    }

    @Override
    public Long getId() {
        return id;
    }

    public RS_User getCommitter() {
        return committer;
    }

    public RS_Province getProvince() {
        return province;
    }

    public Date getExpires() {
        return expires;
    }

    public RS_OpType getType() {
        return type;
    }
}
