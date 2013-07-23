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

package commands.utopia.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.GameMechanicCalculator;

import javax.inject.Inject;
import java.util.Collection;

public class EffectsCommandHandler implements CommandHandler {
    private final GameMechanicCalculator gameMechanicCalculator;

    @Inject
    public EffectsCommandHandler(final GameMechanicCalculator gameMechanicCalculator) {
        this.gameMechanicCalculator = gameMechanicCalculator;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            if (params.containsKey("building")) {
                return CommandResponse.resultResponse("buildingEffects", gameMechanicCalculator
                        .getBuildingEffects(params.getParameter("building"), params.getParameter("percent"), params.getParameter("amount"),
                                params.getParameter("be")));
            } else if (params.containsKey("scienceType")) {
                if (params.containsKey("percent")) return CommandResponse.resultResponse("bpa", gameMechanicCalculator
                        .getBpaFromPercent(params.getParameter("scienceType"), params.getDoubleParameter("percent"),
                                params.getParameter("bonus")));
                else return CommandResponse.resultResponse("scienceEffects", gameMechanicCalculator
                        .getPercentFromBpa(params.getParameter("scienceType"), params.getIntParameter("bpa"),
                                params.getParameter("bonus")));
            } else if (params.containsKey("spell")) return CommandResponse
                    .resultResponse("spellEffects", gameMechanicCalculator.getSpellEffectsDescriptions(params.getParameter("spell")));
            else if (params.containsKey("op")) return CommandResponse
                    .resultResponse("opEffects", gameMechanicCalculator.getOpEffectsDescriptions(params.getParameter("op")));

            throw new IllegalStateException("This code path should not be reachable");
        } catch (Exception e) {
            throw new CommandHandlingException(e);
        }
    }
}
