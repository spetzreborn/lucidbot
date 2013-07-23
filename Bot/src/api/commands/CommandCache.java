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

import api.database.daos.CommandDefinitionDAO;
import api.events.bot.CommandRemovedEvent;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.commands.CommandHandlerFactory;
import spi.commands.DynamicCommandHandlerFactoryGenerator;
import spi.events.EventListener;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static api.tools.text.StringUtil.lowerCase;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class that holds information about all the commands that are currently supported by the bot
 */
@Log4j
@ParametersAreNonnullByDefault
public final class CommandCache implements EventListener {
    private final ConcurrentMap<Command, CommandHandlerFactory> commandToHandlerFactoryMapping = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Command> nameToCommandMapping = new ConcurrentHashMap<>();
    private final Provider<CommandDefinitionDAO> commandDefinitionDAOProvider;

    @Inject
    public CommandCache(final Set<CommandHandlerFactory> commandHandlerFactorySet,
                        final Set<DynamicCommandHandlerFactoryGenerator> dynamicFactoryGeneratorSet,
                        final Provider<CommandDefinitionDAO> commandDefinitionDAOProvider) {
        this.commandDefinitionDAOProvider = commandDefinitionDAOProvider;

        for (final CommandHandlerFactory factory : commandHandlerFactorySet) {
            registerCommandFromFactory(factory);
        }

        for (final DynamicCommandHandlerFactoryGenerator generator : dynamicFactoryGeneratorSet) {
            for (final CommandHandlerFactory factory : generator.generateCommandHandlerFactories()) {
                registerCommandFromFactory(factory);
            }
        }

        refreshDefinitions();
    }

    public void refreshDefinitions() {
        try {
            commandDefinitionDAOProvider.get().loadDefinitionsFromDB(nameToCommandMapping.values());
        } catch (HibernateException e) {
            log.error("Could not load command definitions from db", e);
        }
    }

    private void registerCommandFromFactory(final CommandHandlerFactory factory) {
        Command handledCommand = factory.getHandledCommand();
        commandToHandlerFactoryMapping.put(handledCommand, factory);
        nameToCommandMapping.put(lowerCase(handledCommand.getName()), handledCommand);
    }

    /**
     * @param command the Command to get a handler factory for
     * @return a CommandHandlerFactory for the specified command, or null if no match is found
     */
    @Nullable
    public CommandHandlerFactory getFactoryForCommand(final Command command) {
        checkNotNull(command);
        return commandToHandlerFactoryMapping.get(command);
    }

    /**
     * @param command the name of a Command
     * @return the Command that matches the specified name, or null if no match is found
     */
    @Nullable
    public Command getCommandFromName(final String command) {
        checkNotNull(command);
        return nameToCommandMapping.get(lowerCase(command));
    }

    /**
     * @return a Collection of all supported Commands
     */
    public Collection<Command> getAllCommands() {
        return Collections.unmodifiableCollection(commandToHandlerFactoryMapping.keySet());
    }

    @Subscribe
    public void onCommandRemovedEvent(final CommandRemovedEvent event) {
        Command command = event.getCommand();
        nameToCommandMapping.remove(lowerCase(command.getName()));
        commandToHandlerFactoryMapping.remove(command);
    }
}
