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

package commands;

import com.google.inject.AbstractModule;
import commands.activity.ActivityCommandsModule;
import commands.army.ArmyCommandsModule;
import commands.bot.BotCommandsModule;
import commands.calculator.CalculatorCommandsModule;
import commands.communication.CommunicationCommandsModule;
import commands.forum.ForumCommandsModule;
import commands.help.HelpCommandsModule;
import commands.intel.IntelCommandsModule;
import commands.irc.IrcCommandsModule;
import commands.management.ManagementCommandsModule;
import commands.news.NewsCommandsModule;
import commands.province_management.ProvinceManagementCommandsModule;
import commands.province_resources.ProvinceResourcesCommandsModule;
import commands.scripts.ScriptCommandsModule;
import commands.spells_ops.SpellsOpsCommandsModule;
import commands.statistics.StatisticsCommandsModule;
import commands.targets.TargetsCommandsModule;
import commands.team.TeamCommandsModule;
import commands.time.TimeCommandsModule;
import commands.trivia.TriviaCommandsModule;
import commands.user.UserCommandsModule;
import commands.utopia.UtopiaCommandsModule;

public class CommandHandlingModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new ActivityCommandsModule());
        install(new ArmyCommandsModule());
        install(new BotCommandsModule());
        install(new CalculatorCommandsModule());
        install(new CommunicationCommandsModule());
        install(new ForumCommandsModule());
        install(new HelpCommandsModule());
        install(new IntelCommandsModule());
        install(new IrcCommandsModule());
        install(new ManagementCommandsModule());
        install(new NewsCommandsModule());
        install(new ProvinceManagementCommandsModule());
        install(new ProvinceResourcesCommandsModule());
        install(new ScriptCommandsModule());
        install(new SpellsOpsCommandsModule());
        install(new StatisticsCommandsModule());
        install(new TargetsCommandsModule());
        install(new TeamCommandsModule());
        install(new TimeCommandsModule());
        install(new TriviaCommandsModule());
        install(new UserCommandsModule());
        install(new UtopiaCommandsModule());
    }
}
