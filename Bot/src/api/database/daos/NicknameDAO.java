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
import api.database.models.Nickname;
import api.tools.text.StringUtil;
import com.google.inject.Provider;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class NicknameDAO extends AbstractDAO<Nickname> {
    @Inject
    public NicknameDAO(final Provider<Session> sessionProvider) {
        super(Nickname.class, sessionProvider);
    }

    /**
     * @param id the nickname's id
     * @return the corresponding Nickname object, or null if none exists
     */
    @Transactional
    @Nullable
    public Nickname getNickname(final long id) {
        return get(Restrictions.idEq(id));
    }

    /**
     * Searches for the best matching nickname
     *
     * @param name the name to compare to (NOT case sensitive)
     * @return the closest matching nickname, or null if there are no matches
     */
    @Transactional
    @Nullable
    public Nickname getClosestMatch(final String name) {
        List<Nickname> matchingNicks = find(Restrictions.ilike("nickname", '%' + checkNotNull(name) + '%'));
        Nickname bestMatch = null;
        int bestMatchDistance = -1;
        for (final Nickname nick : matchingNicks) {
            int distance = StringUtil.getLevenshteinDistance(name, nick.getNickname());
            if (bestMatchDistance == -1 || distance < bestMatchDistance) {
                bestMatch = nick;
                bestMatchDistance = distance;
            }
        }
        return bestMatch;
    }

    /**
     * @param nick the nickname (NOT case sensitive)
     * @return the corresponding Nickname object, or null if none exists
     */
    @Transactional
    @Nullable
    public Nickname getNickname(final String nick) {
        return get(Restrictions.ilike("nickname", checkNotNull(nick)));
    }

    /**
     * @return all nicknames
     */
    @Transactional
    public Collection<Nickname> getAllNicknames() {
        return find();
    }
}
