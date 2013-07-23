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
import api.database.models.AccessLevel;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.ForumSectionDAO;
import database.models.ForumSection;
import database.models.ForumThread;
import web.documentation.Documentation;
import web.models.RS_ForumSection;
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
import static com.google.common.base.Preconditions.checkArgument;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("forum/sections")
public class ForumSectionResource {
    private final ForumSectionDAO sectionDAO;
    private final Provider<Validator> validatorProvider;

    @Inject
    public ForumSectionResource(final ForumSectionDAO sectionDAO,
                                final Provider<Validator> validatorProvider) {
        this.sectionDAO = sectionDAO;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds a new section to the forum and returns the saved object. Admin only request")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ForumSection addSection(@Documentation(value = "The new section to add", itemName = "newSection")
                                      @Valid final RS_ForumSection newSection,
                                      @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        ForumSection existing = sectionDAO.getSectionByName(newSection.getName());
        checkArgument(existing == null, "A section with that name already exists");

        ForumSection section = new ForumSection(newSection.getName(), AccessLevel.fromName(newSection.getMinimumAccessLevel()));
        section = sectionDAO.save(section);
        return RS_ForumSection.fromForumSection(section, true);
    }

    @Documentation("Returns the section with the specified id, provided the user has access to it")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ForumSection getSection(@PathParam("id") final long id,
                                      @Context final WebContext webContext) {
        BotUser botUser = webContext.getBotUser();

        ForumSection section = sectionDAO.getForumSection(id);
        if (section == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!section.getMinimumAccessLevel().allows(botUser)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        return RS_ForumSection.fromForumSection(section, true);
    }

    @Documentation("Returns all the sections the current user is allowed to see")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_ForumSection>> getSections(@Context final WebContext webContext) {
        List<RS_ForumSection> sections = new ArrayList<>();

        BotUser botUser = webContext.getBotUser();

        for (ForumSection section : sectionDAO.getAllSections()) {
            if (section.getMinimumAccessLevel().allows(botUser))
                sections.add(RS_ForumSection.fromForumSection(section, false));
        }

        return JResponse.ok(sections).build();
    }

    @Documentation("Returns all the threads in the specified section, provided the user is allowed to see the section")
    @Path("{id : \\d+}/threads")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_ForumThread>> getThreads(@PathParam("id") final long id,
                                                      @Context final WebContext webContext) {
        List<RS_ForumThread> threads = new ArrayList<>();

        BotUser botUser = webContext.getBotUser();

        ForumSection section = sectionDAO.getForumSection(id);
        if (section == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!section.getMinimumAccessLevel().allows(botUser)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        for (ForumThread thread : section.getThreads()) {
            threads.add(RS_ForumThread.fromForumThread(thread, false));
        }

        return JResponse.ok(threads).build();
    }

    @Documentation("Updates the specified section and returns the updated object. Admin only request")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ForumSection updateSection(@PathParam("id") final long id,
                                         @Documentation(value = "The updated section", itemName = "updatedSection")
                                         final RS_ForumSection updatedSection,
                                         @Context final WebContext webContext) {
        validate(updatedSection).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        ForumSection section = sectionDAO.getForumSection(id);
        if (section == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        RS_ForumSection.toForumSection(section, updatedSection);

        return RS_ForumSection.fromForumSection(section, true);
    }

    @Documentation("Deletes the specified section and all of its contents. Admin only request")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteSection(@PathParam("id") final long id,
                              @Context final WebContext webContext) {
        ForumSection section = sectionDAO.getForumSection(id);
        if (section == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        sectionDAO.delete(section);
    }
}
