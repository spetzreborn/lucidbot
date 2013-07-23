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
import api.timers.Timer;
import api.timers.TimerManager;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.AidDAO;
import database.daos.ProvinceDAO;
import database.models.Aid;
import database.models.AidType;
import database.models.Province;
import events.AidAddedEvent;
import listeners.AidManager;
import web.documentation.Documentation;
import web.models.RS_Aid;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static api.tools.validation.ValidationUtil.validate;

@ValidationEnabled
@Path("aid")
public class AidResource {
    private final AidDAO aidDAO;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<TimerManager> timerManagerProvider;
    private final Provider<AidManager> aidManagerProvider;
    private final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public AidResource(final AidDAO aidDAO,
                       final Provider<ProvinceDAO> provinceDAOProvider,
                       final Provider<TimerManager> timerManagerProvider,
                       final Provider<AidManager> aidManagerProvider,
                       final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider,
                       final Provider<Validator> validatorProvider) {
        this.aidDAO = aidDAO;
        this.provinceDAOProvider = provinceDAOProvider;
        this.timerManagerProvider = timerManagerProvider;
        this.aidManagerProvider = aidManagerProvider;
        this.afterCommitEventPosterProvider = afterCommitEventPosterProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds an aid request/offer and returns the saved object")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Aid addAid(@Documentation(value = "The aid to add", itemName = "newAid")
                         @Valid final RS_Aid newAid) {
        Province province = provinceDAOProvider.get().getProvince(newAid.getProvince().getId());

        Aid aid = new Aid(province, AidType.fromName(newAid.getType()));
        RS_Aid.toAid(aid, newAid);

        aid = aidDAO.save(aid);
        if (aid.getExpiryDate() != null) {
            long delay = aid.getExpiryDate().getTime() - System.currentTimeMillis();
            timerManagerProvider.get().schedule(new Timer(Aid.class, aid.getId(), aidManagerProvider.get()), delay, TimeUnit.MILLISECONDS);
        }
        afterCommitEventPosterProvider.get().addEventToPost(new AidAddedEvent(aid.getId(), null));

        return RS_Aid.fromAid(aid);
    }

    @Documentation("Returns the aid request/offer with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Aid getAid(@PathParam("id") final long id) {
        Aid aid = aidDAO.getAid(id);

        if (aid == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Aid.fromAid(aid);
    }

    @Documentation("Returns all aid requests and offers")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Aid>> getAid() {
        List<RS_Aid> aid = new ArrayList<>();

        for (Aid aidReq : aidDAO.getAllAid()) {
            aid.add(RS_Aid.fromAid(aidReq));
        }

        return JResponse.ok(aid).build();
    }

    @Documentation("Updates an aid request/offer (NOTE: does not remove automatically, ever, so if the amount is <=0, use a delete instead). " +
            "Returns the updated object.")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Aid updateAid(@PathParam("id") final long id,
                            @Documentation(value = "An aid object containing the updates", itemName = "updatedAid")
                            final RS_Aid updatedAid) {
        validate(updatedAid).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        Aid aid = aidDAO.getAid(id);

        if (aid == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        if (!Objects.equals(aid.getExpiryDate(), updatedAid.getExpires())) {
            timerManagerProvider.get().cancelTimer(Aid.class, aid.getId());
            if (updatedAid.getExpires() != null) {
                long delay = updatedAid.getExpires().getTime() - System.currentTimeMillis();
                timerManagerProvider.get().schedule(new Timer(Aid.class, aid.getId(), aidManagerProvider.get()), delay, TimeUnit.MILLISECONDS);
            }
        }

        RS_Aid.toAid(aid, updatedAid);
        return RS_Aid.fromAid(aid);
    }

    @Documentation("Deletes the aid with the specified id and removes its timer")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteAid(@PathParam("id") final long id) {
        Aid aid = aidDAO.getAid(id);
        if (aid == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        aidDAO.delete(aid);
        timerManagerProvider.get().cancelTimer(Aid.class, aid.getId());
    }
}
