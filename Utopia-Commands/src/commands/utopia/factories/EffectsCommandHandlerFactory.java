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

package commands.utopia.factories;

import api.commands.*;
import api.database.models.AccessLevel;
import api.irc.ValidationType;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.utopia.handlers.EffectsCommandHandler;
import database.CommonEntitiesAccess;
import events.CacheReloadEvent;
import spi.commands.CommandHandler;
import spi.commands.CommandHandlerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
public class EffectsCommandHandlerFactory implements CommandHandlerFactory {
    private final Command handledCommand = CommandBuilder.forCommand("effects").ofType(CommandTypes.UTOPIA).requiringAccessLevel(AccessLevel.PUBLIC).build();
    private final List<CommandParser> parsers = new ArrayList<>();

    private final Provider<EffectsCommandHandler> handlerProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    @Inject
    public EffectsCommandHandlerFactory(final Provider<EffectsCommandHandler> handlerProvider,
                                        final CommonEntitiesAccess commonEntitiesAccess) {
        this.handlerProvider = handlerProvider;
        this.commonEntitiesAccess = commonEntitiesAccess;

        handledCommand
                .setHelpText("Used for calculating what buildings and sciences do, or to read up on what " + "different spells and ops do");

        loadParsers();
    }

    @Inject
    public void init(final EventBus eventBus) {
        eventBus.register(this);
    }

    private void loadParsers() {
        parsers.clear();

        String buildings = commonEntitiesAccess.getBuildingGroup();
        String sciences = commonEntitiesAccess.getScienceTypeGroup();
        String ops = commonEntitiesAccess.getOpGroup(true);
        String spells = commonEntitiesAccess.getSpellGroup(true);

        ParamParsingSpecification building = new ParamParsingSpecification("building", buildings);
        ParamParsingSpecification be = new ParamParsingSpecification("be", ValidationType.DOUBLE.getPattern() + '%',
                CommandParamGroupingSpecification.OPTIONAL);
        ParamParsingSpecification science = new ParamParsingSpecification("scienceType", sciences);
        ParamParsingSpecification bonus = new ParamParsingSpecification("bonus", ValidationType.DOUBLE.getPattern() + '%',
                CommandParamGroupingSpecification.OPTIONAL_REPEAT);
        ParamParsingSpecification percent = new ParamParsingSpecification("percent", ValidationType.DOUBLE.getPattern() + '%');
        ParamParsingSpecification amount = new ParamParsingSpecification("amount", ValidationType.INT.getPattern());
        ParamParsingSpecification bpa = new ParamParsingSpecification("bpa", ValidationType.INT.getPattern());
        ParamParsingSpecification spell = new ParamParsingSpecification("spell", spells);
        ParamParsingSpecification op = new ParamParsingSpecification("op", ops);
        parsers.add(new CommandParser(building, percent, be));
        parsers.add(new CommandParser(building, percent, amount, be));
        parsers.add(new CommandParser(building, amount, be));
        parsers.add(new CommandParser(science, percent, bonus));
        parsers.add(new CommandParser(science, bpa, bonus));
        parsers.add(new CommandParser(spell));
        parsers.add(new CommandParser(op));
    }

    @Subscribe
    public void onCacheReload(final CacheReloadEvent event) {
        loadParsers();
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
