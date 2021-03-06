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

package commands.scripts;

import api.commands.Command;
import api.commands.CommandParser;
import commands.scripts.factories.ScriptCommandHandlerFactory;
import listeners.ScriptManager;
import spi.commands.CommandHandlerFactory;
import spi.commands.DynamicCommandHandlerFactoryGenerator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ScriptCommandHandlerFactoryGenerator implements DynamicCommandHandlerFactoryGenerator {
    private final ScriptManager scriptManager;

    @Inject
    public ScriptCommandHandlerFactoryGenerator(final ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    @Override
    public Collection<CommandHandlerFactory> generateCommandHandlerFactories() {
        Map<Command, List<CommandParser>> allHandledCommands = scriptManager.getAllHandledCommands();
        Collection<CommandHandlerFactory> out = new ArrayList<>(allHandledCommands.size());
        for (Map.Entry<Command, List<CommandParser>> handledCommandEntry : allHandledCommands.entrySet()) {
            out.add(new ScriptCommandHandlerFactory(handledCommandEntry.getKey(), handledCommandEntry.getValue(), scriptManager));
        }
        return out;
    }
}
