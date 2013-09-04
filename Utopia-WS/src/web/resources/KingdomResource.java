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
import api.events.DelayedEventPoster;
import api.tools.validation.ValidationEnabled;
import com.google.common.base.Supplier;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.DragonDAO;
import database.daos.IntelDAO;
import database.daos.KingdomDAO;
import database.models.Dragon;
import database.models.Kingdom;
import events.NapAddedEvent;
import intel.Intel;
import intel.IntelParser;
import intel.IntelParserManager;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ExistsInDB;
import web.documentation.Documentation;
import web.models.RS_Dragon;
import web.models.RS_Kingdom;
import web.models.RS_Nap;
import web.tools.AfterCommitEventPoster;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("kingdoms")
public class KingdomResource {
    private final KingdomDAO kingdomDAO;
    private final Provider<IntelDAO> intelDAOProvider;
    private final Provider<IntelParserManager> intelParserManagerProvider;
    private final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider;
    private final Provider<DragonDAO> dragonDAOProvider;
    private final Provider<DelayedEventPoster> delayedEventPosterProvider;

    @Inject
    public KingdomResource(final KingdomDAO kingdomDAO,
                           final Provider<IntelDAO> intelDAOProvider,
                           final Provider<IntelParserManager> intelParserManagerProvider,
                           final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider,
                           final Provider<DragonDAO> dragonDAOProvider,
                           final Provider<DelayedEventPoster> delayedEventPosterProvider) {
        this.kingdomDAO = kingdomDAO;
        this.intelDAOProvider = intelDAOProvider;
        this.intelParserManagerProvider = intelParserManagerProvider;
        this.afterCommitEventPosterProvider = afterCommitEventPosterProvider;
        this.dragonDAOProvider = dragonDAOProvider;
        this.delayedEventPosterProvider = delayedEventPosterProvider;
    }

    @Documentation("Parses the incoming text and returns the saved Kingdom")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    @Transactional
    public RS_Kingdom addKingdom(@Documentation(value = "The formatted or unformatted kingdom page", itemName = "newKingdom")
                                 @NotEmpty(message = "The intel may not be null or empty")
                                 final String newKingdom,
                                 @Context final WebContext webContext) throws Exception {
        Map<String, IntelParser<?>> parsers = intelParserManagerProvider.get().getParsers(newKingdom);
        if (parsers.isEmpty()) throw new IllegalArgumentException("Data is not parsable");

        IntelParser<?> intelParser = parsers.get(0);
        if (!intelParser.getIntelTypeHandled().equals(Kingdom.class.getSimpleName()))
            throw new IllegalArgumentException("Data is not recognized as a Kingdom");

        Intel parsedKingdom = intelParser.parse(webContext.getName(), newKingdom);
        intelDAOProvider.get().saveIntel(parsedKingdom, webContext.getBotUser().getId(), delayedEventPosterProvider.get());

        return RS_Kingdom.fromKingdom((Kingdom) parsedKingdom, true);
    }

