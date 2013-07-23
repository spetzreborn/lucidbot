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

import api.irc.ValidationType;
import database.models.AttackType;
import database.models.NewsItem;
import tools.time.UtopiaTime;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for parsing news from inside utopia
 */
public class NewsParser {
    public static final String DATE = "(?<date>" + UtopiaValidationType.UTODATE.getPatternString() + ')';
    public static final String KD = "(?:" + UtopiaValidationType.KDLOC.getPatternString() + ')';
    public static final String INT = ValidationType.INT.getPattern();
    private static final Pattern NEWS_ITEM_PATTERN = Pattern
            .compile(DATE + "\\s+.+?\\s*(?=" + UtopiaValidationType.UTODATE.getPatternString() + "|$)", Pattern.MULTILINE);

    private final NonAttackNewsTypes nonAttackNewsTypes;
    private final UtopiaTimeFactory utopiaTimeFactory;

    @Inject
    public NewsParser(final NonAttackNewsTypes nonAttackNewsTypes, final UtopiaTimeFactory utopiaTimeFactory) {
        this.nonAttackNewsTypes = nonAttackNewsTypes;
        this.utopiaTimeFactory = utopiaTimeFactory;
    }

    /**
     * Parses the specified String and returns a List of the NewsItems that were found
     *
     * @param news the news
     * @return List of NewsItems parsed from the news String
     */
    public List<NewsItem> parseNews(final String news) {
        List<NewsItem> out = new ArrayList<>(100);
        Matcher matcher = NEWS_ITEM_PATTERN.matcher(news);
        String potentialMatch;
        Matcher itemMatcher;
        while (matcher.find()) {
            potentialMatch = matcher.group(0).trim();
            boolean matchFound = false;
            for (AttackType attackType : AttackType.values()) {
                Pattern incomingAttackNewsPattern = attackType.getIncomingAttackNewsPattern();
                if (incomingAttackNewsPattern != null) {
                    itemMatcher = incomingAttackNewsPattern.matcher(potentialMatch);
                    if (itemMatcher.find()) {
                        out.add(getNewsItemFromMatched(itemMatcher, "Incoming " + attackType.getName()));
                        matchFound = true;
                        break;
                    }
                }

                Pattern outgoingAttackNewsPattern = attackType.getOutgoingAttackNewsPattern();
                if (outgoingAttackNewsPattern != null) {
                    itemMatcher = outgoingAttackNewsPattern.matcher(potentialMatch);
                    if (itemMatcher.find()) {
                        out.add(getNewsItemFromMatched(itemMatcher, "Outgoing " + attackType.getName()));
                        matchFound = true;
                        break;
                    }
                }
            }
            if (!matchFound) {
                for (Map.Entry<String, Pattern> type : nonAttackNewsTypes.getAll().entrySet()) {
                    itemMatcher = type.getValue().matcher(potentialMatch);
                    if (itemMatcher.find()) {
                        out.add(getNewsItemFromMatched(itemMatcher, type.getKey()));
                        break;
                    }
                }
            }
        }
        return out;
    }

    private NewsItem getNewsItemFromMatched(final Matcher matched, final String type) {
        NewsItem newsItem = new NewsItem();
        try {
            newsItem.setSource(matched.group("source"));
        } catch (IllegalArgumentException ignore) {
        }

        try {
            newsItem.setTarget(matched.group("target"));
        } catch (IllegalArgumentException ignore) {
        }

        try {
            newsItem.setItemValue(matched.group("value"));
        } catch (IllegalArgumentException ignore) {
        }

        newsItem.setUtoDate(matched.group("date"));
        UtopiaTime utopiaTime = utopiaTimeFactory.newUtopiaTime(newsItem.getUtoDate());
        newsItem.setRealDate(utopiaTime.getDate());
        newsItem.setNewsType(type);
        newsItem.setOriginalMessage(matched.group(0).replaceAll("(\n|\r)", ""));
        return newsItem;
    }
}
