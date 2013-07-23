package web.resources;

import api.database.Transactional;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.OrderCategoryDAO;
import database.models.OrderCategory;
import web.models.RS_OrderCategory;
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
import static com.google.common.base.Preconditions.checkArgument;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("orders/categories")
public class OrderCategoryResource {
    private final OrderCategoryDAO orderCategoryDAO;
    private final Provider<Validator> validatorProvider;

    @Inject
    public OrderCategoryResource(final OrderCategoryDAO orderCategoryDAO,
                                 final Provider<Validator> validatorProvider) {
        this.orderCategoryDAO = orderCategoryDAO;
        this.validatorProvider = validatorProvider;
    }

    /**
     * Adds an order category
     *
     * @param newCategory the order category to add
     * @return the added order category
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_OrderCategory addCategory(@Valid final RS_OrderCategory newCategory) {
        OrderCategory existing = orderCategoryDAO.getOrderCategory(newCategory.getName());
        checkArgument(existing == null, "A category with that name already exists");

        OrderCategory category = new OrderCategory(newCategory.getName());
        category = orderCategoryDAO.save(category);
        return RS_OrderCategory.fromOrderCategory(category);
    }

    /**
     * @param id the id of the order category
     * @return the order category with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_OrderCategory getCategory(@PathParam("id") final long id) {
        OrderCategory category = orderCategoryDAO.getOrderCategory(id);

        if (category == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_OrderCategory.fromOrderCategory(category);
    }

    /**
     * Returns all existing order categories
     *
     * @return a list of order categories
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_OrderCategory>> getCategories() {
        List<RS_OrderCategory> categories = new ArrayList<>();

        for (OrderCategory orderCategory : orderCategoryDAO.getAllOrderCategories()) {
            categories.add(RS_OrderCategory.fromOrderCategory(orderCategory));
        }

        return JResponse.ok(categories).build();
    }

    /**
     * Updates an order category
     *
     * @param id              the id of the order category to update
     * @param updatedCategory the updates
     * @return the updated order category
     */
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_OrderCategory updateCategory(@PathParam("id") final long id,
                                           final RS_OrderCategory updatedCategory,
                                           @Context final WebContext webContext) {
        OrderCategory category = orderCategoryDAO.getOrderCategory(id);
        if (category == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedCategory).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        if (!webContext.isInRole(ADMIN_ROLE))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        category.setName(updatedCategory.getName());
        return RS_OrderCategory.fromOrderCategory(category);
    }

    /**
     * Deletes an order category (and sets the category of existing commands in this category to null)
     *
     * @param id the id of the order category
     */
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteOrder(@PathParam("id") final long id,
                            @Context final WebContext webContext) {
        OrderCategory category = orderCategoryDAO.getOrderCategory(id);
        if (category == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        if (!webContext.isInRole(ADMIN_ROLE))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        orderCategoryDAO.delete(category);
    }
}
