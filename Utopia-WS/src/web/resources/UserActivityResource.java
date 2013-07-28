package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.UserActivitiesDAO;
import database.models.UserActivities;
import web.documentation.Documentation;
import web.models.RS_UserActivities;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

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

    @Documentation("Returns the activity with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_UserActivities getActivity(@PathParam("id") final long id) {
        UserActivities activities = userActivitiesDAO.getUserActivities(id);

        if (activities == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_UserActivities.fromUserActivities(activities);
    }

    @Documentation("Returns user activites for all users, or optionally for the just the one specified user")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_UserActivities>> getActivities(@Documentation("The id of the user to get activites for")
                                                            @QueryParam("userId")
                                                            final Long userId) {
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

    @Documentation("Updates activities and returns the updated object. The types that shouldn't be updated can safely be left out in the request object")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_UserActivities updateActivities(@PathParam("id") final long id,
                                              @Documentation(value = "The updates to do", itemName = "updatedActivities")
                                              @Valid final RS_UserActivities updatedActivities) {
        UserActivities activities = userActivitiesDAO.getUserActivities(id);
        if (activities == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        RS_UserActivities.toUserActivities(activities, updatedActivities);
        return RS_UserActivities.fromUserActivities(activities);
    }
}
