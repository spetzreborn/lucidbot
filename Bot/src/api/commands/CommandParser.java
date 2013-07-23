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

package api.commands;

import api.tools.collections.CommandParams;
import api.tools.collections.Params;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.tools.text.StringUtil.isNullOrEmpty;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class which represents a parsable specification of a command, meaning if the command
 * matches the specification, it can be parsed by this class
 */
@ParametersAreNonnullByDefault
public final class CommandParser {
    /**
     * @return a CommandParser that matches a command that doesn't have any parameters other than the command name
     */
    public static CommandParser getEmptyParser() {
        return new CommandParser();
    }

    private final ParamParsingSpecification[] params;
    private final Pattern pattern;

    private CommandParser() {
        params = null;
        pattern = null;
    }

    public CommandParser(final ParamParsingSpecification... params) {
        this.params = checkNotNull(params);
        StringBuilder sb = new StringBuilder(150);
        for (final ParamParsingSpecification pair : params) {
            sb = pair.getSpec().appendGroup(sb, pair.getRegex());
        }
        this.pattern = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    private boolean isEmpty() {
        return params == null;
    }

    /**
     * Checks if the specified command matches the specifications of this parser
     *
     * @param command the command to check
     * @return true if the command matches the specifications (and therefore can be parsed by this parser)
     */
    public boolean matches(final String command) {
        checkNotNull(command);
        return (isEmpty() && isNullOrEmpty(command)) || (pattern != null && pattern.matcher(command).matches());
    }

    /**
     * Parses the command and returns a Params object containing the params that were found.
     * If the command did not have any params, the Params object is simply empty.
     * <p/>
     * If the command didn't match the specifications of this parser, null is returned.
     *
     * @param command the command to parse
     * @return the Params parsed from the command, or null if the command didn't match this parser's specs
     */
    @Nullable
    public Params parse(final CharSequence command) {
        if (params == null || params.length == 0) return new CommandParams(Collections.<String, String>emptyMap());
        Matcher matcher = pattern.matcher(checkNotNull(command));
        if (!matcher.matches() || matcher.groupCount() != params.length) return null;
        Map<String, String> parsedParams = new HashMap<>();
        for (int i = 0; i < params.length; ++i) {
            parsedParams.put(params[i].getName(), matcher.group(i + 1));
        }
        return new CommandParams(parsedParams);
    }

    /**
     * @return A description of the syntax as declared by this CommandParser
     */
    public String getSyntaxDescription() {
        if (params == null || params.length == 0) return "";

        StringBuilder builder = new StringBuilder(100);
        for (final ParamParsingSpecification param : params) {
            builder.append(param.getSpec().decorateWithSpecification(param.getName())).append(' ');
        }
        return builder.toString().trim();
    }
}
