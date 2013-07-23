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

package tools.parsing;

import tools.time.UtopiaMonth;

import java.util.regex.Pattern;

/**
 * Some type of validations specific to utopia
 */
public enum UtopiaValidationType {
    KDLOC("\\(\\d{1,2}:\\d{1,2}\\)"),
    LENIENT_KDLOC("\\d{1,2}:\\d{1,2}"),
    UTODATE("(?i)(?:" + UtopiaMonth.getMonthPattern() + ") (?:(?:[0-1]\\d)|(?:[2][0-4])|(?:[1-9]))" +
            "(?:th, YR|rd, YR|st, YR|nd, YR| of YR|, YR)(?:\\d{1,2})"),
    UTODATE_WITH_GROUPS("(?i)(?<month>" + UtopiaMonth.getMonthPattern() + ") (?<day>(?:[0-1]\\d)|(?:[2][0-4])|(?:[1-9]))" +
            "(?:th, YR|rd, YR|st, YR|nd, YR| of YR|, YR)(?<year>\\d{1,2})"),
    GENERAL("\\d"),
    TIMEZONE("[+-]?(?:[0]\\d|[1][0-3]|\\d)");

    private final Pattern pattern;

    UtopiaValidationType(final String patternString) {
        pattern = Pattern.compile(patternString);
    }

    /**
     * @param toValidate the text to validate
     * @return true if this validation type matches the text
     */
    public boolean matches(final CharSequence toValidate) {
        return pattern.matcher(toValidate).matches();
    }

    /**
     * @return the pattern of this validator as a String
     */
    public String getPatternString() {
        return pattern.pattern();
    }

    /**
     * @return the Pattern of this validator
     */
    public Pattern getPattern() {
        return pattern;
    }
}
