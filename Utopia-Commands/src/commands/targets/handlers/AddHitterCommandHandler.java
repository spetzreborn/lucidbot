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

package commands.targets.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.TargetDAO;
import database.models.Target;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;

public class AddHitterCommandHandler implements CommandHandler {
    private final TargetDAO targetDAO;
    private final BotUserDAO botUserDAO;

    @Inject
    public AddHitterCommandHandler(final TargetDAO targetDAO, final BotUserDAO botUserDAO) {
        this.targetDAO = targetDAO;
        this.botUserDAO = botUserDAO;
    }

    @Override
    public CommandResponse handleCommand(final IRCContext context, final Params params, final Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            long targetId = params.getLongParameter("targetId");
            Target target = targetDAO.getTarget(targetId);
            if (target == null ||
                    target.getType() != Target.TargetType.GENERATED_TARGET && target.getType() != Target.TargetType.MANUAL_TARGET)
                return CommandResponse.errorResponse("No such target");

            BotUser user = botUserDAO.getUser(params.getParameter("user"));
            if (user == null) return CommandResponse.errorResponse("No such user");

            if (target.containsUserAsHitter(user)) return CommandResponse.errorResponse("That user is already a hitter");

            int currentHitters = target.getAmountOfHitters();
            if (params.containsKey("position")) {
                int position = params.getIntParameter("position");
                if (position > currentHitters) target.insertHitter(user, currentHitters + 1);
                else if (position < 1) target.insertHitter(user, 1);
                else target.insertHitter(user, position);
            } else target.insertHitter(user, currentHitters + 1);

            return CommandResponse.resultResponse("target", target, "user", user);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
