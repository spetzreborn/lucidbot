package web.resources;

import api.events.DelayedEventPoster;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.IntelDAO;
import database.daos.KingdomDAO;
import database.models.Kingdom;
import database.models.SoS;
import database.models.SoSEntry;
import intel.Intel;
import intel.IntelParser;
import intel.IntelParserManager;
import web.models.RS_SoS;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SoSSubResource {
    private final IntelDAO intelDAO;
    private final Provider<IntelParserManager> intelParserManagerProvider;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<DelayedEventPoster> delayedEventPosterProvider;

    @Inject
    public SoSSubResource(final IntelDAO intelDAO,
                          final Provider<IntelParserManager> intelParserManagerProvider,
                          final Provider<KingdomDAO> kingdomDAOProvider,
                          final Provider<DelayedEventPoster> delayedEventPosterProvider) {
        this.intelDAO = intelDAO;
        this.intelParserManagerProvider = intelParserManagerProvider;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.delayedEventPosterProvider = delayedEventPosterProvider;
    }

    RS_SoS addSoS(final String newSos, final WebContext webContext) throws Exception {
        Map<String, IntelParser<?>> parsers = intelParserManagerProvider.get().getParsers(newSos);
        if (parsers.isEmpty()) throw new IllegalArgumentException("Data is not parsable");

        IntelParser<?> intelParser = parsers.values().iterator().next();
        if (!intelParser.getIntelTypeHandled().equals(SoS.class.getSimpleName()))
            throw new IllegalArgumentException("Data is not recognized as a SoS");

        Intel parsedSoS = intelParser.parse(webContext.getName(), newSos);
        intelDAO.saveIntel(parsedSoS, webContext.getBotUser().getId(), delayedEventPosterProvider.get());

        return RS_SoS.fromSoS((SoS) parsedSoS, false);
    }

    RS_SoS getSos(final long id) {
        SoS sos = intelDAO.getSoS(id);

        if (sos == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_SoS.fromSoS(sos, true);
    }

    JResponse<List<RS_SoS>> getSoSs(final Long kingdomId) {
        checkNotNull(kingdomId, "No kingdom specified");

        List<RS_SoS> soss = new ArrayList<>();

        Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
        checkNotNull(kingdom, "No such kingdom");
        List<SoS> sossForKD = intelDAO.getSoSsForKD(kingdom.getLocation());

        for (SoS sos : sossForKD) {
            soss.add(RS_SoS.fromSoS(sos, true));
        }

        return JResponse.ok(soss).build();
    }

    void deleteSoS(final long id) {
        SoS sos = intelDAO.getSoS(id);
        if (sos == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        for (SoSEntry entry : sos.getSciences()) {
            intelDAO.deleteIntelRelatedObject(entry);
        }

        intelDAO.deleteIntel(sos);
    }
}
