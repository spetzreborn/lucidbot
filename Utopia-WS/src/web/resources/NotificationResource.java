package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.NotificationDAO;
import database.models.Notification;
import database.models.NotificationMethod;
import database.models.NotificationType;
import web.documentation.Documentation;
import web.models.RS_Notification;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("notifications")
public class NotificationResource {
    private final NotificationDAO notificationDAO;
    private final Provider<BotUserDAO> userDAOProvider;

    @Inject
    public NotificationResource(final NotificationDAO notificationDAO,
                                final Provider<BotUserDAO> userDAOProvider) {
        this.notificationDAO = notificationDAO;
        this.userDAOProvider = userDAOProvider;
    }

    @Documentation("Adds a new notification and returns the saved object. Only admins can add for other users")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Notification addNotification(@Documentation(value = "The notification to add", itemName = "newNotification")
                                           @Valid final RS_Notification newNotification,
                                           @Context final WebContext webContext) {
        NotificationMethod method = NotificationMethod.getByName(newNotification.getMethod());
        NotificationType type = NotificationType.getByName(newNotification.getType());

        BotUser user;
        if (newNotification.getUser() != null) {
            if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);
            user = userDAOProvider.get().getUser(newNotification.getUser().getId());
        } else user = webContext.getBotUser();

        Notification notification = new Notification(user, type, method);
        notification = notificationDAO.save(notification);
        return RS_Notification.fromNotification(notification);
    }

    @Documentation("Returns all the notifications for either the logged in user, or the one specified by the query param (admin only option)")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Notification>> getNotifications(@Documentation("The id of the user to get notifications for")
                                                             @QueryParam("userId")
                                                             final Long userId,
                                                             @Context final WebContext webContext) {
        BotUser callingUser = webContext.getBotUser();
        boolean callingForSelf = userId == null || callingUser.getId().equals(userId);
        if (callingForSelf || callingUser.isAdmin()) {
            BotUser user = callingForSelf ? callingUser : userDAOProvider.get().getUser(userId);
            if (user == null) throw new IllegalArgumentException("No such user");

            List<Notification> allNotifications = notificationDAO.getAllNotifications(user);
            List<RS_Notification> notifications = new ArrayList<>(allNotifications.size());
            for (Notification notification : allNotifications) {
                notifications.add(RS_Notification.fromNotification(notification));
            }
            return JResponse.ok(notifications).build();
        }

        throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    @Documentation("Deletes the specified notification. Admins can remove any notification, but regular users can only remove their own")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteNotification(@PathParam("id") final long id,
                                   @Context final WebContext webContext) {
        Notification notification = notificationDAO.getNotification(id);
        if (notification == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        BotUser user = webContext.getBotUser();
        if (!notification.getUser().equals(user) && !user.isAdmin())
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        notificationDAO.delete(notification);
    }
}
