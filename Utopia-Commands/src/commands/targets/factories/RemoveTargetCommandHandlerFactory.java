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

package commands.targets.factories;

import api.commands.*;
import api.database.models.AccessLevel;
import api.irc.ValidationType;
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.targets.handlers.RemoveTargetCommandHandler;
import spi.commands.CommandHandler;
import spi.commands.CommandHandlerFactory;
import tools.parsing.UtopiaValidationType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class RemoveTargetCommandHandlerFactory implements CommandHandlerFactory {
    private final Command handledCommand = CommandBuilder.forCommand("removetarget").ofType(CommandTypes.TARGETS).requiringAccessLevel(AccessLevel.ADMIN).build();
    private final List<CommandParser> parsers = new ArrayList<>();

    private final Provider<RemoveTargetCommandHandler> handlerProvider;

    @Inject
    public RemoveTargetCommandHandlerFactory(final Provider<RemoveTargetCommandHandler> handlerProvider) {
        this.handlerProvider = handlerProvider;

        handledCommand.setHelpText("Removes the specified target from the targets list");

        ParamParsingSpecification id = new ParamParsingSpecification("id", ValidationType.INT.getPattern());
        ParamParsingSpecification target = new ParamParsingSpecification("target", "[^(]+");
        ParamParsingSpecification kd = new ParamParsingSpecification("kd", UtopiaValidationType.KDLOC.getPatternString(),
                CommandParamGroupingSpecification.OPTIONAL);
        parsers.add(new CommandParser(id));
        parsers.add(new CommandParser(target, kd));
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
