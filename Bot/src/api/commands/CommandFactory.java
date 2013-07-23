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

import api.database.models.AccessLevel;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory class that creates {@link Command} objects
 */
@ParametersAreNonnullByDefault
public class CommandFactory {
    private CommandFactory() {
    }

    /**
     * Creates a command with the type unspecified, with all the default settings
     *
     * @param name the name of the command
     * @return a new Command
     */
    public static Command newSimpleCommand(final String name) {
        checkNotNull(name);
        return new Command().setName(name).setTemplateFile(name + ".ftl");
    }

    /**
     * Creates a command with the type unspecified which is for admins only
     *
     * @param name the name of the command
     * @return a new Command
     */
    public static Command newSimpleAdminCommand(final String name) {
        return newSimpleCommand(name).setRequiredAccessLevel(AccessLevel.ADMIN);
    }

    /**
     * Creates a command with the type unspecified which is available for everyone, including unregistered users
     *
     * @param name the name of the command
     * @return a new Command
     */
    public static Command newSimplePublicCommand(final String name) {
        return newSimpleCommand(name).setRequiredAccessLevel(AccessLevel.PUBLIC);
    }

    /**
     * Creates a command of the specified type and the default settings
     *
     * @param type the type of the command
     * @param name the name of the command
     * @return a new Command
     */
    public static Command newTypedCommand(final String type, final String name) {
        return newSimpleCommand(name).setCommandType(type);
    }

    /**
     * Creates a command of the specified type which is for admins only
     *
     * @param type the type of the command
     * @param name the name of the command
     * @return a new Command
     */
    public static Command newTypedAdminCommand(final String type, final String name) {
        return newTypedCommand(type, name).setRequiredAccessLevel(AccessLevel.ADMIN);
    }

    /**
     * Creates a command of the specified type which is available for everyone, including unregistered users
     *
     * @param type the type of the command
     * @param name the name of the command
     * @return a new Command
     */
    public static Command newTypedPublicCommand(final String type, final String name) {
        return newTypedCommand(type, name).setRequiredAccessLevel(AccessLevel.PUBLIC);
    }
}
