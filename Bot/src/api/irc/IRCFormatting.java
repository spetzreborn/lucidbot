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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class containing formatting options for IRC
 */
public class IRCFormatting {
    public static final String NORMAL = "\u000f";
    public static final String BOLD = "\u0002";
    public static final String UNDERLINE = "\u001f";
    public static final String REVERSE = "\u0016";
    public static final String WHITE = "\u000300";
    public static final String BLACK = "\u000301";
    public static final String DARK_BLUE = "\u000302";
    public static final String DARK_GREEN = "\u000303";
    public static final String RED = "\u000304";
    public static final String BROWN = "\u000305";
    public static final String PURPLE = "\u000306";
    public static final String OLIVE = "\u000307";
    public static final String YELLOW = "\u000308";
    public static final String GREEN = "\u000309";
    public static final String TEAL = "\u000310";
    public static final String CYAN = "\u000311";
    public static final String BLUE = "\u000312";
    public static final String MAGENTA = "\u000313";
    public static final String DARK_GRAY = "\u000314";
    public static final String LIGHT_GRAY = "\u000315";

    private static final Map<String, String> formattingMap = new HashMap<>(30);

    static {
        formattingMap.put("NORMAL", NORMAL);
        formattingMap.put("BOLD", BOLD);
        formattingMap.put("UNDERLINE", UNDERLINE);
        formattingMap.put("REVERSE", REVERSE);
        formattingMap.put("WHITE", WHITE);
        formattingMap.put("BLACK", BLACK);
        formattingMap.put("DARK_BLUE", DARK_BLUE);
        formattingMap.put("DARK_GREEN", DARK_GREEN);
        formattingMap.put("RED", RED);
        formattingMap.put("BROWN", BROWN);
        formattingMap.put("PURPLE", PURPLE);
        formattingMap.put("OLIVE", OLIVE);
        formattingMap.put("YELLOW", YELLOW);
        formattingMap.put("GREEN", GREEN);
        formattingMap.put("TEAL", TEAL);
        formattingMap.put("CYAN", CYAN);
        formattingMap.put("BLUE", BLUE);
        formattingMap.put("MAGENTA", MAGENTA);
        formattingMap.put("DARK_GRAY", DARK_GRAY);
        formattingMap.put("LIGHT_GRAY", LIGHT_GRAY);
    }

    /**
     * @return A Map with all the formatting options mapped by name-to-value
     */
    public static Map<String, String> getFormattingOptionsMap() {
        return Collections.unmodifiableMap(formattingMap);
    }

    private IRCFormatting() {
    }
}
