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
import web.documentation.Documentation;
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

import static api.tools.collections.CollectionUtil.isNotEmpty;
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

    @Documentation("Adds a new province and returns the saved object. Currently only supports specifying basic data (name, kingdom, race, " +
            "personality and owner. The rest will be ignored)")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Province addProvince(@Documentation(value = "The province to add", itemName = "newProvince")
                                   @Valid final RS_Province newProvince) {
        Province existing = provinceDAO.getProvince(newProvince.getName());
        if (existing != null) throw new IllegalArgumentException("A province with that name already exists");

        Kingdom kingdom = kingdomDAOProvider.get().getKingdom(newProvince.getKingdom().getId());

        Race race = null;
        if (newProvince.getRace() != null)
            race = raceDAOProvider.get().getRace(newProvince.getRace().getId());
        Personality personality = null;
        if (newProvince.getPersonality() != null)
            personality = personalityDAOProvider.get().getPersonality(newProvince.getPersonality().getId());
        BotUser owner = null;
        if (newProvince.getOwner() != null)
            owner = botUserDAOProvider.get().getUser(newProvince.getOwner().getId());

        Province province = new Province(newProvince.getName(), kingdom, race, personality, owner);
        province = provinceDAO.save(province);
        return RS_Province.fromProvince(province, true);
    }

    @Documentation("Returns the province with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Province getProvince(@PathParam("id") final long id) {
        Province province = provinceDAO.getProvince(id);

        if (province == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Province.fromProvince(province, true);
    }

    @Documentation("Returns all provinces, or optionally for either a kingdom or a group of users. If none of the filtering " +
            "parameters are used and all provinces are to be returned, they will have minimum content to keep the data size down")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Province>> getProvinces(@Documentation("The id's of the users to get provinces for")
                                                     @QueryParam("userIds")
                                                     final List<Long> userIds,
                                                     @Documentation("The id of the kingdom to get provinces for")
                                                     @QueryParam("kingdomId")
                                                     final Long kingdomId) {
        List<RS_Province> provinces = new ArrayList<>();
        if (kingdomId != null) {
            Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
            checkNotNull(kingdom, "No such kingdom");
            for (Province province : kingdom.getProvinces()) {
                provinces.add(RS_Province.fromProvince(province, true));
            }
        } else if (isNotEmpty(userIds)) {
            BotUserDAO botUserDAO = botUserDAOProvider.get();
            for (Long userId : userIds) {
                BotUser user = botUserDAO.getUser(userId);
                checkNotNull(user, "No such user");
                Province provinceForUser = provinceDAO.getProvinceForUser(user);
                if (provinceForUser != null) provinces.add(RS_Province.fromProvince(provinceForUser, true));
            }
        } else {
            for (Province province : provinceDAO.getAllProvinces()) {
                provinces.add(RS_Province.fromProvince(province, false));
            }
        }
        return JResponse.ok(provinces).build();
    }

    @Documentation("Sets the owner for the specified province and returns the updated object. Admin only request")
    @Path("{id : \\d+}/owner")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Province setProvinceOwner(@PathParam("id") final long id,
                                        @Documentation(value = "The user to set as owner")
                                        @ExistsInDB(entity = BotUser.class, message = "No such user")
                                        final RS_User provinceOwner,
                                        @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Province province = provinceDAO.getProvince(id);
        if (province == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        BotUser owner = botUserDAOProvider.get().getUser(provinceOwner.getId());

        Province existingProvince = provinceDAO.getProvinceForUser(owner);
        existingProvince.setOwner(null);

        province.setProvinceOwner(owner);
        return RS_Province.fromProvince(province, true);
    }

    @Documentation("Removes the owner from the specified province and returns the updated object. Admin only request")
    @Path("{id : \\d+}/owner")
    @DELETE
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Province deleteProvinceOwner(@PathParam("id") final long id,
                                           @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Province province = provinceDAO.getProvince(id);
        if (province == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        province.setProvinceOwner(null);
        return RS_Province.fromProvince(province, true);
    }


    @Documentation("Deletes the specified province and all it's intel and such. Admin only request")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteProvince(@PathParam("id") final long id, @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Province province = provinceDAO.getProvince(id);
        if (province == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        provinceDAO.delete(province);
    }
}
