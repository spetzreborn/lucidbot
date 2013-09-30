package web.resources;

import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.sun.jersey.api.JResponse;
import database.daos.KingdomDAO;
import database.daos.NewsItemDAO;
import database.models.Kingdom;
import database.models.NewsItem;
import org.hibernate.validator.constraints.NotEmpty;
import tools.parsing.NewsParser;
import web.documentation.Documentation;
import web.models.RS_NewsItem;
import web.tools.DateParameter;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static api.tools.collections.CollectionUtil.isNotEmpty;

@ValidationEnabled
@Path("news")
public class NewsResource {
    private final NewsItemDAO newsItemDAO;
    private final Provider<NewsParser> newsParser;
    private final Provider<KingdomDAO> kingdomDAOProvider;

    @Inject
    public NewsResource(final NewsItemDAO newsItemDAO,
                        final Provider<NewsParser> newsParser,
                        final Provider<KingdomDAO> kingdomDAOProvider) {
        this.newsItemDAO = newsItemDAO;
        this.newsParser = newsParser;
        this.kingdomDAOProvider = kingdomDAOProvider;
    }

    @Documentation("Parses the incoming text and returns the news items created from it")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    @Transactional
    public JResponse<List<RS_NewsItem>> addNews(@Documentation(value = "The news from utopia", itemName = "newNews")
                                                @NotEmpty(message = "The news must not be null or empty")
                                                final String newNews) {
        Collection<NewsItem> newsItems = newsParser.get().parseNews(newNews);

        newsItems = newsItemDAO.save(newsItems);

        List<RS_NewsItem> parsed = new ArrayList<>(newsItems.size());
        for (NewsItem item : newsItems) {
            parsed.add(RS_NewsItem.fromNewsItem(item));
        }

        return JResponse.ok(parsed).build();
    }

    @Documentation("Returns the news item with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_NewsItem getNewsItem(@PathParam("id") final long id) {
        NewsItem newsItem = newsItemDAO.getNewsItem(id);

        if (newsItem == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_NewsItem.fromNewsItem(newsItem);
    }

    @Documentation("Fetches all news item from between the specified dates or id's that involve the specified kingdoms. All parameters are optional. " +
            "Combining a kd id with from and to is usually a good way to zero in a specific conflict and/or war.")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_NewsItem>> getNews(@Documentation("The id's of the kingdoms to limit the news items to")
                                                @QueryParam("kingdomIds")
                                                final List<Long> kingdomIds,
                                                @Documentation("The earliest date to select news items from. " +
                                                        "Only items from this date and time and forward will be included in the response")
                                                @QueryParam("fromDate")
                                                final DateParameter fromDate,
                                                @Documentation("Same as the from date, except this uses id's of specific news items instead " +
                                                        "(for example the id of the war declaration item)")
                                                @QueryParam("fromId")
                                                final Long fromId,
                                                @Documentation("The opposite of the from date, this puts an upper limit on the items instead")
                                                @QueryParam("toDate")
                                                final DateParameter toDate,
                                                @Documentation("The opposite of the from id, this puts an upper limit on the items instead")
                                                @QueryParam("toId")
                                                final Long toId) {
        Collection<NewsItem> news = newsItemDAO.getNewsBetween(
                fromDate == null ? null : fromDate.asDate(),
                fromId,
                toDate == null ? null : toDate.asDate(),
                toId
        );

        if (isNotEmpty(kingdomIds)) {
            List<Kingdom> kingdoms = kingdomDAOProvider.get().getKingdoms(kingdomIds);
            news = Collections2.filter(news, createEnemyKingdomFilter(kingdoms));
        }

        List<RS_NewsItem> out = new ArrayList<>(news.size());
        for (NewsItem newsItem : news) {
            out.add(RS_NewsItem.fromNewsItem(newsItem));
        }
        return JResponse.ok(out).build();
    }

    private static Predicate<NewsItem> createEnemyKingdomFilter(final Collection<Kingdom> kingdoms) {
        return new Predicate<NewsItem>() {
            @Override
            public boolean apply(@Nullable final NewsItem input) {
                for (Kingdom kingdom : kingdoms) {
                    if ((input.getSource() != null && input.getSource().endsWith(kingdom.getLocation())) ||
                            (input.getTarget() != null && input.getTarget().endsWith(kingdom.getLocation()))) return true;
                }
                return false;
            }
        };
    }

    @Documentation("Deletes the specified news item")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteNewsItem(@PathParam("id") final long id) {
        NewsItem newsItem = newsItemDAO.getNewsItem(id);

        if (newsItem == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        newsItemDAO.delete(newsItem);
    }

}
