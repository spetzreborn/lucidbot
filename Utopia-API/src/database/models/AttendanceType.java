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

package database.models;

import api.common.HasName;
import api.tools.text.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public enum AttendanceType implements HasName {
    ATTENDING("attending") {
        @Override
        public String extractDetails(final String text) {
            return null;
        }
    }, NOT_ATTENDING("not attending") {
        @Override
        public String extractDetails(final String text) {
            return null;
        }
    }, LATE("late .+") {
        @Override
        public String extractDetails(final String text) {
            return text.substring(5);
        }
    };

    private final Pattern pattern;

    AttendanceType(String textVersion) {
        pattern = Pattern.compile(textVersion, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public String getName() {
        return StringUtil.prettifyEnumName(this);
    }

    public static AttendanceType fromName(final String name) {
        for (AttendanceType attendanceType : values()) {
            if (attendanceType.getName().equalsIgnoreCase(name)) return attendanceType;
        }
        return null;
    }

    public static AttendanceType fromString(final String text) {
        for (AttendanceType attendanceType : values()) {
            if (attendanceType.pattern.matcher(text).matches()) return attendanceType;
        }
        return null;
    }

    public static String getRegexGroup() {
        List<String> strings = new ArrayList<>();
        for (AttendanceType attendanceType : values()) {
            strings.add(attendanceType.pattern.pattern());
        }
        return StringUtil.merge(strings, '|');
    }

    public abstract String extractDetails(final String text);
}
