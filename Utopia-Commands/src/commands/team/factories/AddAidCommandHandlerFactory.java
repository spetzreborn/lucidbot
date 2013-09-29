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
import api.settings.PropertiesConfig;
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.team.handlers.AddAidCommandHandler;
import database.models.AidImportanceType;
import database.models.AidType;
import spi.commands.CommandHandler;
import spi.commands.CommandHandlerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class AddAidCommandHandlerFactory implements CommandHandlerFactory {
    private final Command handledCommand = CommandBuilder.forCommand("addaid").ofType(CommandTypes.KD_MANAGEMENT).build();
    private final List<CommandParser> parsers = new ArrayList<>();

    private final Provider<AddAidCommandHandler> handlerProvider;

    @Inject
    public AddAidCommandHandlerFactory(final Provider<AddAidCommandHandler> handlerProvider,
                                       @Named(PropertiesConfig.COMMANDS_PREFIX) final String prefix) {
        this.handlerProvider = handlerProvider;

        handledCommand.setHelpText("Adds an aid request or aid offer. Requires a province to be used");
        handledCommand.setSyntax(prefix + "addaid _user_ <amount> <type> _importance_ _expiry binding_ Examples: " +
                prefix + "addaid 50k food medium {expires 24} (50k food, medium prio, expires in 24 hours), " +
                prefix + "addaid 100k gc high (100k gcs, high prio)");

        ParamParsingSpecification user = new ParamParsingSpecification("user", ValidationType.NICKNAME.getPattern(),
                CommandParamGroupingSpecification.OPTIONAL);
        ParamParsingSpecification amount = new ParamParsingSpecification("amount", ValidationType.DOUBLE_WITH_K.getPattern());
        ParamParsingSpecification type = new ParamParsingSpecification("type", AidType.getAliasesGroup());
        ParamParsingSpecification importance = new ParamParsingSpecification("importance", AidImportanceType.getGroup(),
                CommandParamGroupingSpecification.OPTIONAL);
        ParamParsingSpecification bindings = new ParamParsingSpecification("bindings", "\\{[^\\}]+\\}",
                CommandParamGroupingSpecification.OPTIONAL);
        parsers.add(new CommandParser(user, amount, type, importance, bindings));
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
