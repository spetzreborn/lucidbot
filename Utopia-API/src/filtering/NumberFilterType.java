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

package filtering;

import api.irc.ValidationType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ParametersAreNonnullByDefault
public enum NumberFilterType {
    GREATER_THAN("[>]\\s*(" + ValidationType.DOUBLE.getPattern() + ")\\s*"),
    GREATER_THAN_WITH_K("[>]\\s*(" + ValidationType.DOUBLE_WITH_K.getPattern() + ")\\s*"),
    LESS_THAN("[<]\\s*(" + ValidationType.DOUBLE.getPattern() + ")\\s*"),
    LESS_THAN_WITH_K("[<]\\s*(" + ValidationType.DOUBLE_WITH_K.getPattern() + ")\\s*"),
    RANGE('(' + ValidationType.DOUBLE.getPattern() + ")\\s*-\\s*(" + ValidationType.DOUBLE.getPattern() + ")\\s*"),
    RANGE_WITH_K('(' + ValidationType.DOUBLE_WITH_K.getPattern() + ")\\s*-\\s*(" +
            ValidationType.DOUBLE_WITH_K.getPattern() + ")\\s*");

    private static final ConcurrentMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    private final String patternString;

    NumberFilterType(final String patternString) {
        this.patternString = patternString;
    }

    public String[] parse(final String filterEnd, final String text) {
        if (PATTERN_CACHE.get(filterEnd + ordinal()) == null)
            PATTERN_CACHE.putIfAbsent(filterEnd + ordinal(), Pattern.compile(patternString + filterEnd, Pattern.CASE_INSENSITIVE));
        Matcher matcher = PATTERN_CACHE.get(filterEnd + ordinal()).matcher(text.trim());
        if (matcher.matches()) {
            String[] strings = new String[matcher.groupCount()];
            for (int i = 0; i < matcher.groupCount(); ++i) {
                strings[i] = matcher.group(i + 1);
            }
            return strings;
        }
        return null;
    }

    public String getPatternString() {
        return patternString;
    }

    public static String getCompleteGroup(final String filterEnd) {
        StringBuilder b = new StringBuilder(200);
        b.append("(?:");
        for (NumberFilterType numberFilterType : values()) {
            if (b.length() > 3) b.append('|');
            b.append(numberFilterType.patternString);
        }
        b.append(')').append(filterEnd);
        return b.toString();
    }
}
