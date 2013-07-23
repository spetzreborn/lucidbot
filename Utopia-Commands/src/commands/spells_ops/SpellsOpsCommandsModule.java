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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import commands.spells_ops.factories.ListCommandHandlerFactory;
import commands.spells_ops.factories.ResetCommandHandlerFactory;
import commands.spells_ops.factories.ResultsCommandHandlerFactory;
import commands.spells_ops.factories.TargetCommandHandlerFactory;
import spi.commands.CommandHandlerFactory;
import spi.commands.DynamicCommandHandlerFactoryGenerator;

public class SpellsOpsCommandsModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<CommandHandlerFactory> binder = Multibinder.newSetBinder(binder(), CommandHandlerFactory.class);
        binder.addBinding().to(ListCommandHandlerFactory.class);
        binder.addBinding().to(ResetCommandHandlerFactory.class);
        binder.addBinding().to(ResultsCommandHandlerFactory.class);
        binder.addBinding().to(TargetCommandHandlerFactory.class);

        Multibinder<DynamicCommandHandlerFactoryGenerator> genBinder = Multibinder
                .newSetBinder(binder(), DynamicCommandHandlerFactoryGenerator.class);
        genBinder.addBinding().to(SpellsOpsFactoryGenerator.class);
    }
}
