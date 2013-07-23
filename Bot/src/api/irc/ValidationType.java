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

package api.irc;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Enum used to describe various input types the bot can validate against
 */
@ParametersAreNonnullByDefault
public enum ValidationType {
    BOOLEAN("(?i)(?:true|false)"),
    INT("[\\d,]+"),
    DOUBLE("[\\d,]+|[\\d,]*[.]\\d+"),
    STRICT_DOUBLE("[\\d,]*[.]\\d+"),
    NICKNAME("[A-Za-z\\{\\}\\|`\\^\\]\\[_\\\\]{1}[A-Za-z0-9\\{\\}\\|`\\^\\]\\[_\\\\\\-]{0,29}"),
    DOUBLE_WITH_K("[\\d,]+(?:[.]\\d+)?k*"),
    CHANNEL("#[^\\x00-\\x20\\xA0,]{0,31}"),
    DATE_TIME_NO_SECONDS("\\d{4}-(?:[0][1-9]|[1][0-2])-(?:[0][1-9]|[1-2]\\d|[3][0-1]) (?:[0-1]\\d|[2][0-3]):(?:[0-5]\\d)");

    private final Pattern pattern;

    ValidationType(final String patternString) {
        pattern = Pattern.compile(patternString);
    }

    /**
     * @param toValidate the text to validate
     * @return true if the text matches this ValidationType
     */
    public boolean matches(final CharSequence toValidate) {
        return pattern.matcher(checkNotNull(toValidate)).matches();
    }

    /**
     * @return the Pattern of this ValidationType as a String
     */
    public String getPattern() {
        return pattern.pattern();
    }
}
