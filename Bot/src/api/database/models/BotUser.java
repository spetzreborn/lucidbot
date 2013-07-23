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

package api.database.models;

import api.common.HasNumericId;
import api.tools.time.DateFactory;
import api.tools.time.DateUtil;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.*;
import org.mindrot.jbcrypt.BCrypt;

import javax.persistence.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "bot_user")
@NoArgsConstructor
@EqualsAndHashCode(of = "mainNick")
@Getter
@Setter
public final class BotUser implements Comparable<BotUser>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    /**
     * The user's main nick
     */
    @Column(name = "main_nick", updatable = false, nullable = false, unique = true, length = 50)
    private String mainNick;

    /**
     * The password the user uses for the various bot services
     */
    @Column(name = "password", length = 200)
    private String password;

    /**
     * A List of the user's linked nicks (including the main nick)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Nickname> nickList = new ArrayList<>();

    /**
     * Whether this user is an admin or not
     */
    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin;

    /**
     * Whether this user is the owner (super admin) or not
     */
    @Column(name = "is_owner", nullable = false)
    private boolean isOwner;

    /**
     * The user's timezone, +- hours from GMT
     */
    @Column(name = "timezone", nullable = false, length = 10)
    private String timeZone = "0";

    /**
     * The current daylight savings setting, 0 means off, 1 means active
     */
    @Column(name = "dst", nullable = false)
    private int dst;

    /**
     * The country the user is from
     */
    @Column(name = "country", length = 100)
    private String country;

    /**
     * The user's real name
     */
    @Column(name = "real_name", length = 200)
    private String realName;

    /**
     * The user's email address
     */
    @Column(name = "email", length = 200)
    private String email;

    /**
     * The user's GTalk address
     */
    @Column(name = "gtalk", length = 200)
    private String gtalk;

    /**
     * The user's email-to-sms-gateway address
     */
    @Column(name = "sms", length = 200)
    private String sms;

    /**
     * Whether the sms address is confirmed to be working
     */
    @Column(name = "sms_confirmed")
    private boolean smsConfirmed;

    /**
     * The user's current status. Could be something like "Away for 3 hours"
     */
    @Lob
    @Column(name = "status", length = 5000)
    private String status;

    /**
     * The user's contact information
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContactInformation> contactInformation = new ArrayList<>();

    /**
     * The user's stats
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user", orphanRemoval = true)
    private List<UserStatistic> stats = new ArrayList<>();

    public BotUser(final String mainNick, final boolean admin, final boolean owner) {
        this.mainNick = checkNotNull(mainNick);
        this.nickList = Lists.newArrayList(new Nickname(mainNick, this));
        isAdmin = admin;
        isOwner = owner;
    }

    public BotUser(final String mainNick, final boolean admin, final boolean owner, final String timeZone, final int dst,
                   final String country, final String realName, final String email, final String gtalk, final String sms,
                   final boolean smsWorks) {
        this(mainNick, admin, owner);
        this.timeZone = timeZone;
        this.dst = dst;
        this.country = country;
        this.realName = realName;
        this.email = email;
        this.gtalk = gtalk;
        this.sms = sms;
        this.smsConfirmed = smsWorks;
    }

    /**
     * Sets the specified password, or rather the encrypted and salted version of it
     *
     * @param password the password to set
     */
    public void setPassword(final String password) {
        this.password = BCrypt.hashpw(checkNotNull(password), BCrypt.gensalt());
    }

    public void setName(final String name) {
        this.realName = name;
    }

    /**
     * @param date the Date to format
     * @return a String with the specified Date formatted in the ISO date-time format in the user's timezone
     */
    public String getDateInUsersLocalTime(final Date date) {
        DateFormat isoDateTime = DateFactory.getISODateTimeFormat();
        isoDateTime.setTimeZone(TimeZone.getTimeZone("GMT" + getTimeZone()));
        Date dstFixedDate = new Date(date.getTime() + DateUtil.hoursToMillis(getDst()));
        return isoDateTime.format(dstFixedDate);
    }

    /**
     * @param nick the nickname to check
     * @return true if this user is the user who owns the specified nick
     */
    public boolean is(final String nick) {
        if (getMainNick().equalsIgnoreCase(nick)) return true;
        for (Nickname nickname : getNickList()) {
            if (nickname.getNickname().equalsIgnoreCase(nick)) return true;
        }
        return false;
    }

    /**
     * @param nicks the nicknames to check
     * @return true if this user owns one of the specified nicks
     */
    public boolean isOneOf(final String... nicks) {
        for (String nick : nicks) {
            if (is(nick)) return true;
        }
        return false;
    }

    public void incrementStat(final String statType, final int incr) {
        for (UserStatistic statistic : getStats()) {
            if (statType.equalsIgnoreCase(statistic.getType())) {
                statistic.setAmount(statistic.getAmount() + incr);
                return;
            }
        }
        getStats().add(new UserStatistic(this, statType, incr));
    }

    @Override
    public int compareTo(final BotUser o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getMainNick().compareToIgnoreCase(o.getMainNick());
    }
}
