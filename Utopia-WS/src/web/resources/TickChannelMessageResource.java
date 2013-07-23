package web.resources;

import api.database.Transactional;
import api.database.daos.ChannelDAO;
import api.database.models.Channel;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.TickChannelMessageDAO;
import database.models.TickChannelMessage;
import web.models.RS_TickChannelMessage;
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
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("tickmessages")
public class TickChannelMessageResource {
    private final TickChannelMessageDAO tickChannelMessageDAO;
    private final Provider<ChannelDAO> channelDAOProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public TickChannelMessageResource(final TickChannelMessageDAO tickChannelMessageDAO,
                                      final Provider<ChannelDAO> channelDAOProvider,
                                      final Provider<Validator> validatorProvider) {
        this.tickChannelMessageDAO = tickChannelMessageDAO;
        this.channelDAOProvider = channelDAOProvider;
        this.validatorProvider = validatorProvider;
    }

    /**
     * Adds a tick channel message
     *
     * @param newMessage the message to add
     * @return the added message
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_TickChannelMessage addMessage(@Valid final RS_TickChannelMessage newMessage,
                                            @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Channel channel = channelDAOProvider.get().getChannel(newMessage.getChannel());

        TickChannelMessage existing = tickChannelMessageDAO.getTickChannelMessage(channel);
        if (existing != null) throw new IllegalArgumentException("That channel already has a tick message");

        TickChannelMessage channelMessage = new TickChannelMessage(channel, newMessage.getMessage());
        channelMessage = tickChannelMessageDAO.save(channelMessage);
        return RS_TickChannelMessage.fromTickChannelMessage(channelMessage);
    }

    /**
     * @param id the id of the message
     * @return the message with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_TickChannelMessage getMessage(@PathParam("id") final long id) {
        TickChannelMessage message = tickChannelMessageDAO.getTickChannelMessage(id);

        if (message == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_TickChannelMessage.fromTickChannelMessage(message);
    }

    /**
     * Returns all existing messages, or just the ones for the specified channel
     *
     * @param channelId the id of the channel to get the message for
     * @return a list of messages
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_TickChannelMessage>> getMessages(@QueryParam("channelId") final Long channelId) {
        List<RS_TickChannelMessage> messages = new ArrayList<>();
        if (channelId == null) {
            for (TickChannelMessage message : tickChannelMessageDAO.getAllTickChannelMessages()) {
                messages.add(RS_TickChannelMessage.fromTickChannelMessage(message));
            }
        } else {
            Channel channel = channelDAOProvider.get().getChannel(channelId);
            checkNotNull(channel, "No such channel");
            TickChannelMessage tickChannelMessage = tickChannelMessageDAO.getTickChannelMessage(channel);
            if (tickChannelMessage != null)
                messages.add(RS_TickChannelMessage.fromTickChannelMessage(tickChannelMessage));
        }
        return JResponse.ok(messages).build();
    }

    /**
     * Updates a message
     *
     * @param id             the id of the message to update
     * @param updatedMessage the updates
     * @return the updated message
     */
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_TickChannelMessage updateMessage(@PathParam("id") final long id,
                                               final RS_TickChannelMessage updatedMessage,
                                               @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        TickChannelMessage message = tickChannelMessageDAO.getTickChannelMessage(id);
        if (message == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedMessage).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        message.setMessage(updatedMessage.getMessage());
        return RS_TickChannelMessage.fromTickChannelMessage(message);
    }

    /**
     * Deletes a message
     *
     * @param id the id of the message
     */
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteMessage(@PathParam("id") final long id,
                              @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        TickChannelMessage message = tickChannelMessageDAO.getTickChannelMessage(id);
        if (message == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        tickChannelMessageDAO.delete(message);
    }
}
