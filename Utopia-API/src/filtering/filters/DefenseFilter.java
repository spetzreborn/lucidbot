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

import api.tools.numbers.NumberUtil;
import filtering.AbstractNumberBasedFilter;
import filtering.NumberFilterType;
import spi.filters.Filter;
import spi.filters.FilterBuilder;

import java.util.regex.Pattern;

public class DefenseFilter extends AbstractNumberBasedFilter<Integer> {
    private static final String filterEnd = "(?:def|defense|md)";

    public static DefenseFilter createGreaterThanFilter(final Integer val) {
        return new DefenseFilter(val, null);
    }

    public static DefenseFilter createLessThanThanFilter(final Integer val) {
        return new DefenseFilter(null, val);
    }

    public static DefenseFilter createRangeFilter(final Integer lowerBound, final Integer upperBound) {
        return new DefenseFilter(lowerBound, upperBound);
    }

    public static Pattern getFilterPattern() {
        return Pattern.compile(NumberFilterType.getCompleteGroup(filterEnd), Pattern.CASE_INSENSITIVE);
    }

    public static DefenseFilter parseAndCreateFilter(final String text) {
        String[] strings = NumberFilterType.LESS_THAN_WITH_K.parse(filterEnd, text);
        if (strings != null) return createLessThanThanFilter((int) NumberUtil.parseDoubleWithK(strings[0]));

        strings = NumberFilterType.GREATER_THAN_WITH_K.parse(filterEnd, text);
        if (strings != null) return createGreaterThanFilter((int) NumberUtil.parseDoubleWithK(strings[0]));

        strings = NumberFilterType.RANGE_WITH_K.parse(filterEnd, text);
        if (strings != null)
            return createRangeFilter((int) NumberUtil.parseDoubleWithK(strings[0]), (int) NumberUtil.parseDoubleWithK(strings[1]));
        return null;
    }

    public DefenseFilter(final Integer lowerBound, final Integer upperBound) {
        super(Integer.class, lowerBound, upperBound);
    }

    @Override
    protected Class<? extends Filter<Integer>> getFilterType() {
        return getClass();
    }

    public static class Builder implements FilterBuilder {
        private final Pattern pattern = DefenseFilter.getFilterPattern();

        @Override
        public Filter<?> parseAndBuild(final String text) {
            return DefenseFilter.parseAndCreateFilter(text);
        }

        @Override
        public Pattern getFilterPattern() {
            return pattern;
        }
    }
}

