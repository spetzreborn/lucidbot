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

package commands.help.factories;

import api.commands.Command;
import api.commands.CommandBuilder;
import api.commands.CommandParser;
import api.commands.ParamParsingSpecification;
import api.database.models.AccessLevel;
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.help.handlers.AddLinkCommandHandler;
import spi.commands.CommandHandler;
import spi.commands.CommandHandlerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddLinkCommandHandlerFactory implements CommandHandlerFactory {
    private final Command handledCommand = CommandBuilder.forCommand("addlink").ofType(CommandTypes.HELP).requiringAccessLevel(AccessLevel.ADMIN).build();
    private final List<CommandParser> parsers = new ArrayList<>();

    private final Provider<AddLinkCommandHandler> handlerProvider;

    @Inject
    public AddLinkCommandHandlerFactory(final Provider<AddLinkCommandHandler> handlerProvider) {
        this.handlerProvider = handlerProvider;

        handledCommand.setHelpText("Adds a link/URL, similar to using favorites in browsers to remember useful links");

        ParamParsingSpecification command = new ParamParsingSpecification("nameAndLink", ".+\\s.+");
        parsers.add(new CommandParser(command));
    }

    @Override
    public Command getHandledCommand() {
        return handledCommand;
    }

    @Override
    public List<CommandParser> getParsers() {
        return Collections.unmodifiableList(parsers);
    }

    @Override
    public CommandHandler getCommandHandler() {
        return handlerProvider.get();
    }
}
