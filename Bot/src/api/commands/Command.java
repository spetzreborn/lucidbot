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

import api.common.HasName;
import api.database.models.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static api.tools.text.StringUtil.lowerCase;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class that represents a command that the bot supports
 */
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
@ToString
@Getter
@ParametersAreNonnullByDefault
public final class Command implements HasName, Comparable<Command> {
    /**
     * The name of the command, which is also the primary way to invoke it
     */
    private String name;
    /**
     * A description of how to use the command
     */
    private String syntax = "";
    /**
     * A help text for the command, should describe what it does
     */
    private String helpText = "";
    /**
     * The type of command this is. Useful for grouping commands together
     */
    private String commandType = "unspecified";
    /**
     * The lowest access level required to use this command
     */
    private AccessLevel requiredAccessLevel = AccessLevel.USER;
    /**
     * The filename of the file containing the template that specifies how to output the result of this command.
     * Defaults to the command name with the .ftl file extension.
     */
    private String templateFile;

    public Command(final String name,
                   @Nullable final String syntax,
                   @Nullable final String helpText,
                   @Nullable final String commandType,
                   final AccessLevel requiredAccessLevel) {
        this.name = checkNotNull(name);
        this.syntax = syntax;
        this.helpText = helpText;
        this.commandType = commandType == null ? null : lowerCase(commandType);
        this.requiredAccessLevel = checkNotNull(requiredAccessLevel);
        this.templateFile = lowerCase(name);
    }

    public Command setName(final String name) {
        this.name = checkNotNull(name);
        return this;
    }

    public Command setSyntax(@Nullable final String syntax) {
        this.syntax = syntax;
        return this;
    }

    public Command setHelpText(@Nullable final String helpText) {
        this.helpText = helpText;
        return this;
    }

    public Command setCommandType(@Nullable final String commandType) {
        this.commandType = commandType == null ? null : lowerCase(commandType);
        return this;
    }

    public Command setTemplateFile(final String templateFile) {
        this.templateFile = checkNotNull(templateFile);
        return this;
    }

    public Command setRequiredAccessLevel(final AccessLevel requiredAccessLevel) {
        this.requiredAccessLevel = checkNotNull(requiredAccessLevel);
        return this;
    }

    @Override
    public int compareTo(final Command o) {
        int cmdTypeComp = commandType.compareTo(o.getCommandType());
        return cmdTypeComp == 0 ? name.compareToIgnoreCase(o.getName()) : cmdTypeComp;
    }
}
