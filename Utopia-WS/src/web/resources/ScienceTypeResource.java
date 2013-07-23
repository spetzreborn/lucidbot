package web.resources;

import api.database.Transactional;
import api.tools.validation.ValidationEnabled;
import com.sun.jersey.api.JResponse;
import database.daos.ScienceTypeDAO;
import database.models.ScienceType;
import web.models.RS_ScienceType;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ValidationEnabled
@Path("sciences")
public class ScienceTypeResource {
    private final ScienceTypeDAO scienceTypeDAO;

    @Inject
    public ScienceTypeResource(final ScienceTypeDAO scienceTypeDAO) {
        this.scienceTypeDAO = scienceTypeDAO;
    }

    /**
     * @param id the id of the science type
     * @return the science type with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_ScienceType getScienceType(@PathParam("id") final long id) {
        ScienceType scienceType = scienceTypeDAO.getScienceType(id);

        if (scienceType == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_ScienceType.fromScienceType(scienceType, true);
    }

    /**
     * Returns all science types
     *
     * @return a list of science types
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_ScienceType>> getScienceTypes() {
        List<RS_ScienceType> scienceTypes = new ArrayList<>();

        Collection<ScienceType> allScienceTypes = scienceTypeDAO.getAllScienceTypes();
        for (ScienceType scienceType : allScienceTypes) {
            scienceTypes.add(RS_ScienceType.fromScienceType(scienceType, true));
        }

        return JResponse.ok(scienceTypes).build();
    }
}
