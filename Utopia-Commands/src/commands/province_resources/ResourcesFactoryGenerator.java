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

package commands.province_resources;

import api.commands.Command;
import api.commands.CommandBuilder;
import api.settings.PropertiesCollection;
import api.tools.text.StringUtil;
import commands.CommandTypes;
import commands.province_resources.factories.ResourcesCommandHandlerFactory;
import commands.province_resources.handlers.ResourcesCommandHandler;
import database.CommonEntitiesAccess;
import database.models.Building;
import intel.IntelManager;
import intel.ProvinceResourceType;
import spi.commands.CommandHandlerFactory;
import spi.commands.DynamicCommandHandlerFactoryGenerator;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class ResourcesFactoryGenerator implements DynamicCommandHandlerFactoryGenerator {
    private final Map<Command, ProvinceResourceType> handledCommands = new HashMap<>();
    private final PropertiesCollection properties;
    private final IntelManager intelManager;

    @Inject
    public ResourcesFactoryGenerator(final IntelManager intelManager, final CommonEntitiesAccess commonEntitiesAccess,
                                     final PropertiesCollection properties) {
        this.intelManager = intelManager;
        this.properties = properties;

        for (Building building : commonEntitiesAccess.getAllBuildings()) {
            Command command = CommandBuilder.forCommand(building.getShortName()).ofType(CommandTypes.INTEL).usingTemplateFile("provinceresources.ftl").
                    withHelpText("Displays " + building.getName() + " building percentages for provinces").build();
            handledCommands.put(command, ProvinceResourceType.BUILDING_PERCENTAGE);
        }

        for (ProvinceResourceType type : ProvinceResourceType.values()) {
            if (type.getCommand() != null) {
                Command command = CommandBuilder.forCommand(type.getCommand()).ofType(CommandTypes.INTEL).usingTemplateFile("provinceresources.ftl").
                        withHelpText("Displays " + StringUtil.prettifyEnumName(type) + " for provinces").build();
                handledCommands.put(command, type);
            }
        }
    }

    @Override
    public Collection<CommandHandlerFactory> generateCommandHandlerFactories() {
        List<CommandHandlerFactory> out = new ArrayList<>(handledCommands.size());
        ResourcesCommandHandler handler;
        for (Map.Entry<Command, ProvinceResourceType> entry : handledCommands.entrySet()) {
            Command command = entry.getKey();
            handler = new ResourcesCommandHandler(properties, handledCommands.get(command), intelManager);
            out.add(new ResourcesCommandHandlerFactory(command, handler));
        }
        return out;
    }
}
