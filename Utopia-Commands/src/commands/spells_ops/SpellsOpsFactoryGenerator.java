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

package commands.spells_ops;

import api.commands.Command;
import api.commands.CommandBuilder;
import com.google.inject.Provider;
import commands.CommandTypes;
import commands.spells_ops.factories.SpellsOpsCommandHandlerFactory;
import commands.spells_ops.handlers.SpellsOpsCommandHandler;
import database.CommonEntitiesAccess;
import database.daos.ProvinceDAO;
import database.models.OpType;
import database.models.SpellOpCharacter;
import database.models.SpellType;
import spi.commands.CommandHandlerFactory;
import spi.commands.DynamicCommandHandlerFactoryGenerator;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static api.tools.text.StringUtil.lowerCase;

@Singleton
public class SpellsOpsFactoryGenerator implements DynamicCommandHandlerFactoryGenerator {
    private final List<Command> handledCommands = new ArrayList<>();
    private final CommonEntitiesAccess commonEntitiesAccess;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final UtopiaTimeFactory utopiaTimeFactory;

    @Inject
    public SpellsOpsFactoryGenerator(final CommonEntitiesAccess commonEntitiesAccess, final Provider<ProvinceDAO> provinceDAOProvider,
                                     final UtopiaTimeFactory utopiaTimeFactory) {
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.provinceDAOProvider = provinceDAOProvider;
        this.utopiaTimeFactory = utopiaTimeFactory;

        Command command;
        for (OpType opType : commonEntitiesAccess
                .getOpTypesNotLike(SpellOpCharacter.OTHER, SpellOpCharacter.SELF_SPELLOP, SpellOpCharacter.INSTANT_SELF_SPELLOP)) {
            final String name = opType.getShortName() == null ? opType.getName() : opType.getShortName();
            command = CommandBuilder.forCommand(lowerCase(name)).ofType(CommandTypes.SPELLS_AND_OPS).usingTemplateFile("spellsops.ftl").
                    withHelpText("Used for adding " + opType.getName()).withNonDowngradableAccessLevel().build();
            handledCommands.add(command);
        }
        for (SpellType spellType : commonEntitiesAccess
                .getSpellTypesNotLike(SpellOpCharacter.OTHER, SpellOpCharacter.SELF_SPELLOP, SpellOpCharacter.INSTANT_SELF_SPELLOP)) {
            final String name = spellType.getShortName() == null ? spellType.getName() : spellType.getShortName();
            command = CommandBuilder.forCommand(lowerCase(name)).ofType(CommandTypes.SPELLS_AND_OPS).usingTemplateFile("spellsops.ftl").
                    withHelpText("Used for adding " + spellType.getName()).build();
            handledCommands.add(command);
        }
    }

    @Override
    public Collection<CommandHandlerFactory> generateCommandHandlerFactories() {
        List<CommandHandlerFactory> out = new ArrayList<>(handledCommands.size());
        SpellsOpsCommandHandler handler;
        for (Command command : handledCommands) {
            handler = new SpellsOpsCommandHandler(commonEntitiesAccess, provinceDAOProvider, utopiaTimeFactory);
            out.add(new SpellsOpsCommandHandlerFactory(command, handler));
        }
        return out;
    }
}
