package web.resources;

import api.database.Transactional;
import api.tools.validation.ValidationEnabled;
import com.sun.jersey.api.JResponse;
import database.daos.RaceDAO;
import database.models.Race;
import web.models.RS_Race;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ValidationEnabled
@Path("races")
public class RaceResource {
    private final RaceDAO raceDAO;

    @Inject
    public RaceResource(final RaceDAO raceDAO) {
        this.raceDAO = raceDAO;
    }

    /**
     * @param id the id of the race
     * @return the race with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Race getRace(@PathParam("id") final long id) {
        Race race = raceDAO.getRace(id);

        if (race == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Race.fromRace(race, true);
    }

    /**
     * Returns all races
     *
     * @return a list of races
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Race>> getRaces() {
        List<RS_Race> races = new ArrayList<>();

        Collection<Race> allRaces = raceDAO.getAllRaces();
        for (Race race : allRaces) {
            races.add(RS_Race.fromRace(race, true));
        }

        return JResponse.ok(races).build();
    }
}
