package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.ProvinceDAO;
import database.daos.UserSpellOpTargetDAO;
import database.models.Province;
import database.models.UserSpellOpTarget;
import web.models.RS_UserSpellOpTarget;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@ValidationEnabled
@Path("users/spelloptargets")
public class UserSpellOpTargetResource {
    private final UserSpellOpTargetDAO userSpellOpTargetDAO;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;

    @Inject
    public UserSpellOpTargetResource(final UserSpellOpTargetDAO userSpellOpTargetDAO,
                                     final Provider<ProvinceDAO> provinceDAOProvider,
                                     final Provider<BotUserDAO> botUserDAOProvider) {
        this.userSpellOpTargetDAO = userSpellOpTargetDAO;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;
    }

    /**
     * Adds/updates a spell/op target
     *
     * @param newTarget the target to add
     * @return the added target
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_UserSpellOpTarget addTarget(@Valid final RS_UserSpellOpTarget newTarget,
                                          @Context final WebContext webContext) {
        Province province = provinceDAOProvider.get().getProvince(newTarget.getTarget().getId());

        BotUser user = webContext.getBotUser();

        UserSpellOpTarget existing = userSpellOpTargetDAO.getUserSpellOpTarget(user);
        if (existing != null) userSpellOpTargetDAO.delete(existing);

        UserSpellOpTarget spellOpTarget = new UserSpellOpTarget(user, province);
        spellOpTarget = userSpellOpTargetDAO.save(spellOpTarget);
        return RS_UserSpellOpTarget.fromUserSpellOpTarget(spellOpTarget);
    }

    /**
     * @param id the id of the target
     * @return the target with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_UserSpellOpTarget getTarget(@PathParam("id") final long id) {
        UserSpellOpTarget target = userSpellOpTargetDAO.getUserSpellOpTarget(id);

        if (target == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_UserSpellOpTarget.fromUserSpellOpTarget(target);
    }

    /**
     * Returns all existing targets, or just the one for the specified user
     *
     * @param userId the user to get the target for
     * @return a list of targets
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_UserSpellOpTarget>> getTargets(@QueryParam("userId") final Long userId) {
        List<RS_UserSpellOpTarget> targets = new ArrayList<>();
        if (userId == null) {
            for (UserSpellOpTarget target : userSpellOpTargetDAO.getAllUserSpellOpTargets()) {
                targets.add(RS_UserSpellOpTarget.fromUserSpellOpTarget(target));
            }
        } else {
            BotUser user = botUserDAOProvider.get().getUser(userId);
            checkNotNull(user, "No such user");
            UserSpellOpTarget target = userSpellOpTargetDAO.getUserSpellOpTarget(user);
            if (target != null)
                targets.add(RS_UserSpellOpTarget.fromUserSpellOpTarget(target));
        }
        return JResponse.ok(targets).build();
    }

    /**
     * Deletes a target
     *
     * @param id the id of the target
     */
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteTarget(@PathParam("id") final long id) {
        UserSpellOpTarget target = userSpellOpTargetDAO.getUserSpellOpTarget(id);

        if (target == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        userSpellOpTargetDAO.delete(target);
    }
}
