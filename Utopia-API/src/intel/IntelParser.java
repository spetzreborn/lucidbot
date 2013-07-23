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

package intel;

import java.util.regex.Pattern;

/**
 * A parser of intel
 *
 * @param <E> the type of intel this class parses
 */
public interface IntelParser<E extends Intel> {
    /**
     * @return a Pattern that may be used to uniquely identify the type of intel this parser is capable of parsing
     */
    Pattern getIdentifierPattern();

    /**
     * Parses the specified text
     *
     * @param savedBy the nick of whoever sent this intel
     * @param text    the intel
     * @return the parsed intel, or null if it's too old and shouldn't be saved
     * @throws Exception if something goes wrong during parsing
     */
    E parse(String savedBy, String text) throws Exception;

    String getIntelTypeHandled();

    Pattern exportLinePattern = Pattern.compile("\\*\\* Export Line [^*]+\\*\\*(.*?)(?:\u001D|\\*\\* Finished \\*\\*)", Pattern.DOTALL);
}
