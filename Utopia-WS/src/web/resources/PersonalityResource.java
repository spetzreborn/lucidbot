package web.resources;

import api.database.Transactional;
import api.tools.validation.ValidationEnabled;
import com.sun.jersey.api.JResponse;
import database.daos.PersonalityDAO;
import database.models.Personality;
import web.models.RS_Personality;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ValidationEnabled
@Path("personalities")
public class PersonalityResource {
    private final PersonalityDAO personalityDAO;

    @Inject
    public PersonalityResource(final PersonalityDAO personalityDAO) {
        this.personalityDAO = personalityDAO;
    }

    /**
     * @param id the id of the personality
     * @return the personality with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Personality getPersonality(@PathParam("id") final long id) {
        Personality personality = personalityDAO.getPersonality(id);

        if (personality == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Personality.fromPersonality(personality, true);
    }

    /**
     * Returns all personalities
     *
     * @return a list of personalities
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Personality>> getPersonalities() {
        List<RS_Personality> personalities = new ArrayList<>();

        Collection<Personality> allPersonalities = personalityDAO.getAllPersonalities();
        for (Personality personality : allPersonalities) {
            personalities.add(RS_Personality.fromPersonality(personality, true));
        }

        return JResponse.ok(personalities).build();
    }
}
