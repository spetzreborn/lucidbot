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

package api.tools.text;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class IRCFormattingUtil {
    private static final Pattern COLOR_PATTERN = Pattern.compile("\u0003\\d{2}(,\\d{2})?");
    private static final Pattern FORMATTING_PATTERN = Pattern.compile("(\u000f|\u0002|\u001f|\u0016)");

    private IRCFormattingUtil() {
    }

    /**
     * Removes all the IRC colors from the specified String
     *
     * @param line the String to process
     * @return a new String with the colors removed
     */
    public static String removeColors(final String line) {
        return COLOR_PATTERN.matcher(checkNotNull(line)).replaceAll("");
    }

    /**
     * Removes all the non-color IRC formatting from the specified String
     *
     * @param line the String to process
     * @return a new String with the formatting removed
     */
    public static String removeFormatting(final String line) {
        return FORMATTING_PATTERN.matcher(checkNotNull(line)).replaceAll("");
    }

    /**
     * Removes all the colors and IRC formatting from the specified String
     *
     * @param line the String to process
     * @return a new String with the formatting and colors removed
     */
    public static String removeFormattingAndColors(final String line) {
        return removeFormatting(removeColors(checkNotNull(line)));
    }

    /**
     * Calculates the length of the string after colors and formatting is removed
     *
     * @param string the string
     * @return the length
     */
    public static int lengthWithoutFormattingChars(final String string) {
        return removeFormattingAndColors(checkNotNull(string)).length();
    }
}
