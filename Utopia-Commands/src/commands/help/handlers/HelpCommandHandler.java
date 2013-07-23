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
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.HelpTopicCollectionDAO;
import database.daos.HelpTopicDAO;
import database.models.HelpTopic;
import database.models.HelpTopicCollection;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class HelpCommandHandler implements CommandHandler {
    private final HelpTopicDAO helpTopicDAO;
    private final HelpTopicCollectionDAO helpTopicCollectionDAO;
    private final CommandCache commandCache;

    @Inject
    public HelpCommandHandler(final CommandCache commandCache, final HelpTopicDAO helpTopicDAO,
                              final HelpTopicCollectionDAO helpTopicCollectionDAO) {
        this.helpTopicDAO = helpTopicDAO;
        this.commandCache = commandCache;
        this.helpTopicCollectionDAO = helpTopicCollectionDAO;
    }

    @Override
    public CommandResponse handleCommand(final IRCContext context, final Params params, final Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            if (params.isEmpty()) {
                Collection<String> commandTypes = new TreeSet<>();
                for (Command command : commandCache.getAllCommands()) {
                    commandTypes.add(command.getCommandType());
                }
                return CommandResponse
                        .resultResponse("commandTypes", commandTypes, "topics", helpTopicDAO.getTopLevelTopics(), "collections",
                                helpTopicCollectionDAO.getTopLevelCollections());
            } else {
                CommandResponse data = getBestMatch(params.getParameter("item"));
                return data == null ? CommandResponse.errorResponse("Nothing found with that name") : data;
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private CommandResponse getBestMatch(final String item) {
        HelpTopic topic = helpTopicDAO.getByName(item);
        if (topic != null) return CommandResponse.resultResponse("topic", topic);

        HelpTopicCollection topicCollection = helpTopicCollectionDAO.getByName(item);
        if (topicCollection != null) return CommandResponse.resultResponse("collection", topicCollection);

        Collection<Command> commands = getBestCommandAreaMatch(item);
        if (!commands.isEmpty()) return CommandResponse.resultResponse("commands", commands);

        Command command = this.commandCache.getCommandFromName(item);
        if (command != null) return CommandResponse.resultResponse("command", command);

        return null;
    }

    private Collection<Command> getBestCommandAreaMatch(final String typeString) {
        Set<Command> commandsOfType = new TreeSet<>();
        for (Command command : commandCache.getAllCommands()) {
            if (command.getCommandType().equals(typeString)) commandsOfType.add(command);
        }
        return commandsOfType;
    }
}
