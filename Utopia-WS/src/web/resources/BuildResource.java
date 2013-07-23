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
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.BuildDAO;
import database.daos.BuildingDAO;
import database.daos.PersonalityDAO;
import database.daos.RaceDAO;
import database.models.*;
import events.BuildAddedEvent;
import tools.BindingsManager;
import web.documentation.Documentation;
import web.models.RS_Build;
import web.models.RS_BuildEntry;
import web.tools.AfterCommitEventPoster;
import web.tools.BindingsParser;
import web.tools.WebContext;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static api.tools.collections.CollectionUtil.isNotEmpty;
import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkNotNull;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("builds")
public class BuildResource {
    private final BuildDAO buildDAO;
    private final Provider<BuildingDAO> buildingDAOProvider;
    private final Provider<BindingsParser> bindingsParserProvider;
    private final Provider<RaceDAO> raceDAOProvider;
    private final Provider<PersonalityDAO> personalityDAOProvider;
    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<BindingsManager> bindingsManagerProvider;
    private final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public BuildResource(final BuildDAO buildDAO,
                         final Provider<BuildingDAO> buildingDAOProvider,
                         final Provider<BindingsParser> bindingsParserProvider,
                         final Provider<RaceDAO> raceDAOProvider,
                         final Provider<PersonalityDAO> personalityDAOProvider,
                         final Provider<BotUserDAO> userDAOProvider,
                         final Provider<BindingsManager> bindingsManagerProvider,
                         final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider,
                         final Provider<Validator> validatorProvider) {
        this.buildDAO = buildDAO;
        this.buildingDAOProvider = buildingDAOProvider;
        this.bindingsParserProvider = bindingsParserProvider;
        this.raceDAOProvider = raceDAOProvider;
        this.personalityDAOProvider = personalityDAOProvider;
        this.userDAOProvider = userDAOProvider;
        this.bindingsManagerProvider = bindingsManagerProvider;
        this.afterCommitEventPosterProvider = afterCommitEventPosterProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds the specified build, fires off a BuildAddedEvent and then returns the saved object. Admin only request")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Build addBuild(@Documentation(value = "The build to add", itemName = "newBuild")
                             @Valid final RS_Build newBuild,
                             @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Bindings bindings = bindingsParserProvider.get().parse(newBuild.getBindings());
        Build build = new Build(bindings, newBuild.getType(), webContext.getName());

        editBuildEntries(build, newBuild, buildingDAOProvider.get());
        RS_Build.toBuild(build, newBuild);
        build = buildDAO.save(build);
        afterCommitEventPosterProvider.get().addEventToPost(new BuildAddedEvent(build.getId(), null));

        return RS_Build.fromBuild(build);
    }

    @Documentation("Returns the build with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Build getBuild(@PathParam("id") final long id) {
        Build build = buildDAO.getBuild(id);

        if (build == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Build.fromBuild(build);
    }

    @Documentation("Returns builds according to the query parameters, or all builds (if no parameters are specified).")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Build>> getBuilds(@Documentation("The id's of the builds to return. May not contain nulls. Cannot be combined with other parameters")
                                               @QueryParam("buildIds")
                                               final List<Long> buildIds,
                                               @Documentation("The id of the race to get builds for. May be combined with personalityId")
                                               @QueryParam("raceId")
                                               final Long raceId,
                                               @Documentation("The id of the personality to get builds for. May be combined with raceId")
                                               @QueryParam("personalityId")
                                               final Long personalityId,
                                               @Documentation("The type of the builds to get. Cannot be combined with other parameters")
                                               @QueryParam("type")
                                               final String type,
                                               @Documentation("The user to get builds for. Cannot be combined with other parameters")
                                               @QueryParam("userId")
                                               final Long userId) {
        List<RS_Build> builds = new ArrayList<>();
        if (isNotEmpty(buildIds)) {
            Long[] buildIdArray = buildIds.toArray(new Long[buildIds.size()]);
            for (Build build : buildDAO.getBuilds(buildIdArray)) {
                builds.add(RS_Build.fromBuild(build));
            }
        } else if (raceId != null || personalityId != null) {
            Race race = raceId == null ? null : raceDAOProvider.get().getRace(raceId);
            if (raceId != null && race == null) throw new IllegalArgumentException("No such race");
            Personality personality = personalityId == null ? null : personalityDAOProvider.get().getPersonality(personalityId);
            if (personalityId != null && personality == null) throw new IllegalArgumentException("No such personality");

            for (Build build : buildDAO.getBuilds(race, personality, type)) {
                builds.add(RS_Build.fromBuild(build));
            }
        } else if (userId != null) {
            BotUser user = userDAOProvider.get().getUser(userId);
            checkNotNull(user, "No such user");
            for (Build build : buildDAO.getBuildsForUser(user, bindingsManagerProvider.get())) {
                builds.add(RS_Build.fromBuild(build));
            }
        } else if (type != null) {
            for (Build build : buildDAO.getBuilds(type)) {
                builds.add(RS_Build.fromBuild(build));
            }
        } else {
            for (Build build : buildDAO.getAllBuilds()) {
                builds.add(RS_Build.fromBuild(build));
            }
        }
        return JResponse.ok(builds).build();
    }

    @Documentation("Updates the specified build and returns the updated object. Admin only request")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Build updateBuild(@PathParam("id") final long id,
                                @Documentation(value = "The updated build", itemName = "updatedBuild")
                                final RS_Build updatedBuild,
                                @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Build build = buildDAO.getBuild(id);
        if (build == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedBuild).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        editBuildEntries(build, updatedBuild, buildingDAOProvider.get());

        RS_Build.toBuild(build, updatedBuild);

        return RS_Build.fromBuild(build);
    }

    private static void editBuildEntries(final Build build, final RS_Build updatedBuild, final BuildingDAO buildingDAO) {
        Map<Building, BuildEntry> existingEntries = new HashMap<>();
        for (BuildEntry entry : build.getBuildings()) {
            existingEntries.put(entry.getBuilding(), entry);
        }

        Map<Long, Building> allBuildings = new HashMap<>();
        for (Building building : buildingDAO.getAllBuildings()) {
            allBuildings.put(building.getId(), building);
        }

        Set<BuildEntry> unseenInUpdate = new HashSet<>(existingEntries.values());
        for (RS_BuildEntry rs_buildEntry : updatedBuild.getBuildings()) {
            Long buildingId = rs_buildEntry.getBuilding().getId();
            Building building = allBuildings.get(buildingId);
            if (existingEntries.containsKey(building)) {
                BuildEntry existingEntry = existingEntries.get(building);
                existingEntry.setPercentage(rs_buildEntry.getPercentage().doubleValue());
                unseenInUpdate.remove(existingEntry);
            } else {
                build.getBuildings().add(new BuildEntry(build, building, rs_buildEntry.getPercentage().doubleValue()));
            }
        }

        build.getBuildings().removeAll(unseenInUpdate);
    }

    @Documentation("Deletes the specified build. Admin only request")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteBuild(@PathParam("id") final long id,
                            @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Build build = buildDAO.getBuild(id);
        if (build == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        buildDAO.delete(build);
    }
}
