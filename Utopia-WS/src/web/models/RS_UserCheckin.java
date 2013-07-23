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
import database.models.UserCheckIn;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ExistsInDB;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "UserCheckin")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_UserCheckin implements HasNumericId {
    /**
     * The id for this checkin. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("users/checkins/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user checking in. Only the id needs to be populated on the user object.
     * <p/>
     * This is neither updatable nor mandatory, so the only time you'd supply it is if you want to checkin info for some other user when doing an add.
     */
    @ExistsInDB(entity = BotUser.class, optional = true, message = "No such user")
    @XmlElement(name = "User")
    private RS_User user;

    /**
     * The user's province. For display purposes only, need never be specified.
     */
    @XmlElement(name = "Province")
    private RS_Province province;

    /**
     * The actual checkin content.
     * <p/>
     * This value is updatable and is overwritten in update operations, so always send it in if you want to keep the data.
     */
    @NotEmpty(message = "You can't check in nothing", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "CheckedIn")
    private String checkedIn;

    /**
     * When the checkin info was last updated. For display purposes only and set automatically on checkins.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "CheckInTime")
    private Date checkInTime;

    public RS_UserCheckin() {
    }

    private RS_UserCheckin(final Long id, final RS_User user, final RS_Province province, final String checkedIn, final Date checkInTime) {
        this.id = id;
        this.user = user;
        this.province = province;
        this.checkedIn = checkedIn;
        this.checkInTime = checkInTime;
    }

    public static RS_UserCheckin fromUserCheckin(final UserCheckIn checkIn) {
        return new RS_UserCheckin(checkIn.getId(), RS_User.fromBotUser(checkIn.getUser(), false),
                RS_Province.fromProvince(checkIn.getProvince(), false), checkIn.getCheckedIn(), checkIn.getCheckInTime());
    }

    @Override
    public Long getId() {
        return id;
    }

    public RS_User getUser() {
        return user;
    }

    public RS_Province getProvince() {
        return province;
    }

    public String getCheckedIn() {
        return checkedIn;
    }

    public Date getCheckInTime() {
        return checkInTime;
    }
}
