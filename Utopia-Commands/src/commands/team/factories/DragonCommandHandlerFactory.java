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

package commands.team.factories;

import api.commands.*;
import api.irc.ValidationType;
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.team.handlers.DragonCommandHandler;
import spi.commands.CommandHandler;
import spi.commands.CommandHandlerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DragonCommandHandlerFactory implements CommandHandlerFactory {
    private final Command handledCommand = CommandBuilder.forCommand("dragon").ofType(CommandTypes.KD_MANAGEMENT).build();
    private final List<CommandParser> parsers = new ArrayList<>();

    private final Provider<DragonCommandHandler> handlerProvider;

    @Inject
    public DragonCommandHandlerFactory(final Provider<DragonCommandHandler> handlerProvider) {
        this.handlerProvider = handlerProvider;

        handledCommand.setHelpText("Used for checking dragon stats for users, both killing and sending");

        ParamParsingSpecification id = new ParamParsingSpecification("id", ValidationType.INT.getPattern());
        ParamParsingSpecification sending = new ParamParsingSpecification("sending", "sending");
        ParamParsingSpecification killing = new ParamParsingSpecification("killing", "killing");
        ParamParsingSpecification users = new ParamParsingSpecification("users", ValidationType.NICKNAME.getPattern(),
                CommandParamGroupingSpecification.OPTIONAL_REPEAT);
        ParamParsingSpecification all = new ParamParsingSpecification("all", "\\*");
        parsers.add(new CommandParser(id, users));
        parsers.add(new CommandParser(id, all));
        parsers.add(new CommandParser(sending, users));
        parsers.add(new CommandParser(sending, all));
        parsers.add(new CommandParser(killing, users));
        parsers.add(new CommandParser(killing, all));
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
