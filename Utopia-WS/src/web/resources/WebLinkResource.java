package web.resources;

import api.database.Transactional;
import api.tools.validation.ValidationEnabled;
import com.sun.jersey.api.JResponse;
import database.daos.WebLinkDAO;
import database.models.WebLink;
import web.models.RS_WebLink;
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
@Path("weblinks")
public class WebLinkResource {
    private final WebLinkDAO webLinkDAO;

    @Inject
    public WebLinkResource(final WebLinkDAO webLinkDAO) {
        this.webLinkDAO = webLinkDAO;
    }

    /**
     * Adds a web link
     *
     * @param newWebLink the link to add
     * @return the added link
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_WebLink addWebLink(@Valid final RS_WebLink newWebLink,
                                 @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        WebLink webLink = new WebLink(newWebLink.getName(), newWebLink.getUrl());
        webLink = webLinkDAO.save(webLink);
        return RS_WebLink.fromWebLink(webLink);
    }

    /**
     * @param id the id of the web link
     * @return the link with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_WebLink getWebLink(@PathParam("id") final long id) {
        WebLink webLink = webLinkDAO.getWebLink(id);
        if (webLink == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_WebLink.fromWebLink(webLink);
    }

    /**
     * Returns all existing web links
     *
     * @return a list of links
     */
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

    /**
     * Updates a web link
     *
     * @param id             the id of the link to update
     * @param updatedWebLink the updates
     * @return the updated link
     */
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_WebLink updateWebLink(@PathParam("id") final long id,
                                    final RS_WebLink updatedWebLink,
                                    @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        checkNotNull(updatedWebLink.getName(), "You must specify a name");
        checkNotNull(updatedWebLink.getUrl(), "You must specify a url");

        WebLink webLink = webLinkDAO.getWebLink(id);
        checkNotNull("No such web link");

        webLink.setName(updatedWebLink.getName());
        webLink.setLink(updatedWebLink.getUrl());
        return RS_WebLink.fromWebLink(webLink);
    }

    /**
     * Deletes a web link
     *
     * @param id the id of the link
     */
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
