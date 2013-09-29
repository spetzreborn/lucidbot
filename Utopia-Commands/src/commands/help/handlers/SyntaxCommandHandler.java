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

import api.commands.*;
import api.database.daos.CommandDefinitionDAO;
import api.database.models.CommandDefinition;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.settings.PropertiesConfig;
import api.tools.collections.Params;
import api.tools.text.StringUtil;
import spi.commands.CommandHandler;
import spi.commands.CommandHandlerFactory;
import spi.filters.Filter;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static api.tools.text.StringUtil.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public class SyntaxCommandHandler implements CommandHandler {
    private final CommandCache commandCache;
    private final CommandDefinitionDAO commandDefinitionDAO;
    private final String commandPrefix;

    @Inject
    public SyntaxCommandHandler(final CommandCache commandCache, final CommandDefinitionDAO commandDefinitionDAO,
                                @Named(PropertiesConfig.COMMANDS_PREFIX) final String commandPrefix) {
        this.commandCache = commandCache;
        this.commandDefinitionDAO = commandDefinitionDAO;
        this.commandPrefix = commandPrefix;
    }

    @Override
    public CommandResponse handleCommand(final IRCContext context, final Params params, final Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        Command command = commandCache.getCommandFromName(params.getParameter("command"));
        if (command == null) return CommandResponse.errorResponse("There's no such command");

        String syntax = commandPrefix + command.getName() + ' ';
        CommandDefinition commandDefinition = commandDefinitionDAO.getCommandDefinition(command.getName());
        if (commandDefinition == null || isNullOrEmpty(commandDefinition.getSyntax())) {
            if (isNullOrEmpty(command.getSyntax())) {
                CommandHandlerFactory factory = commandCache.getFactoryForCommand(command);
                List<String> syntaxVariants = new ArrayList<>();
                for (CommandParser parser : factory.getParsers()) {
                    syntaxVariants.add(parser.getSyntaxDescription());
                }
                syntax += StringUtil.merge(syntaxVariants, " | " + commandPrefix + command.getName() + ' ');
            } else syntax = command.getSyntax();
        } else syntax += commandDefinition.getSyntax();
        return CommandResponse.resultResponse("syntax", nullToEmpty(syntax));
    }
}
