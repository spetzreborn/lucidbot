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

import lombok.extern.log4j.Log4j;
import spi.filters.AbstractFilter;
import spi.filters.Filter;
import spi.filters.FilterBuilder;
import tools.parsing.UtopiaValidationType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j
public class KingdomLocationFilter extends AbstractFilter<String> {
    private final String location;
    private final boolean filterEquals;

    protected KingdomLocationFilter(final String location, final boolean filterEquals) {
        super(String.class);
        this.location = location;
        this.filterEquals = filterEquals;
    }

    @Override
    protected Class<? extends Filter<String>> getFilterType() {
        return getClass();
    }

    public static Pattern getFilterPattern() {
        return Pattern.compile("[!]?(" + UtopiaValidationType.KDLOC.getPatternString() + ')', Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean passesFilter(String value) {
        return value != null && (filterEquals ? value.equals(location) : !value.equals(location));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof KingdomLocationFilter)) return false;

        KingdomLocationFilter that = (KingdomLocationFilter) o;

        return location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }

    public static class Builder implements FilterBuilder {
        private final Pattern pattern = KingdomLocationFilter.getFilterPattern();

        @Override
        public Filter<?> parseAndBuild(final String text) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.matches()) return new KingdomLocationFilter(matcher.group(1), !text.startsWith("!"));
            return null;
        }

        @Override
        public Pattern getFilterPattern() {
            return pattern;
        }
    }
}
