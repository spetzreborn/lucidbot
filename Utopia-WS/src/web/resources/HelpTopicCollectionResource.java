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
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.HelpTopicCollectionDAO;
import database.models.HelpTopicCollection;
import web.documentation.Documentation;
import web.models.RS_HelpTopicCollection;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkArgument;

@ValidationEnabled
@Path("help/collections")
public class HelpTopicCollectionResource {
    private final HelpTopicCollectionDAO collectionDAO;
    private final Provider<Validator> validatorProvider;

    @Inject
    public HelpTopicCollectionResource(final HelpTopicCollectionDAO collectionDAO,
                                       final Provider<Validator> validatorProvider) {
        this.collectionDAO = collectionDAO;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds the specified help topic collection and returns the saved object")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_HelpTopicCollection addCollection(@Documentation(value = "The new collection to add", itemName = "newCollection")
                                                @Valid final RS_HelpTopicCollection newCollection) {
        HelpTopicCollection existing = collectionDAO.getByName(newCollection.getName());
        checkArgument(existing == null, "A collection with that name already exists");

        HelpTopicCollection parent = null;
        if (newCollection.getParent() != null) {
            parent = collectionDAO.getHelpTopicCollection(newCollection.getParent().getId());
        }

        HelpTopicCollection collection = new HelpTopicCollection(newCollection.getName(), parent);
        collectionDAO.save(collection);
        return RS_HelpTopicCollection.fromHelpTopicCollection(collection, true);
    }

    @Documentation("Returns the collection with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_HelpTopicCollection getCollection(@PathParam("id") final long id) {
        HelpTopicCollection collection = collectionDAO.getHelpTopicCollection(id);

        if (collection == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_HelpTopicCollection.fromHelpTopicCollection(collection, true);
    }

    @Documentation("Returns all the help topic collections, or optionally just the top level ones")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_HelpTopicCollection>> getCollections(@Documentation("Whether to only include top level collections in the response")
                                                                  @QueryParam("topLevelOnly")
                                                                  final boolean topLevelOnly) {
        List<RS_HelpTopicCollection> collections = new ArrayList<>();

        Collection<HelpTopicCollection> topicCollections =
                topLevelOnly ? collectionDAO.getTopLevelCollections() : collectionDAO.getAllCollections();
        for (HelpTopicCollection collection : topicCollections) {
            collections.add(RS_HelpTopicCollection.fromHelpTopicCollection(collection, true));
        }

        return JResponse.ok(collections).build();
    }

    @Documentation("Updates the specified collection and returns the updated object")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_HelpTopicCollection updateCollection(@PathParam("id") final long id,
                                                   @Documentation(value = "The updated collection", itemName = "updatedCollection")
                                                   final RS_HelpTopicCollection updatedCollection) {
        HelpTopicCollection collection = collectionDAO.getHelpTopicCollection(id);
        if (collection == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedCollection).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        HelpTopicCollection existing = collectionDAO.getByName(collection.getName());
        checkArgument(existing == null, "There's already a collection with that name");

        HelpTopicCollection parent = null;
        if (updatedCollection.getParent() != null) {
            parent = collectionDAO.getHelpTopicCollection(updatedCollection.getParent().getId());
        }

        collection.setParent(parent);
        collection.setName(updatedCollection.getName());

        return RS_HelpTopicCollection.fromHelpTopicCollection(collection, true);
    }

    @Documentation("Deletes the specified collection and all its content (both child collections and all the topics)")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteCollection(@PathParam("id") final long id) {
        HelpTopicCollection collection = collectionDAO.getHelpTopicCollection(id);
        if (collection == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        collectionDAO.delete(collection);
    }
}

