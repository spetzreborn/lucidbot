package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.KingdomDAO;
import database.daos.PersonalityDAO;
import database.daos.ProvinceDAO;
import database.daos.RaceDAO;
import database.models.Kingdom;
import database.models.Personality;
import database.models.Province;
import database.models.Race;
import tools.validation.ExistsInDB;
import web.models.RS_Province;
import web.models.RS_User;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static api.tools.collections.CollectionUtil.isEmpty;
import static com.google.common.base.Preconditions.checkNotNull;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("provinces")
public class ProvinceResource {
    private final ProvinceDAO provinceDAO;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<RaceDAO> raceDAOProvider;
    private final Provider<PersonalityDAO> personalityDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;

    @Inject
    public ProvinceResource(final ProvinceDAO provinceDAO,
                            final Provider<KingdomDAO> kingdomDAOProvider,
                            final Provider<RaceDAO> raceDAOProvider,
                            final Provider<PersonalityDAO> personalityDAOProvider,
                            final Provider<BotUserDAO> botUserDAOProvider) {
        this.provinceDAO = provinceDAO;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.raceDAOProvider = raceDAOProvider;
        this.personalityDAOProvider = personalityDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;
    }

    /**
     * Adds a province
     *
     * @param newProvince the province to add
     * @return the added province
     */
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Province addProvince(@Valid final RS_Province newProvince) {
        Province existing = provinceDAO.getProvince(newProvince.getName());
        if (existing != null) throw new IllegalArgumentException("A province with that name already exists");

        Kingdom kingdom = kingdomDAOProvider.get().getKingdom(newProvince.getKingdom().getId());

        Race race = null;
        if (newProvince.getRace() != null && newProvince.getRace().getId() != null)
            race = raceDAOProvider.get().getRace(newProvince.getRace().getId());
        Personality personality = null;
        if (newProvince.getPersonality() != null && newProvince.getPersonality().getId() != null)
            personality = personalityDAOProvider.get().getPersonality(newProvince.getPersonality().getId());
        BotUser owner = null;
        if (newProvince.getOwner() != null && newProvince.getOwner().getId() != null)
            owner = botUserDAOProvider.get().getUser(newProvince.getOwner().getId());

        Province province = new Province(newProvince.getName(), kingdom, race, personality, owner);
        province = provinceDAO.save(province);
        return RS_Province.fromProvince(province, true);
    }

    /**
     * @param id the id of the province
     * @return the province with the specified id
     */
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Province getProvince(@PathParam("id") final long id) {
        Province province = provinceDAO.getProvince(id);

        if (province == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Province.fromProvince(province, true);
    }

    /**
     * Returns provinces for the specified users, or for the specified kd
     *
     * @param userIds   the ids of the users you want to get the provinces for
     * @param kingdomId the id of the kingdom you want to get the provinces for
     * @return a list of provinces
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Province>> getProvinces(@QueryParam("userIds") final List<Long> userIds,
                                                     @QueryParam("kingdomId") final Long kingdomId) {
        List<RS_Province> provinces = new ArrayList<>();
        if (isEmpty(userIds)) {
            if (kingdomId != null) {
                Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
                checkNotNull(kingdom, "No such kingdom");
                for (Province province : kingdom.getProvinces()) {
                    provinces.add(RS_Province.fromProvince(province, true));
                }
            } else
                throw new IllegalArgumentException("You must specify either a kingdom or some users to get the provinces for");
        } else {
            BotUserDAO botUserDAO = botUserDAOProvider.get();
            for (Long userId : userIds) {
                BotUser user = botUserDAO.getUser(userId);
                checkNotNull(user, "No such user");
                Province provinceForUser = provinceDAO.getProvinceForUser(user);
                if (provinceForUser != null) provinces.add(RS_Province.fromProvince(provinceForUser, true));
            }
        }
        return JResponse.ok(provinces).build();
    }

    /**
     * Sets the owner for a province
     *
     * @param id            the id of the province to update
     * @param provinceOwner the owner
     * @return the updated province
     */
    @Path("{id : \\d+}/owner")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Province setProvinceOwner(@PathParam("id") final long id,
                                        @ExistsInDB(entity = BotUser.class, message = "No such user")
                                        final RS_User provinceOwner,
                                        @Context final WebContext webContext) {
        Province province = provinceDAO.getProvince(id);
        if (province == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        if (!webContext.isInRole(ADMIN_ROLE))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        BotUser owner = botUserDAOProvider.get().getUser(provinceOwner.getId());

        Province existingProvince = provinceDAO.getProvinceForUser(owner);
        existingProvince.setOwner(null);

        province.setProvinceOwner(owner);
        return RS_Province.fromProvince(province, true);
    }

    /**
     * Deletes a province
     *
     * @param id the id of the province
     */
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteProvince(@PathParam("id") final long id, @Context final WebContext webContext) {
        Province province = provinceDAO.getProvince(id);
        if (province == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        if (!webContext.isInRole(ADMIN_ROLE))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        provinceDAO.delete(province);
    }
}
