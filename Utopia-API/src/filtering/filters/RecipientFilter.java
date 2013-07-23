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

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.text.RegexUtil;
import com.google.inject.Provider;
import lombok.extern.log4j.Log4j;
import spi.filters.AbstractFilter;
import spi.filters.Filter;
import spi.filters.FilterBuilder;

import javax.inject.Inject;
import java.util.regex.Pattern;

@Log4j
public class RecipientFilter extends AbstractFilter<BotUser> {
    private final BotUser user;

    public RecipientFilter(final BotUser user) {
        super(BotUser.class);
        this.user = user;
    }

    @Override
    protected Class<? extends Filter<BotUser>> getFilterType() {
        return getClass();
    }

    public static Pattern getFilterPattern() {
        return Pattern.compile("(?:recipient|user) [^ ]+", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean passesFilter(final BotUser value) {
        return user.equals(value);
    }

    public static class Builder implements FilterBuilder {
        private final Pattern pattern = RecipientFilter.getFilterPattern();
        private final Provider<BotUserDAO> botUserDAOProvider;

        @Inject
        public Builder(final Provider<BotUserDAO> botUserDAOProvider) {
            this.botUserDAOProvider = botUserDAOProvider;
        }

        @Override
        public Filter<?> parseAndBuild(final String text) {
            try {
                BotUser user = botUserDAOProvider.get().getClosestMatch(RegexUtil.SPACE_PATTERN.split(text)[1]);
                if (user != null) return new RecipientFilter(user);
            } catch (Exception e) {
                log.error("", e);
            }
            return null;
        }

        @Override
        public Pattern getFilterPattern() {
            return pattern;
        }
    }
}
