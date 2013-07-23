package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.WaitDAO;
import database.models.Wait;
import web.models.RS_Wait;
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
@Path("users/wait")
public class WaitResource {
    private final WaitDAO waitDAO;
    private final Provider<BotUserDAO> botUserDAOProvider;

    @Inject
    public WaitResource(final WaitDAO waitDAO,
                        final Provider<BotUserDAO> botUserDAOProvider) {
        this.waitDAO = waitDAO;
        this.botUserDAOProvider = botUserDAOProvider;
    }

    /**
     * Adds a wait
     *
     * @param newWait the wait to add
     * @return the added wait
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Wait addWait(@Valid final RS_Wait newWait,
                           @Context final WebContext webContext) {
        BotUser waitingFor = botUserDAOProvider.get().getUser(newWait.getWaitingFor().getId());

        BotUser user = webContext.getBotUser();

        Wait existing = waitDAO.getWait(user, waitingFor);
        if (existing != null) throw new IllegalArgumentException("You're already waiting for that user");

        Wait wait = new Wait(user, waitingFor);
        wait = waitDAO.save(wait);
        return RS_Wait.fromWait(wait);
    }

    /**
     * @param id the id of the wait
     * @return the wait with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Wait getWait(@PathParam("id") final long id,
                           @Context final WebContext webContext) {
        Wait wait = waitDAO.getWait(id);
        if (wait == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        BotUser user = webContext.getBotUser();
        if (!user.equals(wait.getUser())) throw new WebApplicationException(Response.Status.FORBIDDEN);

        return RS_Wait.fromWait(wait);
    }

    /**
     * Returns all existing waits for the current user
     *
     * @return a list of waits
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Wait>> getWaits(@Context final WebContext webContext) {
        List<RS_Wait> waits = new ArrayList<>();

        BotUser user = webContext.getBotUser();
        for (Wait wait : waitDAO.getWaitsForUser(user)) {
            waits.add(RS_Wait.fromWait(wait));
        }

        return JResponse.ok(waits).build();
    }

    /**
     * Deletes a wait
     *
     * @param id the id of the wait
     */
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteWait(@PathParam("id") final long id,
                           @Context final WebContext webContext) {
        BotUser user = webContext.getBotUser();

        Wait wait = waitDAO.getWait(id);
        if (wait == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        else if (!user.equals(wait.getUser())) throw new WebApplicationException(Response.Status.FORBIDDEN);

        waitDAO.delete(wait);
    }
}
