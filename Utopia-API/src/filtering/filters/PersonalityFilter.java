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

package filtering.filters;

import database.CommonEntitiesAccess;
import database.models.Personality;
import spi.filters.AbstractFilter;
import spi.filters.Filter;
import spi.filters.FilterBuilder;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonalityFilter extends AbstractFilter<Personality> {
    private final Personality personality;
    private final boolean filterEquals;

    public static PersonalityFilter createEqualsFilter(final Personality personality) {
        return new PersonalityFilter(personality, true);
    }

    public static PersonalityFilter createNonEqualsFilter(final Personality personality) {
        return new PersonalityFilter(personality, false);
    }

    public static PersonalityFilter parseAndCreateFilter(final CommonEntitiesAccess access, final Pattern pattern, final String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.matches()) {
            try {
                Personality personality = access.getPersonality(matcher.group(1));
                if (personality != null)
                    return matcher.group(0).startsWith("!") ? createNonEqualsFilter(personality) : createEqualsFilter(personality);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static Pattern getFilterPattern(final CommonEntitiesAccess access) {
        return Pattern.compile("[!]?(" + access.getPersonalityGroup() + ')', Pattern.CASE_INSENSITIVE);
    }

    private PersonalityFilter(final Personality personality, final boolean filterEquals) {
        super(Personality.class);
        this.personality = personality;
        this.filterEquals = filterEquals;
    }

    @Override
    protected Class<? extends Filter<Personality>> getFilterType() {
        return getClass();
    }

    @Override
    public boolean passesFilter(Personality value) {
        return value != null && (filterEquals ? value.equals(personality) : !value.equals(personality));
    }

    public static class Builder implements FilterBuilder {
        private final Pattern pattern;
        private final CommonEntitiesAccess commonEntitiesAccess;

        @Inject
        public Builder(final CommonEntitiesAccess commonEntitiesAccess) {
            this.commonEntitiesAccess = commonEntitiesAccess;
            pattern = PersonalityFilter.getFilterPattern(commonEntitiesAccess);
        }

        @Override
        public Filter<?> parseAndBuild(final String text) {
            return PersonalityFilter.parseAndCreateFilter(commonEntitiesAccess, pattern, text);
        }

        @Override
        public Pattern getFilterPattern() {
            return pattern;
        }
    }
}
