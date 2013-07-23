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
import database.daos.ForumPostDAO;
import database.daos.ForumThreadDAO;
import database.models.ForumPost;
import database.models.ForumThread;
import web.documentation.Documentation;
import web.models.RS_ForumPost;
import web.tools.WebContext;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static api.tools.validation.ValidationUtil.validate;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("forum/posts")
public class ForumPostResource {
    private final ForumPostDAO forumPostDAO;
    private final Provider<ForumThreadDAO> threadDAOProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public ForumPostResource(final ForumPostDAO forumPostDAO,
                             final Provider<ForumThreadDAO> threadDAOProvider,
                             final Provider<Validator> validatorProvider) {
        this.forumPostDAO = forumPostDAO;
        this.threadDAOProvider = threadDAOProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Posts the specified post into the forum and returns the saved object. Only works if the user has access to the section the thread " +
            "is in and the thread isn't locked")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ForumPost addPost(@Documentation(value = "The post to add", itemName = "newPost")
                                @Valid final RS_ForumPost newPost,
                                @Context final WebContext webContext) {
        BotUser user = webContext.getBotUser();

        ForumThread thread = threadDAOProvider.get().getForumThread(newPost.getThread().getId());
        if (!thread.getSection().getMinimumAccessLevel().allows(user)) throw new WebApplicationException(Response.Status.FORBIDDEN);
        else if (!webContext.isInRole(ADMIN_ROLE) && thread.isLocked()) throw new WebApplicationException(Response.Status.FORBIDDEN);

        ForumPost post = new ForumPost(user, thread, newPost.getPost());
        post = forumPostDAO.save(post);
        return RS_ForumPost.fromForumPost(post);
    }

    @Documentation("Returns the post with the specified id, provided the user has access to it")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ForumPost getPost(@PathParam("id") final long id,
                                @Context final WebContext webContext) {
        BotUser botUser = webContext.getBotUser();

        ForumPost post = forumPostDAO.getForumPost(id);
        if (post == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!post.getThread().getSection().getMinimumAccessLevel().allows(botUser))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        return RS_ForumPost.fromForumPost(post);
    }

    @Documentation("Updates the specified post and returns the updated object. Admins may edit any post, but regular users can only edit their own")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ForumPost updatePost(@PathParam("id") final long id,
                                   @Documentation(value = "The updated post", itemName = "updatedPost")
                                   final RS_ForumPost updatedPost,
                                   @Context final WebContext webContext) {
        validate(updatedPost).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        BotUser botUser = webContext.getBotUser();

        ForumPost post = forumPostDAO.getForumPost(id);
        if (post == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!userHasAccess(botUser, post)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        RS_ForumPost.toForumPost(post, updatedPost);

        return RS_ForumPost.fromForumPost(post);
    }

    @Documentation("Deletes the specified post. Admins may remove any post, but regular users can only delete their own")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deletePost(@PathParam("id") final long id,
                           @Context final WebContext webContext) {
        BotUser botUser = webContext.getBotUser();

        ForumPost post = forumPostDAO.getForumPost(id);
        if (post == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!userHasAccess(botUser, post)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        forumPostDAO.delete(post);
    }

    private static boolean userHasAccess(final BotUser botUser, final ForumPost post) {
        return post.getThread().getSection().getMinimumAccessLevel().allows(botUser) &&
                (post.getUser().equals(botUser) || botUser.isAdmin());
    }
}