    @Documentation("Returns the Kingdom with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Kingdom getKingdom(@PathParam("id") final long id) {
        Kingdom kingdom = kingdomDAO.getKingdom(id);

        if (kingdom == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Kingdom.fromKingdom(kingdom, true);
    }

    @Documentation("Returns all kingdoms, or optionally just the one with the specified kingdom location")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Kingdom>> getKingdoms(@Documentation("Optionally limits the result to only the kingdom with this location")
                                                   @QueryParam("location")
                                                   final String location) {
        List<RS_Kingdom> kingdoms = new ArrayList<>();

        if (location == null) {
            for (Kingdom kingdom : kingdomDAO.getAllKingdoms()) {
                kingdoms.add(RS_Kingdom.fromKingdom(kingdom, false));
            }
        } else {
            Kingdom kingdom = kingdomDAO.getKingdom(location);
            checkNotNull(kingdom, "No such kingdom");
            kingdoms.add(RS_Kingdom.fromKingdom(kingdom, true));
        }

        return JResponse.ok(kingdoms).build();
    }

    @Documentation("Deletes the kingdom with the specified id, including all the provinces, intel, armies etc.. Admin only request")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteKingdom(@PathParam("id") final long id,
                              @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Kingdom kingdom = kingdomDAO.getKingdom(id);
        if (kingdom == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        kingdomDAO.delete(kingdom);
    }

    @Documentation("Sets the kingdom comment for the specified kingdom. Admin only request")
    @Path("{id : \\d+}/comment")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Kingdom setKingdomComment(@PathParam("id") final long id,
                                        @Documentation(value = "The new comment", itemName = "newComment")
                                        final String newComment,
                                        @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Kingdom kingdom = kingdomDAO.getKingdom(id);
        if (kingdom == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        kingdom.setKdComment(newComment);
        kingdom.setSavedBy(webContext.getName());

        return RS_Kingdom.fromKingdom(kingdom, true);
    }

    @Documentation("Deletes the kingdom comment for the specified kingdom. Admin only request")
    @Path("{id : \\d+}/comment")
    @DELETE
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Kingdom deleteKingdomComment(@PathParam("id") final long id,
                                           @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Kingdom kingdom = kingdomDAO.getKingdom(id);
        if (kingdom == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        kingdom.setKdComment(null);
        kingdom.setSavedBy(webContext.getName());

        return RS_Kingdom.fromKingdom(kingdom, true);
    }

    @Documentation("Sets the NAP info for the specified kingdom, fires off a NapAddedEvent and returns the saved kingdom. Admin only request")
    @Path("{id : \\d+}/nap")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Kingdom setNap(@PathParam("id") final long id,
                             @Documentation(value = "The NAP to add", itemName = "nap")
                             @Valid final RS_Nap nap,
                             @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        final Kingdom kingdom = kingdomDAO.getKingdom(id);
        if (kingdom == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        kingdom.setNapAdded(new Date());
        kingdom.setNapDescription(nap.getDescription());
        kingdom.setNapEndDate(nap.getEnds());

        afterCommitEventPosterProvider.get().addEventToPost(new Supplier<Object>() {
            @Override
            public Object get() {
                return new NapAddedEvent(kingdom.getId(), null);
            }
        });

        return RS_Kingdom.fromKingdom(kingdom, true);
    }

    @Documentation("Removes the NAP info for the specified kingdom. Admin only request")
    @Path("{id : \\d+}/nap")
    @DELETE
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Kingdom deleteNap(@PathParam("id") final long id,
                                @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Kingdom kingdom = kingdomDAO.getKingdom(id);
        if (kingdom == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        kingdom.setNapAdded(null);
        kingdom.setNapDescription(null);
        kingdom.setNapEndDate(null);
        return RS_Kingdom.fromKingdom(kingdom, true);
    }

    @Documentation("Adds a dragon to the specified kingdom and returns the updated kingdom")
    @Path("{id : \\d+}/dragon")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Kingdom setDragon(@PathParam("id") final long id,
                                @Documentation(value = "The dragon to set", itemName = "newDragon")
                                @NotNull(message = "The dragon must not be null")
                                @ExistsInDB(entity = Dragon.class, message = "No such dragon")
                                final RS_Dragon newDragon) {
        Kingdom kingdom = kingdomDAO.getKingdom(id);
        if (kingdom == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        Dragon dragon = dragonDAOProvider.get().getDragon(newDragon.getId());

        kingdom.setDragon(dragon);

        return RS_Kingdom.fromKingdom(kingdom, true);
    }

    @Documentation("Deletes the dragon from the specified kingdom")
    @Path("{id : \\d+}/dragon")
    @DELETE
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Kingdom deleteDragon(@PathParam("id") final long id) {
        Kingdom kingdom = kingdomDAO.getKingdom(id);
        if (kingdom == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        kingdom.setDragon(null);
        return RS_Kingdom.fromKingdom(kingdom, true);
    }
}
