package web.resources;

import api.database.Transactional;
import api.tools.validation.ValidationEnabled;
import com.sun.jersey.api.JResponse;
import org.hibernate.validator.constraints.NotEmpty;
import web.documentation.Documentation;
import web.models.RS_SoM;
import web.models.RS_SoS;
import web.models.RS_SoT;
import web.models.RS_Survey;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

@ValidationEnabled
@Path("intel")
public class IntelResource {
    private final Provider<SoMSubResource> soMSubResourceProvider;
    private final Provider<SoSSubResource> soSSubResourceProvider;
    private final Provider<SoTSubResource> soTSubResourceProvider;
    private final Provider<SurveySubResource> surveySubResourceProvider;

    @Inject
    public IntelResource(final Provider<SoMSubResource> soMSubResourceProvider,
                         final Provider<SoSSubResource> soSSubResourceProvider,
                         final Provider<SoTSubResource> soTSubResourceProvider,
                         final Provider<SurveySubResource> surveySubResourceProvider) {
        this.soMSubResourceProvider = soMSubResourceProvider;
        this.soSSubResourceProvider = soSSubResourceProvider;
        this.soTSubResourceProvider = soTSubResourceProvider;
        this.surveySubResourceProvider = surveySubResourceProvider;
    }

    @Documentation("Parses the incoming text and returns the saved SoM")
    @Path("soms")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    @Transactional
    public RS_SoM addSoM(@Documentation(value = "The formatted or unformatted SoM", itemName = "newSoM")
                         @NotEmpty(message = "The intel may not be null or empty")
                         final String newSoM,
                         @Context final WebContext webContext) throws Exception {
        return soMSubResourceProvider.get().addSoM(newSoM, webContext);
    }

    @Documentation("Returns the SoM with the specified id")
    @Path("soms/{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_SoM getSoM(@PathParam("id") final long id) {
        return soMSubResourceProvider.get().getSoM(id);
    }

    @Documentation("Returns all the SoM's for the specified kingdom (see the query parameter, it's mandatory)")
    @Path("soms")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_SoM>> getSoMs(@Documentation("The id of the kingdom to limit the results to")
                                           @QueryParam("kingdomId")
                                           final Long kingdomId) {
        return soMSubResourceProvider.get().getSoMs(kingdomId);
    }

    @Documentation("Deletes the specified SoM and removes the appropriate army timers")
    @Path("soms/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteSoM(@PathParam("id") final long id) {
        soMSubResourceProvider.get().deleteSoM(id);
    }

    @Documentation("Parses the incoming text and returns the saved SoS")
    @Path("soss")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    @Transactional
    public RS_SoS addSoS(@Documentation(value = "The formatted or unformatted SoS", itemName = "newSoS")
                         @NotEmpty(message = "The intel may not be null or empty")
                         final String newSoS,
                         @Context final WebContext webContext) throws Exception {
        return soSSubResourceProvider.get().addSoS(newSoS, webContext);
    }

    @Documentation("Returns the SoS with the specified id")
    @Path("soss/{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_SoS getSoS(@PathParam("id") final long id) {
        return soSSubResourceProvider.get().getSos(id);
    }

    @Documentation("Returns all the SoS's for the specified kingdom (see the query parameter, it's mandatory)")
    @Path("soss")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_SoS>> getSoSs(@Documentation("The id of the kingdom to limit the results to")
                                           @QueryParam("kingdomId")
                                           final Long kingdomId) {
        return soSSubResourceProvider.get().getSoSs(kingdomId);
    }

    @Documentation("Deletes the specified SoS and removes the appropriate army timers")
    @Path("soss/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteSoS(@PathParam("id") final long id) {
        soSSubResourceProvider.get().deleteSoS(id);
    }

    @Documentation("Parses the incoming text and returns the saved SoT")
    @Path("sots")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    @Transactional
    public RS_SoT addSoT(@Documentation(value = "The formatted or unformatted SoT", itemName = "newSoT")
                         @NotEmpty(message = "The intel may not be null or empty")
                         final String newSoT,
                         @Context final WebContext webContext) throws Exception {
        return soTSubResourceProvider.get().addSoT(newSoT, webContext);
    }

    @Documentation("Returns the SoT with the specified id")
    @Path("sots/{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_SoT getSoT(@PathParam("id") final long id) {
        return soTSubResourceProvider.get().getSoT(id);
    }

    @Documentation("Returns all the SoT's for the specified kingdom (see the query parameter, it's mandatory)")
    @Path("sots")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_SoT>> getSoTs(@Documentation("The id of the kingdom to limit the results to")
                                           @QueryParam("kingdomId")
                                           final Long kingdomId) {
        return soTSubResourceProvider.get().getSoTs(kingdomId);
    }

    @Documentation("Deletes the specified SoT and removes the appropriate army timers")
    @Path("sots/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteSoT(@PathParam("id") final long id) {
        soTSubResourceProvider.get().deleteSoT(id);
    }

    @Documentation("Parses the incoming text and returns the saved Survey")
    @Path("surveys")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.TEXT_PLAIN})
    @Transactional
    public RS_Survey addSurvey(@Documentation(value = "The formatted or unformatted Survey", itemName = "newSurvey")
                               @NotEmpty(message = "The intel may not be null or empty")
                               final String newSurvey,
                               @Context final WebContext webContext) throws Exception {
        return surveySubResourceProvider.get().addSurvey(newSurvey, webContext);
    }

    @Documentation("Returns the Survey with the specified id")
    @Path("surveys/{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Survey getSurvey(@PathParam("id") final long id) {
        return surveySubResourceProvider.get().getSurvey(id);
    }

    @Documentation("Returns all the Survey's for the specified kingdom (see the query parameter, it's mandatory)")
    @Path("surveys")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Survey>> getSurveys(@Documentation("The id of the kingdom to limit the results to")
                                                 @QueryParam("kingdomId")
                                                 final Long kingdomId) {
        return surveySubResourceProvider.get().getSurveys(kingdomId);
    }

    @Documentation("Deletes the specified Survevy and removes the appropriate army timers")
    @Path("surveys/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteSurvey(@PathParam("id") final long id) {
        surveySubResourceProvider.get().deleteSurvey(id);
    }

}
