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

package database.models;

import api.common.HasName;
import api.database.models.BotUser;
import api.tools.text.StringUtil;
import database.daos.ProvinceDAO;
import database.daos.UserSpellOpTargetDAO;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;

import java.util.regex.Matcher;

@Log4j
public enum SpellOpCharacter implements HasName {
    FADING_SPELLOP_WITH_PROVINCE(false) {
        @Override
        public Province getTarget(final ProvinceDAO provinceDAO, final UserSpellOpTargetDAO userSpellOpTargetDAO, final BotUser user,
                                  final Matcher matchedRegex) {
            String target = matchedRegex.group("target");
            String province = target.substring(0, target.indexOf('(')).trim();
            String kingdom = target.substring(target.indexOf('('));
            if (province == null)
                throw new IllegalStateException("Could not find a province in the spell/op message even" + "though one was expected");
            return getProvince(provinceDAO, province, kingdom);
        }
    }, FADING_SPELLOP_WITHOUT_PROVINCE(false) {
        @Override
        public Province getTarget(final ProvinceDAO provinceDAO, final UserSpellOpTargetDAO userSpellOpTargetDAO, final BotUser user,
                                  final Matcher matchedRegex) {
            UserSpellOpTarget userSpellOpTarget = null;
            try {
                userSpellOpTarget = userSpellOpTargetDAO.getUserSpellOpTarget(user);
            } catch (HibernateException e) {
                log.error("", e);
            }
            return userSpellOpTarget == null ? null : userSpellOpTarget.getTarget();
        }
    }, INSTANT_SPELLOP_WITH_PROVINCE(true) {
        @Override
        public Province getTarget(final ProvinceDAO provinceDAO, final UserSpellOpTargetDAO userSpellOpTargetDAO, final BotUser user,
                                  final Matcher matchedRegex) {
            String target = matchedRegex.group("target");
            String province = target.substring(0, target.indexOf('(')).trim();
            String kingdom = target.substring(target.indexOf('('));
            if (province == null)
                throw new IllegalStateException("Could not find a province in the spell/op message even though one was expected");
            return getProvince(provinceDAO, province, kingdom);
        }
    }, INSTANT_SPELLOP_WITHOUT_PROVINCE(true) {
        @Override
        public Province getTarget(final ProvinceDAO provinceDAO, final UserSpellOpTargetDAO userSpellOpTargetDAO, final BotUser user,
                                  final Matcher matchedRegex) {
            UserSpellOpTarget userSpellOpTarget = null;
            try {
                userSpellOpTarget = userSpellOpTargetDAO.getUserSpellOpTarget(user);
            } catch (HibernateException e) {
                log.error("", e);
            }
            return userSpellOpTarget == null ? null : userSpellOpTarget.getTarget();
        }
    }, SELF_SPELLOP(false) {
        @Override
        public Province getTarget(final ProvinceDAO provinceDAO, final UserSpellOpTargetDAO userSpellOpTargetDAO, final BotUser user,
                                  final Matcher matchedRegex) {
            try {
                return provinceDAO.getProvinceForUser(user);
            } catch (HibernateException e) {
                log.error("", e);
            }
            return null;
        }
    }, INSTANT_SELF_SPELLOP(true) {
        @Override
        public Province getTarget(final ProvinceDAO provinceDAO, final UserSpellOpTargetDAO userSpellOpTargetDAO, final BotUser user,
                                  final Matcher matchedRegex) {
            try {
                return provinceDAO.getProvinceForUser(user);
            } catch (HibernateException e) {
                log.error("", e);
            }
            return null;
        }
    }, OTHER(false) {
        @Override
        public Province getTarget(final ProvinceDAO provinceDAO, final UserSpellOpTargetDAO userSpellOpTargetDAO, final BotUser user,
                                  final Matcher matchedRegex) {
            throw new UnsupportedOperationException("This method should never be used on this type");
        }
    };
    private final boolean isInstant;

    SpellOpCharacter(final boolean isInstant) {
        this.isInstant = isInstant;
    }

    public boolean isInstant() {
        return isInstant;
    }

    @Override
    public String getName() {
        return StringUtil.prettifyEnumName(this);
    }

    public String getCharacterName() {
        return getName();
    }

    public static SpellOpCharacter fromName(final String name) {
        for (SpellOpCharacter character : values()) {
            if (character.getName().equalsIgnoreCase(name)) return character;
        }
        throw new IllegalArgumentException("No such SpellOpCharacter exists");
    }

    private static Province getProvince(final ProvinceDAO provinceDAO, final String name, final String kingdom) {
        try {
            return provinceDAO.getOrCreateProvince(name, kingdom);
        } catch (HibernateException e) {
            log.error("", e);
        }
        return null;
    }

    public abstract Province getTarget(final ProvinceDAO provinceDAO, final UserSpellOpTargetDAO userSpellOpTargetDAO, final BotUser user,
                                       final Matcher matchedRegex);
}
