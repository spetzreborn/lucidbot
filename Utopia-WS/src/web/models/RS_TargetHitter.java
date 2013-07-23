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
import tools.validation.ExistsInDB;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static com.google.common.base.Objects.firstNonNull;

@XmlType(name = "TargetHitter")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_TargetHitter {
    /**
     * The target of this hitter. For display purposes only, so doesn't need to be sent in (the target will be in the URL already).
     */
    @XmlElement(required = true, name = "Target")
    private RS_Target target;

    /**
     * The user doing the hit. Only the id needs to be populated on the user object.
     */
    @NotNull(message = "The user must not be null", groups = {Add.class, Update.class})
    @ExistsInDB(entity = BotUser.class, message = "No such user")
    @XmlElement(required = true, name = "User")
    private RS_User user;

    /**
     * The hitters position in the hitter list. This is the only updatable value and therefore needs to be sent in for updates every time.
     */
    @NotNull(message = "Position must not be null", groups = {Add.class, Update.class})
    @Min(value = 1, message = "Position must be a positive number", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Position")
    private Integer position;

    public RS_TargetHitter() {
    }

    RS_TargetHitter(final RS_Target target, final RS_User user, final int position) {
        this.target = target;
        this.user = user;
        this.position = position;
    }

    public RS_Target getTarget() {
        return target;
    }

    public RS_User getUser() {
        return user;
    }

    public int getPosition() {
        return firstNonNull(position, 0);
    }
}
