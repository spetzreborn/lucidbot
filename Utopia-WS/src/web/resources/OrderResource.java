package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.OrderCategoryDAO;
import database.daos.OrderDAO;
import database.models.Bindings;
import database.models.Order;
import database.models.OrderCategory;
import events.OrderAddedEvent;
import tools.BindingsManager;
import web.documentation.Documentation;
import web.models.RS_Order;
import web.tools.AfterCommitEventPoster;
import web.tools.BindingsParser;
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
@Path("orders")
public class OrderResource {
    private final OrderDAO orderDAO;
    private final Provider<OrderCategoryDAO> orderCategoryDAOProvider;
    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<BindingsParser> bindingsParserProvider;
    private final Provider<BindingsManager> bindingsManagerProvider;
    private final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public OrderResource(final OrderDAO orderDAO,
                         final Provider<OrderCategoryDAO> orderCategoryDAOProvider,
                         final Provider<BotUserDAO> userDAOProvider,
                         final Provider<BindingsParser> bindingsParserProvider,
                         final Provider<BindingsManager> bindingsManagerProvider,
                         final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider,
                         final Provider<Validator> validatorProvider) {
        this.orderDAO = orderDAO;
        this.orderCategoryDAOProvider = orderCategoryDAOProvider;
        this.userDAOProvider = userDAOProvider;
        this.bindingsParserProvider = bindingsParserProvider;
        this.bindingsManagerProvider = bindingsManagerProvider;
        this.afterCommitEventPosterProvider = afterCommitEventPosterProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds an order, fires off an OrderAddedEvent and returns the saved object. Admin only request")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Order addOrder(@Documentation(value = "The new order to add", itemName = "newOrder")
                             @Valid final RS_Order newOrder,
                             @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Bindings bindings = bindingsParserProvider.get().parse(newOrder.getBindings());
        OrderCategory category = null;
        if (newOrder.getCategory() != null)
            category = orderCategoryDAOProvider.get().getOrderCategory(newOrder.getCategory().getId());

        Order order = new Order(bindings, category, newOrder.getOrder(), webContext.getName());
        order = orderDAO.save(order);
        afterCommitEventPosterProvider.get().addEventToPost(new OrderAddedEvent(order.getId(), null));
        return RS_Order.fromOrder(order);
    }

    @Documentation("Returns the order with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Order getOrder(@PathParam("id") final long id) {
        Order order = orderDAO.getOrder(id);

        if (order == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Order.fromOrder(order);
    }

    @Documentation("Returns all the order, or optionally just those specifically targetted at the specified user (including untargetted one as well)")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Order>> getOrders(@Documentation("The id of the user to get orders for")
                                               @QueryParam("userId")
                                               final Long userId) {
        List<RS_Order> orders = new ArrayList<>();
        if (userId == null) {
            for (Order order : orderDAO.getAllOrders()) {
                orders.add(RS_Order.fromOrder(order));
            }
        } else {
            BotUser user = userDAOProvider.get().getUser(userId);
            checkNotNull(user, "There's no such user");
            for (Order order : orderDAO.getOrdersForUser(user, bindingsManagerProvider.get())) {
                orders.add(RS_Order.fromOrder(order));
            }
        }
        return JResponse.ok(orders).build();
    }

    @Documentation("Updates the specified order and returns the updated object. Admin only request")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Order updateOrder(@PathParam("id") final long id,
                                @Documentation(value = "The updated order", itemName = "updatedOrder")
                                final RS_Order updatedOrder,
                                @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Order order = orderDAO.getOrder(id);
        if (order == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedOrder).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        OrderCategory category = null;
        if (updatedOrder.getCategory() != null)
            category = orderCategoryDAOProvider.get().getOrderCategory(updatedOrder.getCategory().getId());

        order.setOrder(updatedOrder.getOrder());
        order.setCategory(category);
        return RS_Order.fromOrder(order);
    }

    @Documentation("Deletes the specified order. Admin only request")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteOrder(@PathParam("id") final long id,
                            @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Order order = orderDAO.getOrder(id);
        if (order == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        orderDAO.delete(order);
    }
}
