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

import api.irc.ValidationType;
import api.tools.text.RegexUtil;
import filtering.AbstractBooleanBasedFilter;
import spi.filters.Filter;
import spi.filters.FilterBuilder;

import java.util.regex.Pattern;

public class ReadFilter extends AbstractBooleanBasedFilter {
    public ReadFilter(final boolean bool) {
        super(bool);
    }

    @Override
    protected Class<? extends Filter<Boolean>> getFilterType() {
        return getClass();
    }

    public static Pattern getFilterPattern() {
        return Pattern.compile("read (?:true|false)", Pattern.CASE_INSENSITIVE);
    }

    public static class Builder implements FilterBuilder {
        private final Pattern pattern = ReadFilter.getFilterPattern();

        @Override
        public Filter<?> parseAndBuild(final String text) {
            String[] split = RegexUtil.SPACE_PATTERN.split(text);
            if (split.length == 2 && ValidationType.BOOLEAN.matches(split[1])) return new ReadFilter(Boolean.valueOf(split[1]));
            return null;
        }

        @Override
        public Pattern getFilterPattern() {
            return pattern;
        }
    }
}
