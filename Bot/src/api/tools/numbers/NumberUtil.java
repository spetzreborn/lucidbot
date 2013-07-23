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

package api.tools.numbers;

import api.tools.text.StringUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class NumberUtil {
    private static final NumberFormat nf = NumberFormat.getInstance(Locale.US);

    private NumberUtil() {
    }

    /**
     * @param val the String to parse
     * @return the parsed double
     * @throws IllegalArgumentException if the String does not contain a parseable double
     */
    public static double parseDouble(final String val) {
        try {
            return nf.parse(checkNotNull(val)).doubleValue();
        } catch (ParseException e) {
            throw new IllegalArgumentException("String could not be parsed as double: " + val);
        }
    }

    /**
     * @param val the String to parse, may include k's to signify *1000
     * @return the parsed double
     * @throws IllegalArgumentException if the String does not contain a parseable double
     */
    public static double parseDoubleWithK(final String val) {
        try {
            int power = StringUtil.countOccurance(checkNotNull(val), 'k');
            return Math.pow(1000, power) * nf.parse(val.replace("k", "")).doubleValue();
        } catch (ParseException e) {
            throw new IllegalArgumentException("String could not be parsed as double: " + val);
        }
    }

    /**
     * @param val the String to parse
     * @return the parsed int
     * @throws IllegalArgumentException if the String does not contain a parseable int
     */
    public static int parseInt(final String val) {
        try {
            return nf.parse(checkNotNull(val)).intValue();
        } catch (ParseException e) {
            throw new IllegalArgumentException("String could not be parsed as int: " + val);
        }
    }

    /**
     * @param val the String to parse
     * @return the parsed long
     * @throws IllegalArgumentException if the String does not contain a parseable long
     */
    public static long parseLong(final String val) {
        try {
            return nf.parse(checkNotNull(val)).longValue();
        } catch (ParseException e) {
            throw new IllegalArgumentException("String could not be parsed as long: " + val);
        }
    }
}
