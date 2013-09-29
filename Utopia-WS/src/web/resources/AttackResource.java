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

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.AttackDAO;
import database.daos.ProvinceDAO;
import database.models.Attack;
import database.models.AttackType;
import database.models.Province;
import web.documentation.Documentation;
import web.models.RS_Attack;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static api.tools.collections.CollectionUtil.isNotEmpty;
import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkNotNull;

@ValidationEnabled
@Path("attacks")
public class AttackResource {
    private final AttackDAO attackDAO;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public AttackResource(final AttackDAO attackDAO,
                          final Provider<ProvinceDAO> provinceDAOProvider,
                          final Provider<BotUserDAO> botUserDAOProvider,
                          final Provider<Validator> validatorProvider) {
        this.attackDAO = attackDAO;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds an attack and returns the saved object")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Attack addAttack(@Documentation(value = "The attack to add", itemName = "newAttack")
                               @Valid final RS_Attack newAttack) {
        ProvinceDAO provinceDAO = provinceDAOProvider.get();
        Province attacker = provinceDAO.getProvince(newAttack.getAttacker().getId());
        Province defender = provinceDAO.getProvince(newAttack.getDefender().getId());

        Attack attack = new Attack(attacker, defender, AttackType.fromName(newAttack.getType()), new Date());
        RS_Attack.toAttack(attack, newAttack);
        attack = attackDAO.save(attack);

        return RS_Attack.fromAttack(attack);
    }

    @Documentation("Returns the attack with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Attack getAttack(@PathParam("id") final long id) {
        Attack attack = attackDAO.getAttack(id);

        if (attack == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Attack.fromAttack(attack);
    }

    @Documentation("Fetches attacks. Which ones are fetched depends on what query params are specified. It goes through the query params" +
            "in the order described below, and if one is defined, that one is used and the rest are ignored." +
            "<p/>" +
            "Params resolve order:" +
            "<ol>" +
            "<li>attackIds</li>" +
            "<li>provinceId</li>" +
            "<li>userId</li>" +
            "</ol>" +
            "<p/>" +
            "maxIncoming and maxOutgoing may be used to control what type of attacks to return, not just limit the lists. For example, " +
            "provided you only want the received attacks of a province, you could set maxOutgoing to 0 (and vice versa).")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Attack>> getAttacks(@Documentation("The id's of the attacks to return. May not contain null values")
                                                 @QueryParam("attackIds")
                                                 final List<Long> attackIds,
                                                 @Documentation("The id of a province for which to fetch attacks")
                                                 @QueryParam("provinceId")
                                                 final Long provinceId,
                                                 @Documentation("The id of a user for which to fetch attacks")
                                                 @QueryParam("userId")
                                                 final Long userId,
                                                 @Documentation("Used together with specifying a province or a user to decide how many of the received hits " +
                                                         "to list. Defaults to 10.")
                                                 @QueryParam("maxIncoming") @DefaultValue("10")
                                                 final int maxIncoming,
                                                 @Documentation("The same as maxIncoming, but for hits made instead. Defaults to 10")
                                                 @QueryParam("maxOutgoing") @DefaultValue("10")
                                                 final int maxOutgoing) {
        List<RS_Attack> attacks = new ArrayList<>();
        if (isNotEmpty(attackIds)) {
            Long[] attackIdsArray = attackIds.toArray(new Long[attackIds.size()]);
            for (Attack attack : attackDAO.getAttacks(attackIdsArray)) {
                attacks.add(RS_Attack.fromAttack(attack));
            }
        } else if (provinceId != null) {
            ProvinceDAO provinceDAO = provinceDAOProvider.get();
            Province dbProvince = provinceDAO.getProvince(provinceId);
            checkNotNull(dbProvince, "No such province");

            for (Attack attack : attackDAO.getLastHitsReceived(dbProvince, maxIncoming)) {
                attacks.add(RS_Attack.fromAttack(attack));
            }
            for (Attack attack : attackDAO.getLastHitsMade(dbProvince, maxOutgoing)) {
                attacks.add(RS_Attack.fromAttack(attack));
            }
        } else if (userId != null) {
            BotUserDAO userDAO = botUserDAOProvider.get();
            BotUser dbUser = userDAO.getUser(userId);
            checkNotNull(dbUser, "No such user");

            ProvinceDAO provinceDAO = provinceDAOProvider.get();
            Province userProvince = provinceDAO.getProvinceForUser(dbUser);
            checkNotNull(userProvince, "That user has no province");

            for (Attack attack : attackDAO.getLastHitsReceived(userProvince, maxIncoming)) {
                attacks.add(RS_Attack.fromAttack(attack));
            }
            for (Attack attack : attackDAO.getLastHitsMade(userProvince, maxOutgoing)) {
                attacks.add(RS_Attack.fromAttack(attack));
            }
        } else {
            for (Attack attack : attackDAO.getAllAttacks()) {
                attacks.add(RS_Attack.fromAttack(attack));
            }
        }
        return JResponse.ok(attacks).build();
    }

    @Documentation("Updates the specified attack and returns the updated object")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Attack updateAttack(@PathParam("id") final long id,
                                  @Documentation(value = "The updated attack", itemName = "updatedAttack")
                                  final RS_Attack updatedAttack) {
        Attack attack = attackDAO.getAttack(id);
        if (attack == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedAttack).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        RS_Attack.toAttack(attack, updatedAttack);

        return RS_Attack.fromAttack(attack);
    }

    @Documentation("Deletes the attack with the specified id")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteAttack(@PathParam("id") final long id) {
        Attack attack = attackDAO.getAttack(id);

        if (attack == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        attackDAO.delete(attack);
    }
}
