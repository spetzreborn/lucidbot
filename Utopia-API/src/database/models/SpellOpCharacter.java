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
import api.tools.text.StringUtil;
import lombok.extern.log4j.Log4j;
import tools.target_locator.SelfTargetLocator;
import tools.target_locator.TargetInTextLocator;
import tools.target_locator.TargetLocator;
import tools.target_locator.UserSpecifiedTargetLocator;

@Log4j
public enum SpellOpCharacter implements HasName {
    FADING_SPELLOP_WITH_PROVINCE(false) {
        @Override
        public Class<? extends TargetLocator> getTargetLocatorType() {
            return TargetInTextLocator.class;
        }
    }, FADING_SPELLOP_WITHOUT_PROVINCE(false) {
        @Override
        public Class<? extends TargetLocator> getTargetLocatorType() {
            return UserSpecifiedTargetLocator.class;
        }
    }, INSTANT_SPELLOP_WITH_PROVINCE(true) {
        @Override
        public Class<? extends TargetLocator> getTargetLocatorType() {
            return TargetInTextLocator.class;
        }
    }, INSTANT_SPELLOP_WITHOUT_PROVINCE(true) {
        @Override
        public Class<? extends TargetLocator> getTargetLocatorType() {
            return UserSpecifiedTargetLocator.class;
        }
    }, SELF_SPELLOP(false) {
        @Override
        public Class<? extends TargetLocator> getTargetLocatorType() {
            return SelfTargetLocator.class;
        }
    }, INSTANT_SELF_SPELLOP(true) {
        @Override
        public Class<? extends TargetLocator> getTargetLocatorType() {
            return SelfTargetLocator.class;
        }
    }, OTHER(false) {
        @Override
        public Class<? extends TargetLocator> getTargetLocatorType() {
            throw new UnsupportedOperationException("No target locator supported");
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

    public abstract Class<? extends TargetLocator> getTargetLocatorType();
}
