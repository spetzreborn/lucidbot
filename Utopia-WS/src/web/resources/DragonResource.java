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

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.DragonDAO;
import database.daos.DragonProjectDAO;
import database.models.Dragon;
import database.models.DragonAction;
import database.models.DragonProject;
import database.models.DragonProjectType;
import web.documentation.Documentation;
import web.models.RS_Dragon;
import web.models.RS_DragonAction;
import web.models.RS_DragonProject;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static api.tools.text.StringUtil.prettifyEnumName;
import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkArgument;

@ValidationEnabled
@Path("dragons")
public class DragonResource {
    private final Provider<DragonDAO> dragonDAOProvider;
    private final Provider<BotUserDAO> userDAOProvider;
    private final DragonProjectDAO dragonProjectDAO;
    private final Provider<Validator> validatorProvider;

    @Inject
    public DragonResource(final Provider<DragonDAO> dragonDAOProvider, final Provider<BotUserDAO> userDAOProvider,
                          final DragonProjectDAO dragonProjectDAO, final Provider<Validator> validatorProvider) {
        this.dragonDAOProvider = dragonDAOProvider;
        this.userDAOProvider = userDAOProvider;
        this.dragonProjectDAO = dragonProjectDAO;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Returns the dragon with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Dragon getDragon(@PathParam("id") final long id) {
        Dragon dragon = dragonDAOProvider.get().getDragon(id);

        if (dragon == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Dragon.fromDragon(dragon, true);
    }

    @Documentation("Returns all dragons")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Dragon>> getDragons() {
        List<RS_Dragon> dragons = new ArrayList<>();

        for (Dragon dragon : dragonDAOProvider.get().getAllDragons()) {
            dragons.add(RS_Dragon.fromDragon(dragon, true));
        }

        return JResponse.ok(dragons).build();
    }

    @Documentation("Adds a new dragon project and returns the saved object")
    @Path("projects")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_DragonProject addDragonProject(@Documentation(value = "The dragon project to add", itemName = "newProject")
                                             @Valid final RS_DragonProject newProject) {
        DragonProject dragonProject = new DragonProject(DragonProjectType.fromName(newProject.getType()), newProject.getOriginalStatus());
        dragonProjectDAO.save(dragonProject);
        return RS_DragonProject.fromDragonProject(dragonProject, true);
    }

    @Documentation("Returns the dragon project with the specified id")
    @Path("projects/{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_DragonProject getDragonProject(@PathParam("id") final long id) {
        DragonProject project = dragonProjectDAO.getProject(id);
        if (project == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        return RS_DragonProject.fromDragonProject(project, true);
    }

    @Documentation("Returns all existing dragon projects, regardless if they're active or not")
    @Path("projects")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_DragonProject>> getDragonProjects() {
        List<RS_DragonProject> projects = new ArrayList<>();
        for (DragonProject project : dragonProjectDAO.getAllProjects()) {
            projects.add(RS_DragonProject.fromDragonProject(project, false));
        }
        return JResponse.ok(projects).build();
    }

    @Documentation("Updates the specified dragon project and returns the updated object")
    @Path("projects/{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_DragonProject updateDragonProject(@PathParam("id") final long id,
                                                @Documentation(value = "The updated dragon project", itemName = "updatedProject")
                                                final RS_DragonProject updatedProject) {
        DragonProject project = dragonProjectDAO.getProject(id);
        if (project == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedProject).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        RS_DragonProject.toDragonProject(project, updatedProject);

        return RS_DragonProject.fromDragonProject(project, true);
    }

    @Documentation("Deletes the specified dragon project")
    @Path("projects/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteDragonProject(@PathParam("id") final long id) {
        DragonProject project = dragonProjectDAO.getProject(id);
        if (project == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        dragonProjectDAO.delete(project);
    }

    @Documentation("Registers a dragon project action (meaning someone doing killing or donating)")
    @Path("projects/actions")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public void registerDragonAction(@Documentation(value = "The action to register", itemName = "newAction")
                                     final RS_DragonAction newAction) {
        validate(newAction).using(validatorProvider.get()).throwOnFailedValidation();

        DragonProject project = dragonProjectDAO.getProject(newAction.getDragonProject().getId());
        BotUser user = userDAOProvider.get().getUser(newAction.getUser().getId());

        registerDragonAction(project, user, newAction.getContribution());
    }

    private void registerDragonAction(final DragonProject project, final BotUser user, final int contribution) {
        int actualContribution = Math.min(contribution, project.getStatus());
        checkArgument(actualContribution > 0, "Impossible contribution");
        user.incrementStat("Dragon " + prettifyEnumName(project.getType()), actualContribution);

        Set<DragonAction> actions = project.getActions();
        Date now = new Date();
        for (DragonAction action : actions) {
            if (action.getUser().equals(user)) {
                action.setContribution(action.getContribution() + actualContribution);
                project.setStatus(Math.max(0, project.getStatus() - contribution));
                project.setUpdated(now);
                action.setUpdated(now);
                return;
            }
        }

        actions.add(new DragonAction(user, actualContribution, now, project));
        project.setStatus(Math.max(0, project.getStatus() - contribution));
        project.setUpdated(now);
    }
}
