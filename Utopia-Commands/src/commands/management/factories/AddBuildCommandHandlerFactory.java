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

package commands.management.factories;

import api.commands.*;
import api.irc.ValidationType;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.management.handlers.AddBuildCommandHandler;
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
public class AddBuildCommandHandlerFactory implements CommandHandlerFactory {
    private final Command handledCommand = CommandFactory.newTypedAdminCommand(CommandTypes.KD_MANAGEMENT, "addbuild");
    private final List<CommandParser> parsers = new ArrayList<>();

    private final Provider<AddBuildCommandHandler> handlerProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    @Inject
    public AddBuildCommandHandlerFactory(final Provider<AddBuildCommandHandler> handlerProvider,
                                         final CommonEntitiesAccess commonEntitiesAccess) {
        this.handlerProvider = handlerProvider;
        this.commonEntitiesAccess = commonEntitiesAccess;

        handledCommand.setHelpText("Adds a build. The build type is the 'name' of this build, and is marked by ()");

        loadParsers();
    }

    @Inject
    public void init(final EventBus eventBus) {
        eventBus.register(this);
    }

    private void loadParsers() {
        parsers.clear();
        ParamParsingSpecification type = new ParamParsingSpecification("buildType", "\\([^(]+\\)",
                CommandParamGroupingSpecification.OPTIONAL);
        ParamParsingSpecification bindings = new ParamParsingSpecification("bindings", "\\{[^\\}]+\\}");
        String buildings = commonEntitiesAccess.getBuildingGroup();
        ParamParsingSpecification build = new ParamParsingSpecification("build",
                "(?:wpa|tpa|epa|ospa|dspa|bpa|land|" + buildings + ")\\s+(?:" + ValidationType.DOUBLE.getPattern() + ')',
                CommandParamGroupingSpecification.REPEAT);
        parsers.add(new CommandParser(type, bindings, build));
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
