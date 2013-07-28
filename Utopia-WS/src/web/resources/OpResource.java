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
import api.tools.validation.ValidationEnabled;
import com.google.common.base.Function;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.KingdomDAO;
import database.daos.OpDAO;
import database.daos.ProvinceDAO;
import database.models.*;
import events.DurationOpRegisteredEvent;
import events.InstantOpRegisteredEvent;
import web.documentation.Documentation;
import web.models.RS_DurationOp;
import web.models.RS_InstantOp;
import web.tools.AfterCommitEventPoster;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static api.tools.collections.CollectionUtil.isEmpty;
import static api.tools.collections.CollectionUtil.isNotEmpty;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Collections2.transform;

@ValidationEnabled
@Path("ops")
public class OpResource {
    private static final OpType[] EMPTY_OP_TYPE_ARRAY = new OpType[0];
    private static final Function<DurationOp, RS_DurationOp> DURATION_OP_CONVERTER = new Function<DurationOp, RS_DurationOp>() {
        @Override
        public RS_DurationOp apply(@Nullable final DurationOp input) {
            return RS_DurationOp.fromDurationOp(input);
        }
    };
    private static final Function<InstantOp, RS_InstantOp> INSTANT_OP_CONVERTER = new Function<InstantOp, RS_InstantOp>() {
        @Override
        public RS_InstantOp apply(@Nullable final InstantOp input) {
            return RS_InstantOp.fromInstantOp(input);
        }
    };

    private final OpDAO opDAO;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider;

    @Inject
    public OpResource(final OpDAO opDAO,
                      final Provider<KingdomDAO> kingdomDAOProvider,
                      final Provider<ProvinceDAO> provinceDAOProvider,
                      final Provider<BotUserDAO> botUserDAOProvider,
                      final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider) {
        this.opDAO = opDAO;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;
        this.afterCommitEventPosterProvider = afterCommitEventPosterProvider;
    }

    @Documentation("Adds a new duration op, fires off a DurationOpRegisteredEvent and returns the saved object")
    @Path("duration")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_DurationOp addDurationOp(@Documentation(value = "The op to add", itemName = "op")
                                       @Valid final RS_DurationOp op) {
        BotUser user = botUserDAOProvider.get().getUser(op.getCommitter().getId());
        Province province = provinceDAOProvider.get().getProvince(op.getProvince().getId());
        OpType opType = opDAO.getOpType(op.getType().getId());

        DurationOp durationOp = getOrCreateDurationOp(province, user, opType, op);
        opDAO.save(durationOp);
        afterCommitEventPosterProvider.get().addEventToPost(new DurationOpRegisteredEvent(durationOp.getId(), null));
        return RS_DurationOp.fromDurationOp(durationOp);
    }

    private static DurationOp getOrCreateDurationOp(final Province province,
                                                    final BotUser user,
                                                    final OpType opType,
                                                    final RS_DurationOp op) {
        DurationOp existing = province.getDurationOp(opType);
        if (existing != null) {
            existing.setCommitter(user);
            existing.setExpires(op.getExpires());
            return existing;
        }
        return new DurationOp(user, province, op.getExpires(), opType);
    }

