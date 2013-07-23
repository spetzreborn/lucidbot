package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.PrivateMessageDAO;
import database.models.PrivateMessage;
import web.models.RS_PrivateMessage;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@ValidationEnabled
@Path("privatemessages")
public class PrivateMessageResource {
    private final PrivateMessageDAO privateMessageDAO;
    private final Provider<BotUserDAO> userDAOProvider;

    @Inject
    public PrivateMessageResource(final PrivateMessageDAO privateMessageDAO,
                                  final Provider<BotUserDAO> userDAOProvider) {
        this.privateMessageDAO = privateMessageDAO;
        this.userDAOProvider = userDAOProvider;
    }

    /**
     * Adds a private message
     *
     * @param newPM the pm to add
     * @return the added pm
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_PrivateMessage addPM(@Valid final RS_PrivateMessage newPM,
                                   @Context final WebContext webContext) {
        String sender = webContext.getName();
        BotUser recipient = userDAOProvider.get().getUser(newPM.getRecipient().getId());

        PrivateMessage privateMessage = new PrivateMessage(recipient, sender, newPM.getMessage());
        privateMessage = privateMessageDAO.save(privateMessage);
        return RS_PrivateMessage.fromPrivateMessage(privateMessage);
    }

    /**
     * @param id the id of the pm
     * @return the pm with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_PrivateMessage getPM(@PathParam("id") final long id,
                                   @Context final WebContext webContext) {
        PrivateMessage pm = privateMessageDAO.getPrivateMessage(id);
        if (pm == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        BotUser user = webContext.getBotUser();
        if (!pm.getRecipient().equals(user) && !pm.getSender().equals(user.getMainNick()))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        return RS_PrivateMessage.fromPrivateMessage(pm);
    }

    /**
     * Returns all received pms for the current user
     *
     * @return a list of pms
     */
    @Path("received")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_PrivateMessage>> getReceivedPMs(@Context final WebContext webContext) {
        BotUser user = webContext.getBotUser();

        List<RS_PrivateMessage> pms = new ArrayList<>();

        for (PrivateMessage pm : privateMessageDAO.getReceivedPrivateMessages(user)) {
            pms.add(RS_PrivateMessage.fromPrivateMessage(pm));
        }

        return JResponse.ok(pms).build();
    }

    /**
     * Returns all sent pms for the current user
     *
     * @return a list of pms
     */
    @Path("sent")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_PrivateMessage>> getSentPMs(@Context final WebContext webContext) {
        BotUser user = webContext.getBotUser();

        List<RS_PrivateMessage> pms = new ArrayList<>();

        for (PrivateMessage pm : privateMessageDAO.getSentPrivateMessages(user.getMainNick())) {
            pms.add(RS_PrivateMessage.fromPrivateMessage(pm));
        }

        return JResponse.ok(pms).build();
    }

    /**
     * Deletes a pm
     *
     * @param id the id of the pm
     */
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deletePM(@PathParam("id") final long id,
                         @Context final WebContext webContext) {
        PrivateMessage pm = privateMessageDAO.getPrivateMessage(id);
        if (pm == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        BotUser user = webContext.getBotUser();
        if (!user.equals(pm.getRecipient()))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        privateMessageDAO.delete(pm);
    }
}
