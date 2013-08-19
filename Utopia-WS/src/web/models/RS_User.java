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
import api.database.models.ContactInformation;
import api.database.models.Nickname;
import com.sun.jersey.server.linking.Ref;
import org.hibernate.validator.constraints.NotEmpty;
import web.validation.Add;
import web.validation.Update;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.*;

import static api.tools.collections.ListUtil.toEmptyListIfNull;
import static api.tools.text.StringUtil.lowerCase;

@XmlRootElement(name = "User")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_User implements HasNumericId {
    /**
     * The id for this user. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("users/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user's name, or main nick.
     * <p/>
     * This value is not updatable at the moment, so while it's mandatory on add operations, it can be left out on update operations.
     */
    @NotEmpty(message = "Name must not be null or empty", groups = {Add.class})
    @XmlElement(required = true, name = "Name")
    private String name;

    /**
     * A list of all the user's nicknames.
     * <p/>
     * This value is updatable, and will be overwritten every time you make an add or update request. That means if you don't want to lose
     * nicknames, you need to send them all in every time. Also, the main nick will be in this list, and it can never be removed (if you leave it
     * out it will still be saved). Nicknames that are specified here that are already taken by someone else will be ignored.
     */
    @Valid
    @XmlElementWrapper(required = true, name = "Nicknames")
    @XmlElement(name = "Nickname")
    private List<RS_Nickname> nicknames;

    /**
     * Whether the user is an admin or not.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (false).
     */
    @XmlElement(name = "IsAdmin")
    private Boolean admin;

    /**
     * Whether the user is the bot owner or not.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (false).
     */
    @XmlElement(name = "IsOwner")
    private Boolean owner;

    /**
     * The user's time zone. Must always be specified. If you don't know what time zone the user is in, just supply 0 or so.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (false).
     */
    @NotNull(message = "Timezone must not be null", groups = {Add.class, Update.class})
    @Pattern(regexp = "[+-]?(?:[0]\\d|[1][0-3]|\\d)", message = "Invalid time zone", groups = {Add.class, Update.class})
    @XmlElement(name = "TimeZone")
    private String timeZone;

    /**
     * Whether the user has daylight saving time active or not.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (false).
     */
    @XmlElement(name = "DSTIsActive")
    private Boolean dstActive;

    /**
     * The country the user lives in.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (null).
     */
    @XmlElement(name = "Country")
    private String country;

    /**
     * Whether the user is an admin or not.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (null).
     */
    @XmlElement(name = "RealName")
    private String realName;

    /**
     * The user's email address.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (null).
     */
    @XmlElement(name = "EmailAddress")
    private String email;

    /**
     * The user's sms email gateway address.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (null).
     */
    @XmlElement(name = "SMSEmailAddress")
    private String sms;

    /**
     * Whether the user's sms email gateway is confirmed working or not.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (false).
     */
    @XmlElement(name = "SMSConfirmedWorking")
    private Boolean smsConfirmed;

    /**
     * The user's other contact info.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (empty).
     * All values that are missing from this list will be automatically deleted from the user.
     */
    @Valid
    @XmlElementWrapper(required = true, name = "OtherContactInformation")
    @XmlElement(name = "ContactInformation")
    private List<RS_ContactInformation> otherContacts;

    /**
     * A status message for the user. Could for example explain which times the user will be available this week because of vacation or lots to do at work
     * or something like that.
     * <p/>
     * This value is updatable, and needs to be sent in with each request if you don't want it to revert to it's default value (null).
     */
    @XmlElement(name = "Status")
    private String status;

    public RS_User() {
    }

    private RS_User(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    private RS_User(final BotUser user) {
        this(user.getId(), user.getMainNick());
        this.nicknames = new LinkedList<>();
        for (Nickname nickname : user.getNickList()) {
            this.nicknames.add(RS_Nickname.fromNickname(nickname));
        }
        this.admin = user.isAdmin();
        this.owner = user.isOwner();
        this.timeZone = user.getTimeZone();
        this.dstActive = user.getDst() == 1;
        this.country = user.getCountry();
        this.realName = user.getRealName();
        this.email = user.getEmail();
        this.sms = user.getSms();
        this.smsConfirmed = user.isSmsConfirmed();
        this.otherContacts = new LinkedList<>();
        for (ContactInformation information : user.getContactInformation()) {
            otherContacts.add(RS_ContactInformation.fromContactInformation(information));
        }
        this.status = user.getStatus();
    }

    public static RS_User fromBotUser(final BotUser user, final boolean full) {
        return full ? new RS_User(user) : new RS_User(user.getId(), user.getMainNick());
    }

    public static void toBotUser(final BotUser botUser, final RS_User updatedUser) {
        botUser.setAdmin(updatedUser.getAdmin());
        botUser.setOwner(updatedUser.getOwner());
        botUser.setCountry(updatedUser.getCountry());
        botUser.setDst(updatedUser.isDstActive() ? 1 : 0);
        botUser.setEmail(updatedUser.getEmail());
        botUser.setRealName(updatedUser.getRealName());
        botUser.setSms(updatedUser.getSms());
        botUser.setSmsConfirmed(updatedUser.isSmsConfirmed());
        botUser.setTimeZone(updatedUser.getTimeZone());
        botUser.setStatus(updatedUser.getStatus());

        updateContactInfo(botUser, updatedUser);

        updateNicks(botUser, updatedUser);
    }

    private static void updateContactInfo(final BotUser botUser, final RS_User updatedUser) {
        Map<String, String> typeToInfoMap = new HashMap<>();
        for (RS_ContactInformation information : updatedUser.getOtherContactInformation()) {
            typeToInfoMap.put(lowerCase(information.getType()), information.getInfo());
        }
        for (Iterator<ContactInformation> iter = botUser.getContactInformation().iterator(); iter.hasNext(); ) {
            ContactInformation contactInformation = iter.next();
            String key = lowerCase(contactInformation.getInformationType());
            if (typeToInfoMap.containsKey(key)) {
                contactInformation.setInformation(typeToInfoMap.remove(key));
            } else iter.remove();
        }
        for (Map.Entry<String, String> entry : typeToInfoMap.entrySet()) {
            botUser.getContactInformation().add(new ContactInformation(botUser, entry.getKey(), entry.getValue()));
        }
    }

    private static void updateNicks(final BotUser botUser, final RS_User updatedUser) {
        Map<String, String> nickMap = new HashMap<>();
        for (RS_Nickname nickname : updatedUser.getNicknames()) {
            nickMap.put(lowerCase(nickname.getName()), nickname.getName());
        }
        for (Iterator<Nickname> iter = botUser.getNickList().iterator(); iter.hasNext(); ) {
            Nickname nickname = iter.next();
            String key = lowerCase(nickname.getName());
            if (nickMap.containsKey(key)) nickMap.remove(key);
            else if (!key.equalsIgnoreCase(botUser.getMainNick())) iter.remove();
        }
        for (Map.Entry<String, String> entry : nickMap.entrySet()) {
            botUser.getNickList().add(new Nickname(entry.getValue(), botUser));
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<RS_Nickname> getNicknames() {
        return toEmptyListIfNull(nicknames);
    }

    public boolean getAdmin() {
        return admin != null && admin;
    }

    public boolean getOwner() {
        return owner != null && owner;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public boolean isDstActive() {
        return dstActive != null && dstActive;
    }

    public String getCountry() {
        return country;
    }

    public String getRealName() {
        return realName;
    }

    public String getEmail() {
        return email;
    }

    public String getSms() {
        return sms;
    }

    public boolean isSmsConfirmed() {
        return smsConfirmed != null && smsConfirmed;
    }

    public List<RS_ContactInformation> getOtherContactInformation() {
        return toEmptyListIfNull(otherContacts);
    }

    public String getStatus() {
        return status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RS_User rs_user = (RS_User) o;

        if (id != null ? !id.equals(rs_user.id) : rs_user.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
