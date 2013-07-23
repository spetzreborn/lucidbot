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

package web.tools;

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import database.daos.PersonalityDAO;
import database.daos.RaceDAO;
import database.models.Bindings;
import database.models.Personality;
import database.models.Race;
import web.models.RS_Bindings;
import web.models.RS_Personality;
import web.models.RS_Race;
import web.models.RS_User;

import javax.inject.Inject;

import static api.tools.collections.CollectionUtil.isEmpty;
import static com.google.common.base.Preconditions.checkNotNull;

public class BindingsParser {
    private final RaceDAO raceDAO;
    private final PersonalityDAO personalityDAO;
    private final BotUserDAO botUserDAO;

    @Inject
    public BindingsParser(final RaceDAO raceDAO, final PersonalityDAO personalityDAO, final BotUserDAO botUserDAO) {
        this.raceDAO = raceDAO;
        this.personalityDAO = personalityDAO;
        this.botUserDAO = botUserDAO;
    }

    public Bindings parse(final RS_Bindings rs_bindings) {
        Bindings bindings = new Bindings();

        if (rs_bindings == null) return bindings;

        addRaces(rs_bindings, bindings);
        addPersonalities(rs_bindings, bindings);
        addUsers(rs_bindings, bindings);
        bindings.setAdminsOnly(rs_bindings.isAdminsOnly());
        bindings.setExpiryDate(rs_bindings.getExpiryDate());
        bindings.setPublishDate(rs_bindings.getPublishDate());
        return bindings;
    }

    private void addRaces(final RS_Bindings rs_bindings, final Bindings bindings) {
        if (isEmpty(rs_bindings.getRaces())) return;

        for (RS_Race rs_race : rs_bindings.getRaces()) {
            checkNotNull(rs_race.getId(), "Race has no id");
            Race race = raceDAO.getRace(rs_race.getId());
            checkNotNull(race, "No such race");
            bindings.addRace(race);
        }
    }

    private void addPersonalities(final RS_Bindings rs_bindings, final Bindings bindings) {
        if (isEmpty(rs_bindings.getPersonalities())) return;

        for (RS_Personality rs_personality : rs_bindings.getPersonalities()) {
            checkNotNull(rs_personality.getId(), "Personality has no id");
            Personality personality = personalityDAO.getPersonality(rs_personality.getId());
            checkNotNull(personality, "No such personality");
            bindings.addPersonality(personality);
        }
    }

    private void addUsers(final RS_Bindings rs_bindings, final Bindings bindings) {
        if (isEmpty(rs_bindings.getUsers())) return;

        for (RS_User rs_user : rs_bindings.getUsers()) {
            checkNotNull(rs_user.getId(), "User has no id");
            BotUser user = botUserDAO.getUser(rs_user.getId());
            checkNotNull(user, "No such user");
            bindings.addUser(user);
        }
    }
}