    @Documentation("Lists duration ops according to the specified criteria. " +
            "The kingdomIds, provinceIds and userId parameters are mutually exclusive, meaning if one is used, the others " +
            "will be ignored. The opTypeIds can be used in combination with all the others however. " +
            "<p/> " +
            "NOTE: The userId param leads the listing of whatever duration ops are currently active that were committed by the specified user, " +
            "not the ops he/she currently has on his/her province.")
    @Path("duration")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_DurationOp>> getDurationOps(@Documentation("Id's for the kingdoms to get ops for. Cannot be combined with other params")
                                                         @QueryParam("kingdomIds")
                                                         final List<Long> kingdomIds,
                                                         @Documentation("Id's for the provinces to get ops for. Cannot be combined with other params")
                                                         @QueryParam("provinceIds")
                                                         final List<Long> provinceIds,
                                                         @Documentation("Id's for the op types to limit the response to " +
                                                                 "(applies in all situations, regardless of which other params were used)")
                                                         @QueryParam("opTypeIds")
                                                         final List<Long> opTypeIds,
                                                         @Documentation("Id for the user to get ops for (committed by). Cannot be combined with other params")
                                                         @QueryParam("userId")
                                                         final Long userId) {
        List<RS_DurationOp> ops = new ArrayList<>();

        OpType[] opTypes = idsToTypes(opTypeIds);
        if (isNotEmpty(kingdomIds)) {
            KingdomDAO kingdomDAO = kingdomDAOProvider.get();
            for (Long kingdomId : kingdomIds) {
                Kingdom kingdom = kingdomDAO.getKingdom(kingdomId);
                if (kingdom != null)
                    ops.addAll(transform(opDAO.getDurationOps(kingdom, opTypes), DURATION_OP_CONVERTER));
            }
        } else if (isNotEmpty(provinceIds)) {
            ProvinceDAO provinceDAO = provinceDAOProvider.get();

            Long[] provinceIdsArray = provinceIds.toArray(new Long[provinceIds.size()]);
            for (Province province : provinceDAO.getProvinces(provinceIdsArray)) {
                if (opTypes.length == 0) ops.addAll(transform(province.getDurationOps(), DURATION_OP_CONVERTER));
                else {
                    for (OpType opType : opTypes) {
                        ops.add(RS_DurationOp.fromDurationOp(province.getDurationOp(opType)));
                    }
                }
            }
        } else if (userId != null) {
            BotUser user = botUserDAOProvider.get().getUser(userId);
            checkNotNull(user, "No such user");
            ops.addAll(transform(opDAO.getDurationOpsCommittedByUser(user, opTypes), DURATION_OP_CONVERTER));
        } else {
            ops.addAll(transform(opDAO.getDurationOps(opTypes), DURATION_OP_CONVERTER));
        }

        return JResponse.ok(ops).build();
    }

