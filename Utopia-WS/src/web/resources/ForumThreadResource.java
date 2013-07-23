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

package web.resources;

import api.database.Transactional;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.ForumSectionDAO;
import database.daos.ForumThreadDAO;
import database.models.ForumPost;
import database.models.ForumSection;
import database.models.ForumThread;
import web.documentation.Documentation;
import web.models.RS_ForumPost;
import web.models.RS_ForumThread;
import web.tools.WebContext;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkNotNull;

@ValidationEnabled
@Path("forum/threads")
public class ForumThreadResource {
    private final ForumThreadDAO threadDAO;
    private final Provider<ForumSectionDAO> sectionDAOProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public ForumThreadResource(final ForumThreadDAO threadDAO,
                               final Provider<ForumSectionDAO> sectionDAOProvider,
                               final Provider<Validator> validatorProvider) {
        this.threadDAO = threadDAO;
        this.sectionDAOProvider = sectionDAOProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds a new forum thread and returns the saved object")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ForumThread addThread(@Documentation(value = "The new thread to add", itemName = "newThread")
                                    @Valid final RS_ForumThread newThread,
                                    @Context final WebContext webContext) {
        BotUser user = webContext.getBotUser();

        ForumSection section = sectionDAOProvider.get().getForumSection(newThread.getSection().getId());
        if (!section.getMinimumAccessLevel().allows(user)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        ForumThread thread = new ForumThread(section, newThread.getName(), user.getMainNick());
        thread = threadDAO.save(thread);
        return RS_ForumThread.fromForumThread(thread, true);
    }

    @Documentation("Returns the thread with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ForumThread getThread(@PathParam("id") final long id,
                                    @Context final WebContext webContext) {
        BotUser botUser = webContext.getBotUser();

        ForumThread thread = threadDAO.getForumThread(id);
        if (thread == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!thread.getSection().getMinimumAccessLevel().allows(botUser)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        return RS_ForumThread.fromForumThread(thread, true);
    }

    @Documentation("Returns all the posts in the specified thread")
    @Path("{id : \\d+}/posts")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_ForumPost>> getPosts(@PathParam("id") final long id,
                                                  @Context final WebContext webContext) {
        List<RS_ForumPost> posts = new ArrayList<>();

        BotUser botUser = webContext.getBotUser();

        ForumThread thread = threadDAO.getForumThread(id);
        if (thread == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!thread.getSection().getMinimumAccessLevel().allows(botUser)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        for (ForumPost post : thread.getPosts()) {
            posts.add(RS_ForumPost.fromForumPost(post));
        }

        return JResponse.ok(posts).build();
    }

    @Documentation("Updates the specified thread and returns the updated object")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ForumThread updateThread(@PathParam("id") final long id,
                                       @Documentation(value = "The updated thread", itemName = "updatedThread")
                                       final RS_ForumThread updatedThread,
                                       @Context final WebContext webContext) {
        validate(updatedThread).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        BotUser botUser = webContext.getBotUser();

        ForumThread thread = threadDAO.getForumThread(id);
        if (thread == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!botUser.isAdmin()) throw new WebApplicationException(Response.Status.FORBIDDEN);

        if (!thread.getSection().getId().equals(updatedThread.getSection().getId())) {
            ForumSection newSection = sectionDAOProvider.get().getForumSection(updatedThread.getSection().getId());
            checkNotNull(newSection, "No such section");
            thread.setSection(newSection);
        }

        RS_ForumThread.toForumThread(thread, updatedThread);

        return RS_ForumThread.fromForumThread(thread, true);
    }

    @Documentation("Deletes the specified thread and all of its posts")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deletePost(@PathParam("id") final long id,
                           @Context final WebContext webContext) {
        BotUser botUser = webContext.getBotUser();

        ForumThread thread = threadDAO.getForumThread(id);
        if (thread == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!botUser.isAdmin()) throw new WebApplicationException(Response.Status.FORBIDDEN);

        threadDAO.delete(thread);
    }
}
