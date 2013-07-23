package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.ProvinceDAO;
import database.daos.UserCheckinDAO;
import database.models.Province;
import database.models.UserCheckIn;
import web.models.RS_UserCheckin;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static api.tools.collections.CollectionUtil.isEmpty;
import static com.google.common.base.Preconditions.checkNotNull;

@ValidationEnabled
@Path("users/checkins")
public class UserCheckinResource {
    private final UserCheckinDAO userCheckinDAO;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;

    @Inject
    public UserCheckinResource(final UserCheckinDAO userCheckinDAO,
                               final Provider<ProvinceDAO> provinceDAOProvider,
                               final Provider<BotUserDAO> botUserDAOProvider) {
        this.userCheckinDAO = userCheckinDAO;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;
    }

    /**
     * Adds/updates a user checkin
     *
     * @param newCheckin the checkin to add
     * @return the added checkin
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_UserCheckin addCheckin(@Valid final RS_UserCheckin newCheckin,
                                     @Context final WebContext webContext) {
        BotUser user;
        Province userProvince;
        if (newCheckin.getUser() == null) {
            user = webContext.getBotUser();
            userProvince = provinceDAOProvider.get().getProvinceForUser(user);
            checkNotNull(userProvince, "You can't checkin without a province");
        } else {
            user = botUserDAOProvider.get().getUser(newCheckin.getUser().getId());
            userProvince = provinceDAOProvider.get().getProvinceForUser(user);
            checkNotNull(userProvince, "That user has no province");
        }

        UserCheckIn checkinForUser = userCheckinDAO.getCheckinForUser(user);
        if (checkinForUser == null) {
            checkinForUser = userCheckinDAO.save(new UserCheckIn(user, userProvince, newCheckin.getCheckedIn()));
        } else {
            checkinForUser.setCheckedIn(newCheckin.getCheckedIn());
            checkinForUser.setCheckInTime(new Date());
        }
        return RS_UserCheckin.fromUserCheckin(checkinForUser);
    }

    /**
     * @param id the id of the checkin
     * @return the checkin with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_UserCheckin getCheckin(@PathParam("id") final long id) {
        UserCheckIn checkin = userCheckinDAO.getUserCheckIn(id);

        if (checkin == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_UserCheckin.fromUserCheckin(checkin);
    }

    /**
     * Returns all existing checkins, or just the ones for the specified users
     *
     * @param userIds the users to get the checkins for
     * @return a list of checkins
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_UserCheckin>> getCheckins(@QueryParam("userIds") final List<Long> userIds) {
        List<RS_UserCheckin> checkins = new ArrayList<>();
        if (isEmpty(userIds)) {
            for (UserCheckIn checkIn : userCheckinDAO.getAllCheckins()) {
                checkins.add(RS_UserCheckin.fromUserCheckin(checkIn));
            }
        } else {
            BotUserDAO botUserDAO = botUserDAOProvider.get();
            for (Long userId : userIds) {
                BotUser user = botUserDAO.getUser(userId);
                if (user != null) {
                    UserCheckIn userCheckIn = userCheckinDAO.getCheckinForUser(user);
                    if (userCheckIn != null) checkins.add(RS_UserCheckin.fromUserCheckin(userCheckIn));
                }
            }
        }
        return JResponse.ok(checkins).build();
    }

    /**
     * Deletes a checkin
     *
     * @param id the id of the checkin
     */
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteCheckin(@PathParam("id") final long id) {
        UserCheckIn checkin = userCheckinDAO.getUserCheckIn(id);

        if (checkin == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        userCheckinDAO.delete(checkin);
    }
}
