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

import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.HelpTopicCollectionDAO;
import database.daos.HelpTopicDAO;
import database.models.HelpTopic;
import database.models.HelpTopicCollection;
import web.documentation.Documentation;
import web.models.RS_HelpTopic;
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
@Path("help/topics")
public class HelpTopicResource {
    private final HelpTopicDAO helpTopicDAO;
    private final Provider<HelpTopicCollectionDAO> collectionDAOProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public HelpTopicResource(final HelpTopicDAO helpTopicDAO,
                             final Provider<HelpTopicCollectionDAO> collectionDAOProvider,
                             final Provider<Validator> validatorProvider) {
        this.helpTopicDAO = helpTopicDAO;
        this.collectionDAOProvider = collectionDAOProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds the specified help topic and returns the saved object")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_HelpTopic addTopic(@Documentation(value = "The new topic to add", itemName = "newTopic")
                                 @Valid final RS_HelpTopic newTopic) {
        HelpTopic existing = helpTopicDAO.getByName(newTopic.getName());
        checkArgument(existing == null, "A topic with that name already exists");

        HelpTopicCollection collection = null;
        if (newTopic.getCollection() != null) {
            collection = collectionDAOProvider.get().getHelpTopicCollection(newTopic.getCollection().getId());
        }

        HelpTopic helpTopic = new HelpTopic(collection, newTopic.getName(), newTopic.getHelpText());
        helpTopicDAO.save(helpTopic);
        return RS_HelpTopic.fromHelpTopic(helpTopic, true);
    }

    @Documentation("Returns the topic with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_HelpTopic getTopic(@PathParam("id") final long id) {
        HelpTopic topic = helpTopicDAO.getHelpTopic(id);

        if (topic == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_HelpTopic.fromHelpTopic(topic, true);
    }

    @Documentation("Returns all the help topics, or optionally just the top level ones")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_HelpTopic>> getTopics(@Documentation("Whether to only include top level topics in the response")
                                                   @QueryParam("topLevelOnly")
                                                   final boolean topLevelOnly) {
        List<RS_HelpTopic> topics = new ArrayList<>();

        Collection<HelpTopic> allHelpTopics = topLevelOnly ? helpTopicDAO.getTopLevelTopics() : helpTopicDAO.getAllHelpTopics();
        for (HelpTopic helpTopic : allHelpTopics) {
            topics.add(RS_HelpTopic.fromHelpTopic(helpTopic, true));
        }

        return JResponse.ok(topics).build();
    }

    @Documentation("Updates the specified topic and returns the updated object")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_HelpTopic updateTopic(@PathParam("id") final long id,
                                    @Documentation(value = "The updated topic", itemName = "updatedTopic")
                                    final RS_HelpTopic updatedTopic) {
        HelpTopic topic = helpTopicDAO.getHelpTopic(id);
        if (topic == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedTopic).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        HelpTopic existing = helpTopicDAO.getByName(topic.getName());
        checkArgument(existing == null, "There's already a topic with that name");

        HelpTopicCollection collection = null;
        if (updatedTopic.getCollection() != null) {
            collection = collectionDAOProvider.get().getHelpTopicCollection(updatedTopic.getCollection().getId());
        }

        topic.setCollection(collection);
        topic.setName(updatedTopic.getName());
        topic.setHelpText(updatedTopic.getHelpText());

        return RS_HelpTopic.fromHelpTopic(topic, true);
    }

    @Documentation("Deletes the specified help topic")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteTopic(@PathParam("id") final long id) {
        HelpTopic topic = helpTopicDAO.getHelpTopic(id);
        if (topic == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        helpTopicDAO.delete(topic);
    }
}
