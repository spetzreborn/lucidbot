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

package commands.management.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.common.ReflectionUtil;
import api.tools.numbers.NumberUtil;
import api.tools.text.RegexUtil;
import database.daos.BuildDAO;
import database.daos.BuildingDAO;
import database.models.Bindings;
import database.models.Build;
import database.models.BuildEntry;
import database.models.Building;
import events.BuildAddedEvent;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AddBuildCommandHandler implements CommandHandler {
    private final BuildingDAO buildingDAO;
    private final BuildDAO buildDAO;
    private final BindingsManager bindingsManager;

    @Inject
    public AddBuildCommandHandler(final BindingsManager bindingsManager, final BuildDAO buildDAO, final BuildingDAO buildingDAO) {
        this.bindingsManager = bindingsManager;
        this.buildDAO = buildDAO;
        this.buildingDAO = buildingDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Bindings bindings = bindingsManager.parseBindings(params.getParameter("bindings"));
            if (bindings.getRaces().isEmpty() && bindings.getPersonalities().isEmpty())
                return CommandResponse.errorResponse("Either a race or personality must be specified, as a minimum");

            String buildType;
            if (params.containsKey("buildType")) {
                buildType = params.getParameter("buildType").substring(1);
                buildType = buildType.substring(0, buildType.length() - 1).trim();
            } else buildType = "default";
            String buildSpec = params.getParameter("build");
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(buildSpec);
            List<BuildEntry> entries = new ArrayList<>();
            Build build = new Build(bindings, buildType, context.getUser().getMainNick());
            for (int i = 0; i < split.length; i += 2) {
                String type = split[i];
                Double value = NumberUtil.parseDouble(split[i + 1]);
                Building building = buildingDAO.getBuilding(type);
                if (building == null) setValueToBuild(build, type, value);
                else entries.add(new BuildEntry(build, building, value));
            }
            build.setBuildings(entries);

            List<Build> existing = buildDAO.getBuilds(build.getRace(), build.getPersonality(), buildType);
            if (!existing.isEmpty()) buildDAO.delete(existing.get(0));

            buildDAO.save(build);
            delayedEventPoster.enqueue(new BuildAddedEvent(build.getId(), context));
            return CommandResponse.resultResponse("build", build);
        } catch (IllegalAccessException e) {
            throw new CommandHandlingException(e);
        }
    }

    private void setValueToBuild(final Build build, final String type, final Double value) throws IllegalAccessException {
        try {
            ReflectionUtil.setFieldValue(build, type, value);
        } catch (IllegalArgumentException e) {
            ReflectionUtil.setFieldValue(build, type, value.intValue());
        }
    }
}
