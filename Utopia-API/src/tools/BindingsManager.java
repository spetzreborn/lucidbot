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

package tools;

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.irc.ValidationType;
import api.tools.collections.CollectionUtil;
import api.tools.numbers.NumberUtil;
import api.tools.text.StringUtil;
import api.tools.time.DateFactory;
import api.tools.time.DateUtil;
import com.google.inject.Provider;
import database.CommonEntitiesAccess;
import database.daos.ProvinceDAO;
import database.models.Bindings;
import database.models.Personality;
import database.models.Province;
import database.models.Race;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import tools.parsing.UtopiaValidationType;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.Date;

import static api.tools.text.StringUtil.lowerCase;
import static api.tools.time.DateUtil.isBefore;

/**
 * A manager for bindings, which is the concept of binding entities to specific things such as race or personality.
 * The idea is that you might want to bind a build to a race/pers, or set a publish date for something or any such thing.
 */
@Log4j
public class BindingsManager {
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    @Inject
    public BindingsManager(final UtopiaTimeFactory utopiaTimeFactory,
                           final CommonEntitiesAccess commonEntitiesAccess,
                           final Provider<BotUserDAO> userDAOProvider,
                           final Provider<ProvinceDAO> provinceDAOProvider) {
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.userDAOProvider = userDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
    }

    /**
     * Checks if the bindings match the specified user
     *
     * @param bindings the bindings to check
     * @param user     the user to check against
     * @return true if the bindings match the specified user
     */
    public boolean matchesBindings(final Bindings bindings, final BotUser user) {
        if (bindings == null) return true;
        if (bindings.isAdminsOnly() && !user.isAdmin()) return false;
        if (bindings.getPublishDate() != null && System.currentTimeMillis() < bindings.getPublishDate().getTime())
            return false;
        if (bindings.getExpiryDate() != null && System.currentTimeMillis() >= bindings.getExpiryDate().getTime())
            return false;
        if (CollectionUtil.isNotEmpty(bindings.getUsers()) && !bindings.getUsers().contains(user)) return false;
        if (CollectionUtil.isNotEmpty(bindings.getRaces())) {
            Province province = null;
            try {
                province = provinceDAOProvider.get().getProvinceForUser(user);
            } catch (HibernateException e) {
                log.error("", e);
            }
            if (province == null || !bindings.getRaces().contains(province.getRace())) return false;
        }
        if (CollectionUtil.isNotEmpty(bindings.getPersonalities())) {
            Province province = null;
            try {
                province = provinceDAOProvider.get().getProvinceForUser(user);
            } catch (HibernateException e) {
                log.error("", e);
            }
            if (province == null || !bindings.getPersonalities().contains(province.getPersonality())) return false;
        }
        return true;
    }

    /**
     * Parses bindings from raw text
     *
     * @param text the text to parse
     * @return the Bindings parsed from the text
     */
    public Bindings parseBindings(final String text) {
        Bindings bindings = new Bindings();
        if (text == null) return bindings;
        String cleaned = text;
        if (cleaned.startsWith("{")) cleaned = cleaned.substring(1);
        if (cleaned.endsWith("}")) cleaned = cleaned.substring(0, cleaned.length() - 1);
        String[] split = StringUtil.splitOnComma(cleaned);
        for (String binding : split) {
            String trimmedBinding = binding.trim();
            if (addAdminOnly(lowerCase(trimmedBinding), bindings)) continue;
            if (addUserMatch(trimmedBinding, bindings)) continue;
            if (addRaceMatch(trimmedBinding, bindings)) continue;
            if (addPersonalityMatch(trimmedBinding, bindings)) continue;
            if (addPublishDate(lowerCase(trimmedBinding), bindings)) continue;
            addExpiryDate(lowerCase(trimmedBinding), bindings);
        }
        return bindings;
    }

    private static boolean addAdminOnly(String binding, Bindings bindings) {
        if ("admin".equalsIgnoreCase(binding)) {
            bindings.setAdminsOnly(true);
            return true;
        }
        return false;
    }

    private boolean addUserMatch(String binding, Bindings bindings) {
        try {
            BotUser user = userDAOProvider.get().getUser(binding.trim());
            if (user == null) return false;
            bindings.addUser(user);
            return true;
        } catch (HibernateException e) {
            log.error("", e);
        }
        return false;
    }

    private boolean addRaceMatch(String binding, Bindings bindings) {
        Race race = commonEntitiesAccess.getRace(binding.trim());
        if (race == null) return false;
        bindings.addRace(race);
        return true;
    }

    private boolean addPersonalityMatch(String binding, Bindings bindings) {
        Personality personality = commonEntitiesAccess.getPersonality(binding.trim());
        if (personality == null) return false;
        bindings.addPersonality(personality);
        return true;
    }

    private boolean addPublishDate(String binding, Bindings bindings) {
        if (StringUtil.startsWith(binding, "publish", "add", "set")) {
            Date date = getDate(binding);
            if (date == null) return false;
            bindings.setPublishDate(date);
            return true;
        }
        return false;
    }

    private boolean addExpiryDate(String binding, Bindings bindings) {
        if (StringUtil.startsWith(binding, "expire", "expires", "delete", "remove")) {
            Date date = getDate(binding);
            Date currentDate = new Date();
            if (date == null || isBefore(date, currentDate)) return false;
            bindings.setExpiryDate(date);
            return true;
        }
        return false;
    }

    private Date getDate(final String binding) {
        int firstSpace = binding.indexOf(' ');
        if (firstSpace > 0 && firstSpace + 1 < binding.length()) {
            String date = binding.substring(firstSpace + 1).trim();
            if (ValidationType.DATE_TIME_NO_SECONDS.matches(date)) {
                return DateFactory.newDate(date);
            } else if (UtopiaValidationType.UTODATE.matches(date)) {
                return utopiaTimeFactory.newUtopiaTime(date).getDate();
            } else if (ValidationType.DOUBLE.matches(date)) {
                return new Date(System.currentTimeMillis() + DateUtil.hoursToMillis(NumberUtil.parseDouble(date)));
            }
        }
        return null;
    }
}