    @Documentation("Deletes the specified duration op")
    @Path("duration/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteDurationOp(@PathParam("id") final long id) {
        DurationOp op = opDAO.getDurationOp(id);
        if (op == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        opDAO.delete(op);
    }

    @Documentation("Bulk deletes duration ops of the specified op types (all if unspecified), optionally for just a specific kingdom")
    @Path("duration")
    @DELETE
    @Transactional
    public void deleteDurationOps(@Documentation("The id of the kingdom to delete ops for")
                                  @QueryParam("kingdomId")
                                  final Long kingdomId,
                                  @Documentation("The op types to delete. If not specified, all types will be removed")
                                  @QueryParam("opTypeIds")
                                  final List<Long> opTypeIds) {
        OpType[] opTypes = idsToTypes(opTypeIds);
        if (kingdomId != null) {
            Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
            checkNotNull(kingdom, "No such kingdom");
            opDAO.deleteDurationOps(kingdom, opTypes);
        } else {
            opDAO.deleteDurationOps(opTypes);
        }
    }

    @Documentation("Adds the specified op, fires off an InstantOpRegisteredEvent and returns the saved object")
    @Path("instant")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_InstantOp addInstantOp(@Documentation(value = "The op to add", itemName = "op")
                                     @Valid final RS_InstantOp op) {
        BotUser user = botUserDAOProvider.get().getUser(op.getCommitter().getId());
        Province province = provinceDAOProvider.get().getProvince(op.getProvince().getId());
        OpType opType = opDAO.getOpType(op.getType().getId());

        InstantOp instantOp = getOrCreateInstantOp(province, user, opType, op);
        opDAO.save(instantOp);
        afterCommitEventPosterProvider.get().addEventToPost(new InstantOpRegisteredEvent(instantOp.getId(), null));
        return RS_InstantOp.fromInstantOp(instantOp);
    }

    private static InstantOp getOrCreateInstantOp(final Province province,
                                                  final BotUser user,
                                                  final OpType opType,
                                                  final RS_InstantOp op) {
        for (InstantOp instantOp : province.getInstantOps(opType)) {
            if (instantOp.getCommitter().equals(user)) {
                instantOp.setAmount(instantOp.getAmount() + op.getAmount());
                instantOp.setDamage(instantOp.getDamage() + op.getDamage());
                return instantOp;
            }
        }
        return new InstantOp(user, province, op.getDamage(), op.getAmount(), opType);
    }

    @Documentation("Lists instant ops according to the specified criteria. " +
            "The kingdomIds, provinceIds and userId parameters are mutually exclusive, meaning if one is used, the others " +
            "will be ignored. The opTypeIds can be used in combination with all the others however. " +
            "<p/> " +
            "NOTE: The userId param leads the listing of whatever instant ops were committed by the specified user, not on.")
    @Path("instant")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_InstantOp>> getInstantOps(@Documentation("Id's for the kingdoms to get ops for. Cannot be combined with other params")
                                                       @QueryParam("kingdomIds")
                                                       final List<Long> kingdomIds,
                                                       @Documentation("Id's for the provinces to get ops for. Cannot be combined with other params")
                                                       @QueryParam("provinceIds")
                                                       final List<Long> provinceIds,
                                                       @Documentation("Id's for the op types to limit the response to " +
                                                               "(applies in all situations, regardless of which other params were used)")
                                                       @QueryParam("opTypeIds")
                                                       final List<Long> opTypeIds,
                                                       @Documentation("Id for the user to get ops for (committed by). Cannot be combined with other params")
                                                       @QueryParam("userId")
                                                       final Long userId) {
        List<RS_InstantOp> ops = new ArrayList<>();

        OpType[] opTypes = idsToTypes(opTypeIds);
        if (isNotEmpty(kingdomIds)) {
            KingdomDAO kingdomDAO = kingdomDAOProvider.get();
            for (Long kingdomId : kingdomIds) {
                Kingdom kingdom = kingdomDAO.getKingdom(kingdomId);
                if (kingdom == null) continue;

                for (Province province : kingdom.getProvinces()) {
                    if (opTypes.length == 0) ops.addAll(transform(province.getInstantOps(), INSTANT_OP_CONVERTER));
                    else {
                        for (OpType opType : opTypes) {
                            ops.addAll(transform(province.getInstantOps(opType), INSTANT_OP_CONVERTER));
                        }
                    }
                }
            }
        } else if (isNotEmpty(provinceIds)) {
            ProvinceDAO provinceDAO = provinceDAOProvider.get();

            Long[] provinceIdsArray = provinceIds.toArray(new Long[provinceIds.size()]);
            for (Province province : provinceDAO.getProvinces(provinceIdsArray)) {
                if (opTypes.length == 0) ops.addAll(transform(province.getInstantOps(), INSTANT_OP_CONVERTER));
                else {
                    for (OpType opType : opTypes) {
                        ops.addAll(transform(province.getInstantOps(opType), INSTANT_OP_CONVERTER));
                    }
                }
            }
        } else if (userId != null) {
            BotUser user = botUserDAOProvider.get().getUser(userId);
            checkNotNull(user, "No such user");
            ops.addAll(transform(opDAO.getInstantOpsCommittedByUser(user, opTypes), INSTANT_OP_CONVERTER));
        } else {
            ops.addAll(transform(opDAO.getInstantOps(opTypes), INSTANT_OP_CONVERTER));
        }

        return JResponse.ok(ops).build();
    }

    @Documentation("Deletes the specified instant op")
    @Path("instant/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteInstantOp(@PathParam("id") final long id) {
        InstantOp op = opDAO.getInstantOp(id);
        if (op == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        opDAO.delete(op);
    }

    @Documentation("Bulk deletes instant ops of the specified op types (all if unspecified), optionally for just a specific kingdom")
    @Path("instant")
    @DELETE
    @Transactional
    public void deleteInstantOps(@Documentation("The id of the kingdom to delete ops for")
                                 @QueryParam("kingdomId")
                                 final Long kingdomId,
                                 @Documentation("The op types to delete. If not specified, all types will be removed")
                                 @QueryParam("opTypeIds")
                                 final List<Long> opTypeIds) {
        OpType[] opTypes = idsToTypes(opTypeIds);
        if (kingdomId != null) {
            Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
            checkNotNull(kingdom, "No such kingdom");
            opDAO.deleteInstantOps(kingdom, opTypes);
        } else {
            opDAO.deleteInstantOps(opTypes);
        }
    }

    private OpType[] idsToTypes(final Collection<Long> opTypeIds) {
        if (isEmpty(opTypeIds)) return EMPTY_OP_TYPE_ARRAY;

        List<OpType> opTypes = opDAO.getOpTypes(opTypeIds.toArray(new Long[opTypeIds.size()]));
        return opTypes.toArray(new OpType[opTypes.size()]);
    }
}
