package web.resources;

import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import com.sun.jersey.api.JResponse;
import database.daos.WebLinkDAO;
import database.models.WebLink;
import web.documentation.Documentation;
import web.models.RS_WebLink;
import web.tools.WebContext;
import web.validation.Update;

import javax.inject.Inject;
import javax.inject.Provider;
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
@Path("weblinks")
public class WebLinkResource {
    private final WebLinkDAO webLinkDAO;
    private final Provider<Validator> validatorProvider;

    @Inject
    public WebLinkResource(final WebLinkDAO webLinkDAO, final Provider<Validator> validatorProvider) {
        this.webLinkDAO = webLinkDAO;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds a web link and returns the saved object. Admin only request")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_WebLink addWebLink(@Documentation(value = "The link to add", itemName = "newWebLink")
                                 @Valid final RS_WebLink newWebLink,
                                 @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        WebLink webLink = new WebLink(newWebLink.getName(), newWebLink.getUrl());
        webLink = webLinkDAO.save(webLink);
        return RS_WebLink.fromWebLink(webLink);
    }

    @Documentation("Returns the web link with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_WebLink getWebLink(@PathParam("id") final long id) {
        WebLink webLink = webLinkDAO.getWebLink(id);

        if (webLink == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_WebLink.fromWebLink(webLink);
    }

    @Documentation("Returns all web links")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_WebLink>> getWebLinks() {
        List<RS_WebLink> webLinks = new ArrayList<>();

        for (WebLink webLink : webLinkDAO.getAllWebLinks()) {
            webLinks.add(RS_WebLink.fromWebLink(webLink));
        }

        return JResponse.ok(webLinks).build();
    }

    @Documentation("Updates the specified web link and returns the updated object. Admin only request")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_WebLink updateWebLink(@PathParam("id") final long id,
                                    @Documentation(value = "The updated link", itemName = "updatedWebLink")
                                    final RS_WebLink updatedWebLink,
                                    @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        WebLink webLink = webLinkDAO.getWebLink(id);
        checkNotNull("No such web link");

        validate(updatedWebLink).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        webLink.setName(updatedWebLink.getName());
        webLink.setLink(updatedWebLink.getUrl());
        return RS_WebLink.fromWebLink(webLink);
    }

    @Documentation("Deletes the specified web link. Admin only request")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteWebLink(@PathParam("id") final long id,
                              @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        WebLink webLink = webLinkDAO.getWebLink(id);
        if (webLink == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        webLinkDAO.delete(webLink);
    }
}
