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

import java.util.regex.Pattern;

public class RegexUtil {
    public static final Pattern NEW_LINE_PATTERN = Pattern.compile("(?:\r)?\n");
    public static final Pattern TAB_PATTERN = Pattern.compile("\t");
    public static final Pattern SPACE_PATTERN = Pattern.compile(" ");
    public static final Pattern COLON_PATTERN = Pattern.compile(":");
    public static final Pattern COMMA_PATTERN = Pattern.compile(",");
    public static final Pattern PERIOD_PATTERN = Pattern.compile("\\.");
    public static final Pattern WHITESPACES_PATTERN = Pattern.compile("\\s+");
    public static final Pattern TAB_OR_MULTIPLE_SPACES_PATTERN = Pattern.compile("[\t ]{2,}");
    public static final Pattern NON_NUMBER_PATTERN = Pattern.compile("[^0-9]");
    public static final Pattern PIPE_PATTERN = Pattern.compile("[|]");

    private RegexUtil() {
    }
}
