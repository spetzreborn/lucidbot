package web.resources;

import api.database.Transactional;
import api.tools.validation.ValidationEnabled;
import com.sun.jersey.api.JResponse;
import database.daos.HonorTitleDAO;
import database.models.HonorTitle;
import web.documentation.Documentation;
import web.models.RS_HonorTitle;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ValidationEnabled
@Path("honortitles")
public class HonorTitleResource {
    private final HonorTitleDAO honorTitleDAO;

    @Inject
    public HonorTitleResource(final HonorTitleDAO honorTitleDAO) {
        this.honorTitleDAO = honorTitleDAO;
    }

    @Documentation("Returns the honor title with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_HonorTitle getHonorTitle(@PathParam("id") final long id) {
        HonorTitle honorTitle = honorTitleDAO.getHonorTitle(id);
        if (honorTitle == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_HonorTitle.fromHonorTitle(honorTitle, true);
    }

    @Documentation("Returns all honor titles")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_HonorTitle>> getHonorTitles() {
        List<RS_HonorTitle> honorTitles = new ArrayList<>();

        Collection<HonorTitle> allHonorTitles = honorTitleDAO.getAllHonorTitles();
        for (HonorTitle honorTitle : allHonorTitles) {
            honorTitles.add(RS_HonorTitle.fromHonorTitle(honorTitle, true));
        }

        return JResponse.ok(honorTitles).build();
    }
}
