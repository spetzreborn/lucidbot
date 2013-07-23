package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.UserActivitiesDAO;
import database.models.UserActivities;
import web.models.RS_UserActivities;
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
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("users/activities")
public class UserActivityResource {
    private final UserActivitiesDAO userActivitiesDAO;
    private final Provider<BotUserDAO> botUserDAOProvider;

    @Inject
    public UserActivityResource(final UserActivitiesDAO userActivitiesDAO,
                                final Provider<BotUserDAO> botUserDAOProvider) {
        this.userActivitiesDAO = userActivitiesDAO;
        this.botUserDAOProvider = botUserDAOProvider;
    }

    /**
     * @param id the id of the activities
     * @return the activities with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_UserActivities getActivity(@PathParam("id") final long id) {
        UserActivities activities = userActivitiesDAO.getUserActivities(id);
        if (activities == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_UserActivities.fromUserActivities(activities);
    }

    /**
     * Returns all existing user activities, or just for the specified user
     *
     * @param userId the id of a user you want to get the activities for
     * @return a list of activities
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_UserActivities>> getActivities(@QueryParam("userId") final Long userId) {
        List<RS_UserActivities> activities = new ArrayList<>();
        if (userId == null) {
            for (UserActivities userActivities : userActivitiesDAO.getAllUserActivities()) {
                activities.add(RS_UserActivities.fromUserActivities(userActivities));
            }
        } else {
            BotUser user = botUserDAOProvider.get().getUser(userId);
            checkNotNull(user, "There's no such user");
            UserActivities userActivities = userActivitiesDAO.getUserActivities(user);
            if (userActivities != null) activities.add(RS_UserActivities.fromUserActivities(userActivities));
        }
        return JResponse.ok(activities).build();
    }

    /**
     * Updates user activities. Use admin credentials for this call. Only updates
     * activities that aren't null.
     *
     * @param id                the id of the activities to update
     * @param updatedActivities the updates
     * @return the updated activities
     */
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_UserActivities updateActivities(@PathParam("id") final long id,
                                              @Valid final RS_UserActivities updatedActivities,
                                              @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        UserActivities activities = userActivitiesDAO.getUserActivities(id);
        if (activities == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        RS_UserActivities.toUserActivities(activities, updatedActivities);
        return RS_UserActivities.fromUserActivities(activities);
    }
}
