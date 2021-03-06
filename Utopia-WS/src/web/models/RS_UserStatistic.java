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

import api.database.models.UserStatistic;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static com.google.common.base.Objects.firstNonNull;

@XmlRootElement(name = "UserStatistic")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_UserStatistic {
    /**
     * The user whose statistics this is.
     */
    @XmlElement(required = true, name = "User")
    private RS_User user;

    /**
     * The type of statistic
     */
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * The value of the statistic
     */
    @XmlElement(required = true, name = "Value")
    private Integer value;

    public RS_UserStatistic() {
    }

    private RS_UserStatistic(final RS_User user, final String type, final int value) {
        this.user = user;
        this.type = type;
        this.value = value;
    }

    public static RS_UserStatistic fromUserStatistic(final UserStatistic statistic) {
        return new RS_UserStatistic(RS_User.fromBotUser(statistic.getUser(), false), statistic.getType(), statistic.getAmount());
    }

    public RS_User getUser() {
        return user;
    }

    public String getType() {
        return type;
    }

    public int getValue() {
        return firstNonNull(value, 0);
    }
}
