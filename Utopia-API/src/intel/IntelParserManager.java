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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.regex.Matcher;

/**
 * A manager for intel parsers
 */
@Singleton
public class IntelParserManager {
    private static final String FINISHED = "** Finished **";
    private final List<IntelParser<?>> parsers;

    @Inject
    public IntelParserManager(final Set<IntelParser<?>> parsers) {
        this.parsers = new ArrayList<>(parsers);
    }

    /**
     * Goes through the specified text and resolves which parsers should be used to parse it.
     * I.e. the text can contain several pieces of intel that should be parsed seperately.
     *
     * @param text the text with intel in it
     * @return a Map of pieces of intel mapped to the corresponding parser for that piece
     */
    public Map<String, IntelParser<?>> getParsers(final String text) {
        Map<String, IntelParser<?>> textToParserMapping = new LinkedHashMap<>();

        List<TextWithParser> textWithParsers = new LinkedList<>();
        for (IntelParser<?> parser : parsers) {
            Matcher matcher = parser.getIdentifierPattern().matcher(text);
            while (matcher.find()) {
                textWithParsers.add(new TextWithParser(matcher.start(), parser));
            }
        }

        if (textWithParsers.isEmpty()) return Collections.emptyMap();
        else if (textWithParsers.size() == 1) {
            textToParserMapping.put(text + FINISHED, textWithParsers.get(0).getParser());
            return textToParserMapping;
        }

        Collections.sort(textWithParsers);
        for (int i = 0; i < textWithParsers.size(); ++i) {
            String part = i + 1 == textWithParsers.size() ? text.substring(textWithParsers.get(i).getTextStart())
                    : text.substring(textWithParsers.get(i).getTextStart(),
                    textWithParsers.get(i + 1).getTextStart());
            textToParserMapping.put(part.trim() + FINISHED, textWithParsers.get(i).getParser());
        }
        return textToParserMapping;
    }

    private static class TextWithParser implements Comparable<TextWithParser> {
        private final int textStart;
        private final IntelParser<?> parser;

        private TextWithParser(int textStart, IntelParser<?> parser) {
            this.textStart = textStart;
            this.parser = parser;
        }

        public int getTextStart() {
            return textStart;
        }

        public IntelParser<?> getParser() {
            return parser;
        }

        @Override
        public int compareTo(TextWithParser o) {
            return Integer.compare(textStart, o.getTextStart());
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof TextWithParser)) return false;

            TextWithParser that = (TextWithParser) o;

            return textStart == that.textStart;
        }

        @Override
        public int hashCode() {
            return textStart;
        }
    }
}
