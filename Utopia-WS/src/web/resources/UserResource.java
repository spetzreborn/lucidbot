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
import api.database.daos.BotUserDAO;
import api.database.daos.NicknameDAO;
import api.database.models.BotUser;
import api.database.models.Nickname;
import api.database.models.UserStatistic;
import api.events.bot.UserRemovedEvent;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.UserActivitiesDAO;
import database.models.UserActivities;
import web.models.RS_Nickname;
import web.models.RS_User;
import web.models.RS_UserStatistic;
import web.tools.AfterCommitEventPoster;
import web.tools.WebContext;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static api.tools.text.StringUtil.isNullOrEmpty;
import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("users")
public class UserResource {
    private final BotUserDAO userDAO;
    private final Provider<UserActivitiesDAO> userActivitiesDAOProvider;
    private final Provider<NicknameDAO> nicknameDAOProvider;
    private final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public UserResource(final BotUserDAO userDAO,
                        final Provider<UserActivitiesDAO> userActivitiesDAOProvider,
                        final Provider<NicknameDAO> nicknameDAOProvider,
                        final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider,
                        final Provider<Validator> validatorProvider) {
        this.userDAO = userDAO;
        this.userActivitiesDAOProvider = userActivitiesDAOProvider;
        this.nicknameDAOProvider = nicknameDAOProvider;
        this.afterCommitEventPosterProvider = afterCommitEventPosterProvider;
        this.validatorProvider = validatorProvider;
    }

    /**
     * Adds a new user, provided the nick isn't already taken
     *
     * @param user the user to add
     * @return the added user
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_User addUser(@Valid final RS_User user,
                           @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        checkArgument(userDAO.getUser(user.getName()) == null, "The selected nickname is not available");

        BotUser newUser = new BotUser(user.getName(), user.getAdmin(), user.getOwner());
        newUser.setPassword("password");

        NicknameDAO nicknameDAO = nicknameDAOProvider.get();
        for (Iterator<RS_Nickname> iter = user.getNicknames().iterator(); iter.hasNext(); ) {
            if (nicknameDAO.getNickname(iter.next().getName()) != null) iter.remove();
        }

        RS_User.toBotUser(newUser, user);
        newUser = userDAO.save(newUser);
        userActivitiesDAOProvider.get().save(new UserActivities(newUser));
        return RS_User.fromBotUser(newUser, true);
    }

    /**
     * Lists all users, or just the one who owns the specified nick
     *
     * @param nick the nick of the user to limit the response to
     * @return a list of users
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_User>> getUsers(@QueryParam("nick") final String nick) {
        if (isNullOrEmpty(nick)) {
            Collection<BotUser> allUsers = userDAO.getAllUsers();
            List<RS_User> users = new ArrayList<>(allUsers.size());
            for (BotUser user : allUsers) {
                users.add(RS_User.fromBotUser(user, true));
            }
            return JResponse.ok(users).build();
        } else {
            BotUser user = userDAO.getUser(nick);
            checkNotNull(user, "There's no such user");
            return JResponse.ok(Arrays.asList(RS_User.fromBotUser(user, true))).build();
        }
    }

    /**
     * @return a list of admin users
     */
    @Path("admins")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_User>> getAdmins() {
        Collection<BotUser> allAdmins = userDAO.getAdminUsers();
        List<RS_User> admins = new ArrayList<>(allAdmins.size());
        for (BotUser user : allAdmins) {
            admins.add(RS_User.fromBotUser(user, true));
        }
        return JResponse.ok(admins).build();
    }

    /**
     * @param id the id of the user
     * @return the user associated with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_User getUser(@PathParam("id") final long id) {
        BotUser user = userDAO.getUser(id);

        if (user == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_User.fromBotUser(user, true);
    }

    /**
     * @param userId the userId of the user
     * @return a list of statistics for the specified user
     */
    @Path("{userId : \\d+}/statistics")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_UserStatistic>> getUserStatistics(@PathParam("userId") final long userId) {
        BotUser user = userDAO.getUser(userId);
        if (user == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        List<UserStatistic> userStats = user.getStats();
        List<RS_UserStatistic> statistics = new ArrayList<>(userStats.size());
        for (UserStatistic statistic : userStats) {
            statistics.add(RS_UserStatistic.fromUserStatistic(statistic));
        }
        return JResponse.ok(statistics).build();
    }

    /**
     * Updates the specified user
     *
     * @param id          the id of the user
     * @param updatedUser the updates
     * @return the updated user
     */
    @Path("{id : \\d+}")
    @PUT
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_User updateUser(@PathParam("id") final long id,
                              final RS_User updatedUser,
                              @Context final WebContext webContext) {
        BotUser user = userDAO.getUser(id);
        if (user == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedUser).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        if (!webContext.getBotUser().equals(user) && !webContext.isInRole(ADMIN_ROLE))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        NicknameDAO nicknameDAO = nicknameDAOProvider.get();
        for (Iterator<RS_Nickname> iter = updatedUser.getNicknames().iterator(); iter.hasNext(); ) {
            Nickname existing = nicknameDAO.getNickname(iter.next().getName());
            if (existing != null && !existing.getUser().equals(user)) iter.remove();
        }

        RS_User.toBotUser(user, updatedUser);
        return RS_User.fromBotUser(user, true);
    }

    /**
     * Deletes the specified user, provided the user in question isn't the bot owner
     *
     * @param id the id of the user to delete
     */
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteUser(@PathParam("id") final long id,
                           @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        BotUser user = userDAO.getUser(id);
        if (user == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        else if (user.isOwner()) throw new WebApplicationException(Response.Status.FORBIDDEN);

        userDAO.delete(user);
        afterCommitEventPosterProvider.get().addEventToPost(new UserRemovedEvent(user.getMainNick()));
    }
}
