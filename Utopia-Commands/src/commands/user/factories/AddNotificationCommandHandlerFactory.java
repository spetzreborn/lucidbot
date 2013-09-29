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

package commands.user.factories;

import api.commands.*;
import api.irc.ValidationType;
import api.settings.PropertiesConfig;
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.user.handlers.AddNotificationCommandHandler;
import database.models.NotificationMethod;
import database.models.NotificationType;
import spi.commands.CommandHandler;
import spi.commands.CommandHandlerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class AddNotificationCommandHandlerFactory implements CommandHandlerFactory {
    private final Command handledCommand = CommandBuilder.forCommand("addnotification").ofType(CommandTypes.USER).build();
    private final List<CommandParser> parsers = new ArrayList<>();

    private final Provider<AddNotificationCommandHandler> handlerProvider;

    @Inject
    public AddNotificationCommandHandlerFactory(final Provider<AddNotificationCommandHandler> handlerProvider,
                                                @Named(PropertiesConfig.COMMANDS_PREFIX) final String prefix) {
        this.handlerProvider = handlerProvider;

        handledCommand.setHelpText("Adds a notification, for example you could be notified via email when a new order is added");
        handledCommand.setSyntax(prefix + "addnotification <type> <method> See the website for which types and methods are available");

        ParamParsingSpecification type = new ParamParsingSpecification("type", NotificationType.getRegexGroup());
        ParamParsingSpecification method = new ParamParsingSpecification("method", NotificationMethod.getRegexGroup());
        ParamParsingSpecification user = new ParamParsingSpecification("user", "\\*|" + ValidationType.NICKNAME.getPattern(),
                CommandParamGroupingSpecification.OPTIONAL);
        parsers.add(new CommandParser(type, method, user));
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
