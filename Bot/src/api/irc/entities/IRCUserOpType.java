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

package api.irc.entities;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * IRC op modes
 */
@ParametersAreNonnullByDefault
public enum IRCUserOpType {
    VOICE('+', 'v'), HALF_OP('%', 'h'), OP('@', 'o'), SUPER_OP('&', 'a'), FOUNDER('~', 'q');

    public static IRCUserOpType get(final char symbolOrLetter) {
        for (IRCUserOpType val : values()) {
            if (val.symbol == symbolOrLetter || val.letter == symbolOrLetter) return val;
        }
        return null;
    }

    public static IRCUserOpType getOnlySymbols(final char symbolOrLetter) {
        for (IRCUserOpType val : values()) {
            if (val.symbol == symbolOrLetter) return val;
        }
        return null;
    }

    private final char symbol;
    private final char letter;

    IRCUserOpType(final char symbol, final char letter) {
        this.symbol = symbol;
        this.letter = letter;
    }

    /**
     * @return the char which is the letter representation of the op mode
     */
    public char getLetter() {
        return letter;
    }

    /**
     * @return the char which is the symbol representation of the of mode
     */
    public char getSymbol() {
        return symbol;
    }

    /**
     * @return a String containing the symbols of all op modes
     */
    public static String getSymbols() {
        IRCUserOpType[] types = values();
        StringBuilder builder = new StringBuilder(types.length);
        for (IRCUserOpType type : types) {
            builder.append(type.symbol);
        }
        return builder.toString();
    }
}
