/*
 * Copyright (c) 2012, Fredrik Yttergren
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name LucidBot nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Fredrik Yttergren BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.timers.Timer;
import api.timers.TimerManager;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.ArmyDAO;
import database.daos.KingdomDAO;
import database.daos.ProvinceDAO;
import database.models.Army;
import database.models.Kingdom;
import database.models.Province;
import events.ArmyAddedEvent;
import listeners.ArmyManager;
import web.documentation.Documentation;
import web.models.RS_Army;
import web.tools.AfterCommitEventPoster;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static api.tools.collections.CollectionUtil.isNotEmpty;
import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkNotNull;

@Path("armies")
public class ArmyResource {
    private final ArmyDAO armyDAO;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider;
    private final Provider<TimerManager> timerManagerProvider;
    private final Provider<ArmyManager> armyManagerProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public ArmyResource(final ArmyDAO armyDAO,
                        final Provider<KingdomDAO> kingdomDAOProvider,
                        final Provider<ProvinceDAO> provinceDAOProvider,
                        final Provider<BotUserDAO> userDAOProvider,
                        final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider,
                        final Provider<TimerManager> timerManagerProvider,
                        final Provider<ArmyManager> armyManagerProvider,
                        final Provider<Validator> validatorProvider) {
        this.armyDAO = armyDAO;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
        this.userDAOProvider = userDAOProvider;
        this.afterCommitEventPosterProvider = afterCommitEventPosterProvider;
        this.timerManagerProvider = timerManagerProvider;
        this.armyManagerProvider = armyManagerProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds a new army provided none of the same type and with the same army number already exists. " +
            "Automatically publishes an ArmyAddedEvent on success and adds the appropriate timer. Returns the saved object")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Army addArmy(@Documentation(value = "The army to add", itemName = "newArmy")
                           @Valid final RS_Army newArmy) {
        Province province = provinceDAOProvider.get().getProvince(newArmy.getProvince().getId());

        Army army = new Army(province, newArmy.getArmyNumber(), Army.ArmyType.fromName(newArmy.getType()), newArmy.getReturning(), newArmy.getGain());
        RS_Army.toArmy(army, newArmy);

        for (Army existing : province.getArmies()) {
            if (army.getType() == existing.getType() && army.getArmyNumber() == existing.getArmyNumber()) {
                throw new IllegalArgumentException("An army with that armynumber and of that type already exists. Delete it first");
            }
        }

        army = armyDAO.save(army);

        long delay = army.getReturningDate().getTime() - System.currentTimeMillis();
        timerManagerProvider.get().schedule(new Timer(Army.class, army.getId(), armyManagerProvider.get()), delay, TimeUnit.MILLISECONDS);
        afterCommitEventPosterProvider.get().addEventToPost(new ArmyAddedEvent(army.getId(), null));

        return RS_Army.fromArmy(army, true);
    }

    @Documentation("Returns the army with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Army getArmy(@PathParam("id") final long id) {
        Army army = armyDAO.getArmy(id);

        if (army == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Army.fromArmy(army, true);
    }

    @Documentation("Fetches armies. Which ones are fetched depends on what query params are specified. It goes through the query params " +
            "in the order described below, and if one is defined, that one is used and the rest are ignored. Returns all types of armies " +
            "(home, training or out from intel as well as the irc armies). " +
            "<p/> " +
            "Params resolve order: " +
            "<ol> " +
            "<li>armyIds</li> " +
            "<li>kingdomId</li> " +
            "<li>provinceId</li> " +
            "<li>userId</li> " +
            "<li>If none of the above, return all armies in the database</li> " +
            "</ol>")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Army>> getArmies(@Documentation("The id's of the armies to return. May not contain null values")
                                              @QueryParam("armyIds")
                                              final List<Long> armyIds,
                                              @Documentation("The id of a kd for which to fetch all armies")
                                              @QueryParam("kingdomId")
                                              final Long kingdomId,
                                              @Documentation("The id of a province for which to fetch all armies")
                                              @QueryParam("provinceId")
                                              final Long provinceId,
                                              @Documentation("The id of a user for which to fetch all armies")
                                              @QueryParam("userId")
                                              final Long userId) {
        List<RS_Army> armies = new ArrayList<>();
        if (isNotEmpty(armyIds)) {
            Long[] armyIdsArray = armyIds.toArray(new Long[armyIds.size()]);
            for (Army army : armyDAO.getAllArmies(armyIdsArray)) {
                armies.add(RS_Army.fromArmy(army, true));
            }
        } else if (kingdomId != null) {
            KingdomDAO kingdomDAO = kingdomDAOProvider.get();
            Kingdom kingdom = kingdomDAO.getKingdom(kingdomId);
            checkNotNull(kingdom, "There's no such kingdom");
            for (Province province : kingdom.getProvinces()) {
                for (Army army : province.getArmies()) {
                    armies.add(RS_Army.fromArmy(army, true));
                }
            }
        } else if (provinceId != null) {
            ProvinceDAO provinceDAO = provinceDAOProvider.get();
            Province province = provinceDAO.getProvince(provinceId);
            checkNotNull(province, "There's no such province");
            for (Army army : province.getArmies()) {
                armies.add(RS_Army.fromArmy(army, true));
            }
        } else if (userId != null) {
            BotUserDAO userDAO = userDAOProvider.get();
            BotUser user = userDAO.getUser(userId);
            checkNotNull(user, "There's no such user");
            for (Army army : armyDAO.getAllArmiesForUser(user)) {
                armies.add(RS_Army.fromArmy(army, true));
            }
        } else {
            for (Army army : armyDAO.getAllArmies()) {
                armies.add(RS_Army.fromArmy(army, true));
            }
        }
        return JResponse.ok(armies).build();
    }

    @Documentation("Updates an army and returns the updated object. Updates the timer appropriately")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Army updateArmy(@PathParam("id") final long id,
                              @Documentation(value = "The updated army", itemName = "updatedArmy")
                              final RS_Army updatedArmy) {
        Army army = armyDAO.getArmy(id);
        if (army == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedArmy).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        if (updatedArmy.getReturning() != null && !updatedArmy.getReturning().equals(army.getReturningDate())) {
            TimerManager timerManager = timerManagerProvider.get();
            timerManager.cancelTimer(Army.class, army.getId());
            timerManager.schedule(new Timer(Army.class, army.getId(), armyManagerProvider.get()),
                    updatedArmy.getReturning().getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        } else if (updatedArmy.getReturning() == null && army.getReturningDate() != null) {
            timerManagerProvider.get().cancelTimer(Army.class, army.getId());
        }
        RS_Army.toArmy(army, updatedArmy);

        return RS_Army.fromArmy(army, true);
    }

    @Documentation("Deletes the specified army and its timer")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteArmy(@PathParam("id") final long id) {
        Army army = armyDAO.getArmy(id);

        if (army == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        armyDAO.delete(army);
        timerManagerProvider.get().cancelTimer(Army.class, army.getId());
    }
}
