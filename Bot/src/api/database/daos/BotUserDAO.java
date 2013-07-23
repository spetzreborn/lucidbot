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

package api.database.daos;

import api.database.AbstractDAO;
import api.database.Transactional;
import api.database.models.BotUser;
import api.database.models.Nickname;
import api.settings.PropertiesCollection;
import com.google.inject.Provider;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.mindrot.jbcrypt.BCrypt;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static api.settings.PropertiesConfig.DEFAULT_PASSWORD_ACCESS_ENABLED;
import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class BotUserDAO extends AbstractDAO<BotUser> {
    private final NicknameDAO nicknameDAO;
    private final PropertiesCollection properties;

    @Inject
    public BotUserDAO(final Provider<Session> sessionProvider,
                      final NicknameDAO nicknameDAO,
                      final PropertiesCollection properties) {
        super(BotUser.class, sessionProvider);
        this.nicknameDAO = nicknameDAO;
        this.properties = properties;
    }

    @Transactional
    @Nullable
    public BotUser getUser(final long id) {
        return get(Restrictions.idEq(id));
    }

    /**
     * Looks for a user who owns the specified nickname
     *
     * @param nick the nickname (NOT case sensitive)
     * @return the matching BotUser, or null if no user is using the specified nickname
     */
    @Transactional
    @Nullable
    public BotUser getUser(final String nick) {
        Nickname matchingNick = nicknameDAO.getNickname(checkNotNull(nick));
        return matchingNick == null ? null : matchingNick.getUser();
    }

    @Transactional
    public List<BotUser> getUsers(final Long... ids) {
        return find(Restrictions.in("id", ids));
    }

    /**
     * @return a Collection of all users
     */
    @Transactional
    public Collection<BotUser> getAllUsers() {
        return find();
    }

    /**
     * @return a Collection of all admin users
     */
    @Transactional
    public Collection<BotUser> getAdminUsers() {
        return find(Restrictions.eq("isAdmin", true));
    }

    /**
     * Checks if the specified password matches that which is saved for the user. Note that if the default password
     * access property is set to false, this will return false for the default password, even if it is actually a match
     *
     * @param nick     the nick of the user (NOT case sensitive)
     * @param password the password to check if correct
     * @return true if a user with the specified nick exists and the password matches
     */
    @Transactional
    public boolean passwordMatches(final String nick, final String password) {
        BotUser user = getUser(nick);
        if (user == null) return false;

        boolean isNonAllowedPassword = !properties.getBoolean(DEFAULT_PASSWORD_ACCESS_ENABLED) && "password".equals(password);
        return !isNonAllowedPassword && BCrypt.checkpw(checkNotNull(password), user.getPassword());
    }

    /**
     * Finds the closest matching user for the specified nickname. Closest matching means
     * the nickname specified here has to be a part of someone's actual nick for this not to return null
     *
     * @param nick the nickname (NOT case sensitive)
     * @return the best matching user, or null if no match is found
     */
    @Transactional
    @Nullable
    public BotUser getClosestMatch(final String nick) {
        Nickname bestMatch = nicknameDAO.getClosestMatch(checkNotNull(nick));
        return bestMatch == null ? null : bestMatch.getUser();
    }

    /**
     * Formats the specified date using the user's time zone
     *
     * @param nick the nickname of the user (NOT case sensitive)
     * @param date the Date to format
     * @return the Date in a String format (ISO formatted in the user's time zone). Returns null if the user isn't found
     */
    @Transactional
    @Nullable
    public String getFormattedDateInUsersTimezone(final String nick, final Date date) {
        BotUser user = getUser(checkNotNull(nick));
        if (user == null) return null;
        return user.getDateInUsersLocalTime(checkNotNull(date));
    }
}
