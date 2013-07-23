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

package api.filters;

import api.tools.text.StringUtil;
import spi.filters.Filter;
import spi.filters.FilterBuilder;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A parser for parsing filters from text
 */
@ParametersAreNonnullByDefault
public final class FilterParser {
    private static final Pattern IN_COMMAND_FILTER_PATTERN = Pattern.compile("\\[(.+?)\\]");
    private final Set<FilterBuilder> filterBuilders;

    @Inject
    public FilterParser(final Set<FilterBuilder> filterBuilders) {
        this.filterBuilders = checkNotNull(filterBuilders);
    }

    /**
     * @return the Pattern describing how a filter group should look in a command
     */
    public static Pattern getInCommandFilterPattern() {
        return IN_COMMAND_FILTER_PATTERN;
    }

    /**
     * Parses all filters from the specified text
     *
     * @param text the String to parse filters from
     * @return a Collection of the filters that were successfully parsed from the text
     */
    public Collection<Filter<?>> parseFilters(final String text) {
        String cleaned = checkNotNull(text);
        if (cleaned.startsWith("[")) cleaned = cleaned.substring(1);
        if (cleaned.endsWith("]")) cleaned = cleaned.substring(0, cleaned.length() - 1);
        String[] split = StringUtil.splitOnComma(cleaned);
        List<Filter<?>> filters = new ArrayList<>(split.length);
        for (String s : split) {
            for (FilterBuilder builder : filterBuilders) {
                if (builder.getFilterPattern().matcher(s.trim()).matches()) {
                    Filter<?> filter = builder.parseAndBuild(s.trim());
                    if (filter != null) filters.add(filter);
                    break;
                }
            }
        }
        return filters;
    }
}
