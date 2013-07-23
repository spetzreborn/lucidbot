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

public class TpaFilter extends AbstractNumberBasedFilter<Double> {
    private static final String filterEnd = "(?:tpa)";

    public static TpaFilter createGreaterThanFilter(final Double val) {
        return new TpaFilter(val, null);
    }

    public static TpaFilter createLessThanThanFilter(final Double val) {
        return new TpaFilter(null, val);
    }

    public static TpaFilter createRangeFilter(final Double lowerBound, final Double upperBound) {
        return new TpaFilter(lowerBound, upperBound);
    }

    public static Pattern getFilterPattern() {
        return Pattern.compile(NumberFilterType.getCompleteGroup(filterEnd), Pattern.CASE_INSENSITIVE);
    }

    public static TpaFilter parseAndCreateFilter(final String text) {
        String[] strings = NumberFilterType.LESS_THAN.parse(filterEnd, text);
        if (strings != null) return createLessThanThanFilter(NumberUtil.parseDouble(strings[0]));

        strings = NumberFilterType.GREATER_THAN.parse(filterEnd, text);
        if (strings != null) return createGreaterThanFilter(NumberUtil.parseDouble(strings[0]));

        strings = NumberFilterType.RANGE.parse(filterEnd, text);
        if (strings != null) return createRangeFilter(NumberUtil.parseDouble(strings[0]), NumberUtil.parseDouble(strings[1]));
        return null;
    }

    public TpaFilter(final Double lowerBound, final Double upperBound) {
        super(Double.class, lowerBound, upperBound);
    }

    @Override
    protected Class<? extends Filter<Double>> getFilterType() {
        return getClass();
    }

    public static class Builder implements FilterBuilder {
        private final Pattern pattern = TpaFilter.getFilterPattern();

        @Override
        public Filter<?> parseAndBuild(final String text) {
            return TpaFilter.parseAndCreateFilter(text);
        }

        @Override
        public Pattern getFilterPattern() {
            return pattern;
        }
    }
}
