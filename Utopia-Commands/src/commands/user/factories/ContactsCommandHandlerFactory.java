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
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.user.handlers.ContactsCommandHandler;
import spi.commands.CommandHandler;
import spi.commands.CommandHandlerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class ContactsCommandHandlerFactory implements CommandHandlerFactory {
    private final Command handledCommand = CommandFactory.newTypedCommand(CommandTypes.KD_MANAGEMENT, "contacts");
    private final List<CommandParser> parsers = new ArrayList<>();

    private final Provider<ContactsCommandHandler> handlerProvider;

    @Inject
    public ContactsCommandHandlerFactory(final Provider<ContactsCommandHandler> handlerProvider) {
        this.handlerProvider = handlerProvider;

        handledCommand.setHelpText(
                "Used for adding and checking contact information. \"Known types\" of contact info are: sms, email, gtalk, country & name, " +
                        "but you can add arbitrary ones as well (for example skype)");

        parsers.add(CommandParser.getEmptyParser());
        ParamParsingSpecification user = new ParamParsingSpecification("user", ValidationType.NICKNAME.getPattern());
        ParamParsingSpecification optionalUser = new ParamParsingSpecification("optionalUser", ValidationType.NICKNAME.getPattern(),
                CommandParamGroupingSpecification.OPTIONAL);
        ParamParsingSpecification add = new ParamParsingSpecification("add", "add");
        ParamParsingSpecification remove = new ParamParsingSpecification("remove", "remove");
        ParamParsingSpecification knownType = new ParamParsingSpecification("knownType", "sms|email|gtalk|country|name");
        ParamParsingSpecification unknownType = new ParamParsingSpecification("unknownType", "[^ ]+");
        ParamParsingSpecification contact = new ParamParsingSpecification("contact", ".+");
        parsers.add(new CommandParser(user));
        parsers.add(new CommandParser(optionalUser, add, knownType, contact));
        parsers.add(new CommandParser(optionalUser, remove, knownType));
        parsers.add(new CommandParser(optionalUser, add, unknownType, contact));
        parsers.add(new CommandParser(optionalUser, remove, unknownType));
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
