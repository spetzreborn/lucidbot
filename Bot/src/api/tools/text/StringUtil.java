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

import api.common.HasName;
import api.irc.IRCFormatting;
import api.irc.ValidationType;
import api.tools.time.DateUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class offers some static access methods to help with string manipulation of different kinds
 */
@ParametersAreNonnullByDefault
public class StringUtil {
    private StringUtil() {
        // don't allow instantiation
    }

    public static String lowerCase(final String string) {
        return string == null ? null : string.toLowerCase(Locale.ENGLISH);
    }

    public static String colorWithAge(final String toColor, final Date first) {
        long timeAgo = System.currentTimeMillis() - first.getTime();
        double hoursOld = DateUtil.hoursFromMillis(timeAgo);
        StringBuilder builder = new StringBuilder(toColor.length() + 20);
        if (hoursOld < 3) builder.append(IRCFormatting.DARK_GREEN);
        else if (hoursOld < 12) builder.append(IRCFormatting.OLIVE);
        else if (hoursOld < 24) builder.append(IRCFormatting.BROWN);
        else builder.append(IRCFormatting.RED);
        builder.append(toColor).append(IRCFormatting.NORMAL);
        return builder.toString();
    }

    /**
     * Capitalizes the first letters in each of the words in the supplied sequence
     *
     * @param sequence .
     * @return a String with the first letters of each word from the specified sequence capitalized
     */
    public static String capitalizeFirstLetters(final String sequence, final boolean lowerCaseTheRest) {
        if (isNullOrEmpty(sequence)) return sequence;
        final StringBuilder stringBuilder = new StringBuilder(sequence.length());
        for (final String s : splitOnSpace(sequence)) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(s.substring(0, 1).toUpperCase());
            String rest = s.substring(1, s.length());
            if (lowerCaseTheRest) rest = lowerCase(rest);
            stringBuilder.append(rest);
        }
        return stringBuilder.toString();
    }

    /**
     * Counts the occurance of the specified char in the String
     *
     * @param string  the String to search for occurances in
     * @param tocount the char to count
     * @return the amount of occurances found
     */
    public static int countOccurance(final String string, final char tocount) {
        int out = 0;
        char[] temp = string.toCharArray();
        for (char c : temp) {
            if (c == tocount) {
                ++out;
            }
        }
        return out;
    }

    /**
     * Returns the amount of occurances of the specific String inside the full string
     *
     * @param string  the String to search for occurances in
     * @param tocount the String to count
     * @return the amount of occurances found
     */
    public static int countOccurance(final String string, final String tocount) {
        int out = 0;
        String temp = string;
        int index;
        while ((index = temp.indexOf(tocount)) != -1) {
            temp = temp.substring(index + tocount.length());
            ++out;
        }
        return out;
    }

    /**
     * Extracts a partial string matching the supplied regex from the complete message
     *
     * @param partial  the part to extract
     * @param complete the complete sequence
     * @return the extracted string
     */
    public static String extractPartialString(final String partial, final CharSequence complete) {
        Matcher matcher = Pattern.compile('(' + checkNotNull(partial) + ')').matcher(checkNotNull(complete));
        if (matcher.find()) return matcher.group(1);
        return "";
    }

    /**
     * Returns the levenshtein distance between two strings, i.e. how much it takes to change one string into the other. Downloaded
     * from http://www.merriampark.com/ldjava.htm Thanks to the creator!
     *
     * @param cs .
     * @param t  .
     * @return an integer representing the Levenshtein distance
     */
    public static int getLevenshteinDistance(final CharSequence cs, final CharSequence t) {
        if (cs == null || t == null) throw new IllegalArgumentException("Strings must not be null");

        /*
         * The difference between this impl. and the previous is that, rather than creating and retaining a matrix of size
         * cs.length()+1 by t.length()+1, we maintain two single-dimensional arrays of length cs.length()+1. The first, costs, is the
         * 'current working' distance array that maintains the newest distance cost counts as we iterate through the characters of
         * String cs. Each time we increment the index of String t we are comparing, costs is copied to previousCosts, the second int[]. Doing so
         * allows us to retain the previous cost counts as required by the algorithm (taking the minimum of the cost count to the
         * left, up one, and diagonally up and to the left of the current cost count being calculated). (Note that the arrays aren't
         * really copied anymore, just switched...this is clearly much better than cloning an array or doing a System.arraycopy()
         * each time through the outer loop.) Effectively, the difference between the two implementations is this one does not cause
         * an out of memory condition when calculating the LD over two very large strings.
         */

        final int n = cs.length(); // length of cs
        final int m = t.length(); // length of t

        if (n == 0) return m;
        else if (m == 0) return n;

        int[] previousCosts = new int[n + 1]; // 'previous' cost array, horizontally
        int[] costs = new int[n + 1]; // cost array, horizontally
        int[] swap; // placeholder to assist in swapping previousCosts and costs

        // indexes into strings cs and t
        int i; // iterates through cs
        int j; // iterates through t

        char t_j; // jth character of t

        int cost; // cost

        for (i = 0; i <= n; i++) {
            previousCosts[i] = i;
        }

        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            costs[0] = j;

            for (i = 1; i <= n; i++) {
                cost = cs.charAt(i - 1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left
                // and up +cost
                costs[i] = Math.min(Math.min(costs[i - 1] + 1, previousCosts[i] + 1), previousCosts[i - 1] + cost);
            }

            // copy current distance counts to 'previous row' distance counts
            swap = previousCosts;
            previousCosts = costs;
            costs = swap;
        }

        // our last action in the above loop was to switch costs and previousCosts, so previousCosts now
        // actually has the most recent cost counts
        return previousCosts[n];
    }

    /**
     * Checks whether the supplied String (when trimmed) is either null or empty
     *
     * @param in the String to check
     * @return true if the String is not null, nor empty
     */
    public static boolean isNotNullOrEmpty(@Nullable final String in) {
        return !isNullOrEmpty(in);
    }

    /**
     * Checks whether the supplied String (when trimmed) is either null or empty
     *
     * @param in the String to check
     * @return true if the String is null or empty
     */
    public static boolean isNullOrEmpty(@Nullable final String in) {
        return in == null || in.trim().isEmpty();
    }

    /**
     * Same as String.trim() except it only removes space and tab characters, nothing else.
     *
     * @param string the String to trim
     * @return a trimmed String
     */
    public static String limitedTrim(final String string) {
        int start = 0;
        char[] chars = string.toCharArray();
        for (char aChar : chars) {
            if (aChar == ' ' || aChar == '\t') ++start;
            else break;
        }

        if (start == chars.length) return "";

        int end = chars.length;
        for (int i = chars.length - 1; i >= start; --i) {
            if (chars[i] == ' ' || chars[i] == '\t') --end;
            else break;
        }
        if (end == chars.length) return string.substring(start);
        else return string.substring(start, end);
    }

    /**
     * Validates the supplied input against the supplied ValidationType
     *
     * @param type  the type of Validation to use
     * @param input the input to validate
     * @return true if the validation succeeds
     */
    public static boolean validateInput(final ValidationType type, final CharSequence input) {
        return type.matches(checkNotNull(input));
    }

    /**
     * @param theEnum the enum to prettify the name of
     * @return a more human readable version of the Enum's name
     */
    public static String prettifyEnumName(final Enum<?> theEnum) {
        String out = lowerCase(theEnum.name());
        out = out.replace('_', ' ');
        return capitalizeFirstLetters(out, true);
    }

    /**
     * @param string the String to convert
     * @return the String converted to a String that looks like Enum names usually do
     */
    public static String getAsEnumStyleName(final String string) {
        String out = string.toUpperCase();
        out = out.replace(' ', '_');
        return out;
    }

    /**
     * Splits the supplied sequence on spaces
     *
     * @param sequence the sequence to split
     * @return a String[] with the split
     */
    public static String[] splitOnSpace(final CharSequence sequence) {
        return RegexUtil.SPACE_PATTERN.split(checkNotNull(sequence));
    }

    /**
     * Splits the supplied sequence on commas
     *
     * @param sequence the sequence to split
     * @return a String[] with the split
     */
    public static String[] splitOnComma(final CharSequence sequence) {
        return RegexUtil.COMMA_PATTERN.split(checkNotNull(sequence));
    }

    /**
     * Splits the supplied sequence on commas
     *
     * @param sequence the sequence to split
     * @return a String[] with the split
     */
    public static String[] splitOnPeriod(final CharSequence sequence) {
        return RegexUtil.PERIOD_PATTERN.split(checkNotNull(sequence));
    }

    /**
     * Splits the supplied sequence on line feeds
     *
     * @param sequence the sequence to split
     * @return a String[] with the split
     */
    public static String[] splitOnEndOfLine(final CharSequence sequence) {
        return RegexUtil.NEW_LINE_PATTERN.split(checkNotNull(sequence));
    }

    /**
     * Splits the supplied sequence on tabs
     *
     * @param sequence the sequence to split
     * @return a String[] with the split
     */
    public static String[] splitOnTab(final CharSequence sequence) {
        return RegexUtil.TAB_PATTERN.split(checkNotNull(sequence));
    }

    /**
     * Merges the Strings from the Iterable into a single String seperated by the specified seperator
     *
     * @param strings   the Iterable of String to merge
     * @param seperator the seperator to use
     * @return a String containing the merged Strings
     */
    public static String merge(final Iterable<String> strings, final char seperator) {
        return Joiner.on(seperator).join(strings);
    }

    /**
     * Merges the Strings from the Iterable into a single String seperated by the specified seperator
     *
     * @param strings   the Iterable of String to merge
     * @param seperator the seperator to use
     * @return a String containing the merged Strings
     */
    public static String merge(final Iterable<String> strings, final String seperator) {
        return Joiner.on(seperator).join(strings);
    }

    /**
     * Merges the names from the array into a single String seperated by the specified seperator
     *
     * @param parts     the array of objects to merge the names for
     * @param seperator the seperator to use
     * @return a String containing the merged names
     */
    public static <E extends HasName> String mergeNamed(final E[] parts, final char seperator) {
        StringBuilder builder = new StringBuilder(500);
        for (HasName part : parts) {
            if (builder.length() > 0) builder.append(seperator);
            builder.append(part.getName());
        }
        return builder.toString();
    }

    /**
     * Checks if the specified String starts with any of the alternatives
     *
     * @param string            the String to check against
     * @param startAlternatives the alternative start Strings
     * @return true if the String starts with any of the alternatives
     */
    public static boolean startsWith(final String string, final String... startAlternatives) {
        for (String startAlternative : startAlternatives) {
            if (string.startsWith(startAlternative)) return true;
        }
        return false;
    }

    /**
     * Cuts the specified String according to the specified interval
     *
     * @param whole    the String to cut
     * @param interval the interval at which to cut
     * @return a List of Strings cut from the whole
     */
    public static List<String> slice(final String whole, final int interval) {
        if (whole.length() < interval) return Lists.newArrayList(whole);

        List<String> out = new ArrayList<>(whole.length() / interval + 1);
        for (int i = 0; i < whole.length(); i += interval) {
            out.add(whole.substring(i, Math.min(i + interval, whole.length())));
        }
        return out;
    }

    public static String cutToSize(final String string, final int length) {
        return string.length() <= length ? string : string.substring(0, length - 1);
    }

    /**
     * Pads or cuts the specified base String to the specified length. Meant for usage with IRC formatted content
     *
     * @param base     the base String
     * @param toLength the length the new String should be
     * @param padding  a char to pad with if necessary to reach the desired length
     * @return a new String with the exact length specified
     */
    public static String toFixLength(final String base, final int toLength, final char padding) {
        int baseLength = IRCFormattingUtil.lengthWithoutFormattingChars(checkNotNull(base));
        if (baseLength >= toLength) return base.substring(0, toLength);

        StringBuilder out = new StringBuilder(toLength);
        out.append(base);
        int paddingReq = toLength - baseLength;
        for (int i = 0; i < paddingReq; ++i) {
            out.append(padding);
        }
        return out.toString();
    }
}
