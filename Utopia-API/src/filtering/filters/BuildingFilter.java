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
import database.CommonEntitiesAccess;
import filtering.AbstractNumberBasedFilter;
import filtering.NumberFilterType;
import spi.filters.Filter;
import spi.filters.FilterBuilder;

import javax.inject.Inject;
import java.util.regex.Pattern;

public class BuildingFilter extends AbstractNumberBasedFilter<Double> {
    public static BuildingFilter createGreaterThanFilter(final Double val, final String buildingName) {
        return new BuildingFilter(val, null, buildingName);
    }

    public static BuildingFilter createLessThanThanFilter(final Double val, final String buildingName) {
        return new BuildingFilter(null, val, buildingName);
    }

    public static BuildingFilter createRangeFilter(final Double lowerBound, final Double upperBound, final String buildingName) {
        return new BuildingFilter(lowerBound, upperBound, buildingName);
    }

    public static Pattern getFilterPattern(final String filterEndPattern) {
        return Pattern.compile(NumberFilterType.getCompleteGroup(filterEndPattern), Pattern.CASE_INSENSITIVE);
    }

    public static BuildingFilter parseAndCreateFilter(final String filterEnd, final String text) {
        String[] strings = NumberFilterType.GREATER_THAN.parse(filterEnd, text);
        if (strings != null) return createGreaterThanFilter(NumberUtil.parseDouble(strings[0]), strings[1]);

        strings = NumberFilterType.LESS_THAN.parse(filterEnd, text);
        if (strings != null) return createLessThanThanFilter(NumberUtil.parseDouble(strings[0]), strings[1]);

        strings = NumberFilterType.RANGE.parse(filterEnd, text);
        if (strings != null) return createRangeFilter(NumberUtil.parseDouble(strings[0]), NumberUtil.parseDouble(strings[1]), strings[2]);
        return null;
    }

    private static String createFilterEnd(final CommonEntitiesAccess commonEntitiesAccess) {
        try {
            return '(' + commonEntitiesAccess.getBuildingGroup() + ')';
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final String buildingName;

    public BuildingFilter(final Double lowerBound, final Double upperBound, final String buildingName) {
        super(Double.class, lowerBound, upperBound);
        this.buildingName = buildingName;
    }

    @Override
    public Object[] getMethodParameters() {
        return new Object[]{buildingName};
    }

    public String getBuildingName() {
        return buildingName;
    }

    @Override
    protected Class<? extends Filter<Double>> getFilterType() {
        return getClass();
    }

    public static class Builder implements FilterBuilder {
        private final Pattern pattern;
        private final String filterEndPattern;

        @Inject
        public Builder(final CommonEntitiesAccess commonEntitiesAccess) {
            filterEndPattern = BuildingFilter.createFilterEnd(commonEntitiesAccess);
            pattern = BuildingFilter.getFilterPattern(filterEndPattern);
        }

        @Override
        public Filter<?> parseAndBuild(final String text) {
            return BuildingFilter.parseAndCreateFilter(filterEndPattern, text);
        }

        @Override
        public Pattern getFilterPattern() {
            return pattern;
        }
    }
}