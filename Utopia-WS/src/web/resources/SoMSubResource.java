package web.resources;

import api.events.DelayedEventPoster;
import api.timers.TimerManager;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.ArmyDAO;
import database.daos.IntelDAO;
import database.daos.KingdomDAO;
import database.models.Army;
import database.models.Kingdom;
import database.models.SoM;
import intel.Intel;
import intel.IntelParser;
import intel.IntelParserManager;
import web.models.RS_SoM;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SoMSubResource {
    private final IntelDAO intelDAO;
    private final Provider<IntelParserManager> intelParserManagerProvider;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<ArmyDAO> armyDAOProvider;
    private final Provider<DelayedEventPoster> delayedEventPosterProvider;
    private final Provider<TimerManager> timerManagerProvider;

    @Inject
    public SoMSubResource(final IntelDAO intelDAO,
                          final Provider<IntelParserManager> intelParserManagerProvider,
                          final Provider<KingdomDAO> kingdomDAOProvider,
                          final Provider<ArmyDAO> armyDAOProvider,
                          final Provider<DelayedEventPoster> delayedEventPosterProvider,
                          final Provider<TimerManager> timerManagerProvider) {
        this.intelDAO = intelDAO;
        this.intelParserManagerProvider = intelParserManagerProvider;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.armyDAOProvider = armyDAOProvider;
        this.delayedEventPosterProvider = delayedEventPosterProvider;
        this.timerManagerProvider = timerManagerProvider;
    }

    RS_SoM addSoM(final String newSoM, final WebContext webContext) throws Exception {
        Map<String, IntelParser<?>> parsers = intelParserManagerProvider.get().getParsers(newSoM);
        if (parsers.isEmpty()) throw new IllegalArgumentException("Data is not parsable");

        IntelParser<?> intelParser = parsers.values().iterator().next();
        if (!intelParser.getIntelTypeHandled().equals(SoM.class.getSimpleName()))
            throw new IllegalArgumentException("Data is not recognized as a SoM");

        Intel parsedSoM = intelParser.parse(webContext.getName(), newSoM);
        intelDAO.saveIntel(parsedSoM, webContext.getBotUser().getId(), delayedEventPosterProvider.get());

        return RS_SoM.fromSoM((SoM) parsedSoM, false);
    }

    RS_SoM getSoM(final long id) {
        SoM som = intelDAO.getSoM(id);

        if (som == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_SoM.fromSoM(som, true);
    }

    JResponse<List<RS_SoM>> getSoMs(final Long kingdomId) {
        checkNotNull(kingdomId, "No kingdom specified");

        List<RS_SoM> soms = new ArrayList<>();

        Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
        checkNotNull(kingdom, "No such kingdom");
        List<SoM> soMsForKD = intelDAO.getSoMsForKD(kingdom.getLocation());

        for (SoM som : soMsForKD) {
            soms.add(RS_SoM.fromSoM(som, true));
        }

        return JResponse.ok(soms).build();
    }

    void deleteSoM(final long id) {
        SoM som = intelDAO.getSoM(id);
        if (som == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        armyDAOProvider.get().delete(som.getArmies());
        for (Army army : som.getArmies()) {
            timerManagerProvider.get().cancelTimer(Army.class, army.getId());
        }

        intelDAO.deleteIntel(som);
    }
}
