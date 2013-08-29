package web.resources;

import api.events.DelayedEventPoster;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.IntelDAO;
import database.daos.KingdomDAO;
import database.models.Kingdom;
import database.models.SoT;
import intel.Intel;
import intel.IntelParser;
import intel.IntelParserManager;
import web.models.RS_SoT;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SoTSubResource {
    private final IntelDAO intelDAO;
    private final Provider<IntelParserManager> intelParserManagerProvider;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<DelayedEventPoster> delayedEventPosterProvider;

    @Inject
    public SoTSubResource(final IntelDAO intelDAO,
                          final Provider<IntelParserManager> intelParserManagerProvider,
                          final Provider<KingdomDAO> kingdomDAOProvider,
                          final Provider<DelayedEventPoster> delayedEventPosterProvider) {
        this.intelDAO = intelDAO;
        this.intelParserManagerProvider = intelParserManagerProvider;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.delayedEventPosterProvider = delayedEventPosterProvider;
    }

    RS_SoT addSoT(final String newSoT, final WebContext webContext) throws Exception {
        Map<String, IntelParser<?>> parsers = intelParserManagerProvider.get().getParsers(newSoT);
        if (parsers.isEmpty()) throw new IllegalArgumentException("Data is not parsable");

        IntelParser<?> intelParser = parsers.values().iterator().next();
        if (!intelParser.getIntelTypeHandled().equals(SoT.class.getSimpleName()))
            throw new IllegalArgumentException("Data is not recognized as a SoT");

        Intel parsedSoT = null;
        try {
            parsedSoT = intelParser.parse(webContext.getName(), newSoT);
        } catch (ParseException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if (parsedSoT == null) throw new WebApplicationException(Response.Status.NOT_MODIFIED);
        intelDAO.saveIntel(parsedSoT, webContext.getBotUser().getId(), delayedEventPosterProvider.get());

        return RS_SoT.fromSoT((SoT) parsedSoT, false);
    }

    RS_SoT getSoT(final long id) {
        SoT sot = intelDAO.getSoT(id);

        if (sot == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_SoT.fromSoT(sot, true);
    }

    JResponse<List<RS_SoT>> getSoTs(final Long kingdomId) {
        checkNotNull(kingdomId, "No kingdom specified");

        List<RS_SoT> sots = new ArrayList<>();

        Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
        checkNotNull(kingdom, "No such kingdom");
        List<SoT> soTsForKD = intelDAO.getSoTsForKD(kingdom.getLocation());

        for (SoT sot : soTsForKD) {
            sots.add(RS_SoT.fromSoT(sot, true));
        }

        return JResponse.ok(sots).build();
    }

    void deleteSoT(final long id) {
        SoT sot = intelDAO.getSoT(id);
        if (sot == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        intelDAO.deleteIntel(sot);
    }
}
