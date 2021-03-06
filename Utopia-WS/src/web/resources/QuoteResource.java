package web.resources;

import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import com.sun.jersey.api.JResponse;
import database.daos.QuoteDAO;
import database.models.Quote;
import web.documentation.Documentation;
import web.models.RS_Quote;
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
@Path("quotes")
public class QuoteResource {
    private final QuoteDAO quoteDAO;

    @Inject
    public QuoteResource(final QuoteDAO quoteDAO) {
        this.quoteDAO = quoteDAO;
    }

    @Documentation("Adds a new quote and returns the saved object")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Quote addQuote(@Documentation(value = "The new quote to add", itemName = "newQuote")
                             @Valid final RS_Quote newQuote,
                             @Context final WebContext webContext) {
        Quote quote = new Quote(webContext.getName(), newQuote.getQuote());
        quote = quoteDAO.save(quote);
        return RS_Quote.fromQuote(quote);
    }

    @Documentation("Returns the quote with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Quote getQuote(@PathParam("id") final long id) {
        Quote quote = quoteDAO.getQuote(id);

        if (quote == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Quote.fromQuote(quote);
    }

    @Documentation("Returns all quotes")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Quote>> getQuotes() {
        List<RS_Quote> orders = new ArrayList<>();

        for (Quote quote : quoteDAO.getAllQuotes()) {
            orders.add(RS_Quote.fromQuote(quote));
        }

        return JResponse.ok(orders).build();
    }

    @Documentation("Deletes the specified quote, provided the user is allowed to (either admin or the user that added the quote in the first place)")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteQuote(@PathParam("id") final long id,
                            @Context final WebContext webContext) {
        Quote quote = quoteDAO.getQuote(id);
        if (quote == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        if (!webContext.isInRole(ADMIN_ROLE) && !quote.getAddedBy().equals(webContext.getName()))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        quoteDAO.delete(quote);
    }
}
