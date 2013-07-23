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

package commands.help.handlers;

import api.commands.Command;
import api.commands.CommandCache;
import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.CommandDefinitionDAO;
import api.database.models.CommandDefinition;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;

public class SetSyntaxCommandHandler implements CommandHandler {
    private final CommandDefinitionDAO definitionDAO;
    private final CommandCache commandCache;

    @Inject
    public SetSyntaxCommandHandler(final CommandCache commandCache, final CommandDefinitionDAO definitionDAO) {
        this.commandCache = commandCache;
        this.definitionDAO = definitionDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Command command = commandCache.getCommandFromName(params.getParameter("command"));
            if (command == null) return CommandResponse.errorResponse("No such command found");

            CommandDefinition def = getDefinition(definitionDAO, command);
            def.setSyntax(params.getParameter("syntax"));
            command.setSyntax(params.getParameter("syntax"));
            return CommandResponse.resultResponse("command", def.getName(), "syntax", def.getSyntax());
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static CommandDefinition getDefinition(final CommandDefinitionDAO dao, final Command command) {
        CommandDefinition def = dao.getCommandDefinition(command.getName());
        if (def == null) {
            def = dao.createFromCommand(command);
        }
        return def;
    }
}
