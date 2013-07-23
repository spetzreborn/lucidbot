package web.resources;

import api.events.DelayedEventPoster;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.IntelDAO;
import database.daos.KingdomDAO;
import database.models.Kingdom;
import database.models.Survey;
import database.models.SurveyEntry;
import intel.Intel;
import intel.IntelParser;
import intel.IntelParserManager;
import web.models.RS_Survey;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SurveySubResource {
    private final IntelDAO intelDAO;
    private final Provider<IntelParserManager> intelParserManagerProvider;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<DelayedEventPoster> delayedEventPosterProvider;

    @Inject
    public SurveySubResource(final IntelDAO intelDAO,
                             final Provider<IntelParserManager> intelParserManagerProvider,
                             final Provider<KingdomDAO> kingdomDAOProvider,
                             final Provider<DelayedEventPoster> delayedEventPosterProvider) {
        this.intelDAO = intelDAO;
        this.intelParserManagerProvider = intelParserManagerProvider;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.delayedEventPosterProvider = delayedEventPosterProvider;
    }

    RS_Survey addSurvey(final String newSurvey, final WebContext webContext) throws Exception {
        Map<String, IntelParser<?>> parsers = intelParserManagerProvider.get().getParsers(newSurvey);
        if (parsers.isEmpty()) throw new IllegalArgumentException("Data is not parsable");

        IntelParser<?> intelParser = parsers.get(0);
        if (!intelParser.getIntelTypeHandled().equals(Survey.class.getSimpleName()))
            throw new IllegalArgumentException("Data is not recognized as a Survey");

        Intel parsedSurvey = intelParser.parse(webContext.getName(), newSurvey);
        intelDAO.saveIntel(parsedSurvey, webContext.getBotUser().getId(), delayedEventPosterProvider.get());

        return RS_Survey.fromSurvey((Survey) parsedSurvey, true);
    }

    RS_Survey getSurvey(final long id) {
        Survey survey = intelDAO.getSurvey(id);

        if (survey == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Survey.fromSurvey(survey, true);
    }

    JResponse<List<RS_Survey>> getSurveys(final Long kingdomId) {
        checkNotNull(kingdomId, "No kingdom specified");

        List<RS_Survey> surveys = new ArrayList<>();

        Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
        checkNotNull(kingdom, "No such kingdom");
        List<Survey> surveysForKd = intelDAO.getSurveysForKD(kingdom.getLocation());

        for (Survey sos : surveysForKd) {
            surveys.add(RS_Survey.fromSurvey(sos, true));
        }

        return JResponse.ok(surveys).build();
    }

    void deleteSurvey(final long id) {
        Survey survey = intelDAO.getSurvey(id);
        if (survey == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        for (SurveyEntry entry : survey.getBuildings()) {
            intelDAO.deleteIntelRelatedObject(entry);
        }

        intelDAO.deleteIntel(survey);
    }
}
