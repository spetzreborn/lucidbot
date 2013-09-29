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

package commands.army.factories;

import api.commands.Command;
import api.commands.CommandBuilder;
import api.commands.CommandParser;
import api.commands.ParamParsingSpecification;
import api.irc.ValidationType;
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.army.handlers.MOCommandHandler;
import spi.commands.CommandHandler;
import spi.commands.CommandHandlerFactory;
import tools.parsing.UtopiaValidationType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class MOCommandHandlerFactory implements CommandHandlerFactory {
    private final Command handledCommand = CommandBuilder.forCommand("mo").ofType(CommandTypes.MILITARY).build();
    private final List<CommandParser> parsers = new ArrayList<>();

    private final Provider<MOCommandHandler> handlerProvider;

    @Inject
    public MOCommandHandlerFactory(final Provider<MOCommandHandler> handlerProvider) {
        this.handlerProvider = handlerProvider;

        handledCommand
                .setHelpText("Sets mod off (and optionally generals) either for the latest added army, or the one specified by army #");

        ParamParsingSpecification mo = new ParamParsingSpecification("mo", ValidationType.DOUBLE_WITH_K.getPattern());
        ParamParsingSpecification armyno = new ParamParsingSpecification("armyno", ValidationType.INT.getPattern());
        ParamParsingSpecification generals = new ParamParsingSpecification("generals", UtopiaValidationType.GENERAL.getPatternString());
        parsers.add(new CommandParser(armyno, mo, generals));
        parsers.add(new CommandParser(mo, generals));
        parsers.add(new CommandParser(mo));
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
