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

package commands.team.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.DragonProjectDAO;
import database.models.DragonProject;
import database.models.DragonProjectType;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;

public class RemoveDragonCommandHandler implements CommandHandler {
    private final DragonProjectDAO dragonProjectDAO;

    @Inject
    public RemoveDragonCommandHandler(final DragonProjectDAO dragonProjectDAO) {
        this.dragonProjectDAO = dragonProjectDAO;
    }

    @Override
    public CommandResponse handleCommand(final IRCContext context, final Params params, final Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            DragonProject existing;
            if (params.containsKey("id")) {
                existing = dragonProjectDAO.getProject(params.getLongParameter("id"));
            } else {
                DragonProjectType type = params.containsKey("sending") ? DragonProjectType.SENDING : DragonProjectType.KILLING;
                existing = dragonProjectDAO.getProjectOfType(type);
            }
            if (existing == null) return CommandResponse.errorResponse("No such dragon project");
            dragonProjectDAO.delete(existing);
            return CommandResponse.resultResponse("project", existing);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
