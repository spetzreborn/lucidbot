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

package commands.forum.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.models.AccessLevel;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.ForumPostDAO;
import database.daos.ForumThreadDAO;
import database.models.ForumPost;
import database.models.ForumThread;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;

public class PostCommandHandler implements CommandHandler {
    private final ForumThreadDAO threadDAO;
    private final ForumPostDAO postDAO;

    @Inject
    public PostCommandHandler(final ForumThreadDAO threadDAO, final ForumPostDAO postDAO) {
        this.threadDAO = threadDAO;
        this.postDAO = postDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            ForumThread thread = threadDAO.getForumThread(params.getLongParameter("id"));
            if (thread == null) return CommandResponse.errorResponse("No thread with that ID could be found");
            if (!thread.getSection().getMinimumAccessLevel().allows(context.getUser(), context.getChannel()))
                return CommandResponse.errorResponse("You don't have access to that part of the forum");
            if (thread.isLocked() && !AccessLevel.ADMIN.allows(context.getUser(), context.getChannel()))
                return CommandResponse.errorResponse("That thread is locked");
            ForumPost post = new ForumPost(context.getBotUser(), thread, params.getParameter("post"));
            thread.getPosts().add(post);
            postDAO.save(post);
            return CommandResponse.resultResponse("post", post);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
