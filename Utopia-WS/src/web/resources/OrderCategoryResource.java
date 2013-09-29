package web.resources;

import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.OrderCategoryDAO;
import database.models.OrderCategory;
import web.documentation.Documentation;
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

    @Documentation("Adds a new order category and returns the saved object. Admin only request")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_OrderCategory addCategory(@Documentation(value = "The new category to add", itemName = "newCategory")
                                        @Valid final RS_OrderCategory newCategory,
                                        @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        OrderCategory existing = orderCategoryDAO.getOrderCategory(newCategory.getName());
        checkArgument(existing == null, "A category with that name already exists");

        OrderCategory category = new OrderCategory(newCategory.getName());
        category = orderCategoryDAO.save(category);
        return RS_OrderCategory.fromOrderCategory(category);
    }

    @Documentation("Returns the category with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_OrderCategory getCategory(@PathParam("id") final long id) {
        OrderCategory category = orderCategoryDAO.getOrderCategory(id);

        if (category == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_OrderCategory.fromOrderCategory(category);
    }

    @Documentation("Returns all existing categories")
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

    @Documentation("Updates a category and returns the updated object. Admin only request")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_OrderCategory updateCategory(@PathParam("id") final long id,
                                           @Documentation(value = "The updated category", itemName = "updatedCategory")
                                           final RS_OrderCategory updatedCategory,
                                           @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        OrderCategory category = orderCategoryDAO.getOrderCategory(id);
        if (category == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedCategory).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        category.setName(updatedCategory.getName());
        return RS_OrderCategory.fromOrderCategory(category);
    }

    @Documentation("Deletes the specified category. The orders that were in that category will no longer be in any category. Admin only request")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteOrder(@PathParam("id") final long id,
                            @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        OrderCategory category = orderCategoryDAO.getOrderCategory(id);
        if (category == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        orderCategoryDAO.delete(category);
    }
}
