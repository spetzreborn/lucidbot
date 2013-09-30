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

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Specifies how a CommandParser should group the regex of a given parameter
 */
@ParametersAreNonnullByDefault
public enum CommandParamGroupingSpecification {
    /**
     * Means the specific parameter is optional
     */
    OPTIONAL {
        @Override
        public StringBuilder appendGroup(final StringBuilder builder, final String paramName, final String regex) {
            return builder.append("(?:(?<").append(paramName).append('>').append(regex).append(')').append("(?:\\s+|$))").append('?');
        }

        @Override
        public String decorateWithSpecification(final String string) {
            return '_' + string + '_';
        }
    },
    /**
     * Means the specific parameter should appear at least once, and possibly more
     */
    REPEAT {
        @Override
        public StringBuilder appendGroup(final StringBuilder builder, final String paramName, final String regex) {
            return builder.append("(?<").append(paramName).append(">(?:(?:").append(regex).append(')').append("(?:\\s+|$))+").append(')');
        }

        @Override
        public String decorateWithSpecification(final String string) {
            return '+' + string + '+';
        }
    },
    /**
     * Means the specific parameter is optional, and may appear more than once too
     */
    OPTIONAL_REPEAT {
        @Override
        public StringBuilder appendGroup(final StringBuilder builder, final String paramName, final String regex) {
            return builder.append("(?<").append(paramName).append(">(?:(?:").append(regex).append(')').append("(?:\\s*|$))*").append(')');
        }

        @Override
        public String decorateWithSpecification(final String string) {
            return '*' + string + '*';
        }
    },
    /**
     * Means the specific parameter is not optional and may not be repeated
     */
    REGULAR {
        @Override
        public StringBuilder appendGroup(final StringBuilder builder, final String paramName, final String regex) {
            return builder.append("(?<").append(paramName).append('>').append(regex).append(')').append("(?:\\s+|$)");
        }

        @Override
        public String decorateWithSpecification(final String string) {
            return '<' + string + '>';
        }
    };

    /**
     * Appends the specified regex string according to this specification to the supplied builder
     * <p/>
     * For example, if the spec is that the regex should be optional, then the regex is added as an optional
     * capturing group.
     *
     * @param builder the StringBuilder to append the regex to
     * @param regex   the regex to append
     * @return the same StringBuilder that was sent in, with the regex appended according to this spec
     */
    public abstract StringBuilder appendGroup(final StringBuilder builder, final String paramName, final String regex);

    /**
     * Visualizes this spec, meaning it decorates the specified String to make it clear what that String
     * means when it comes to parsing. An easy example could be to make _blabla_ mean that blabla is optional.
     *
     * @param string the string to decorate
     * @return a decorated String
     */
    public abstract String decorateWithSpecification(final String string);
}
