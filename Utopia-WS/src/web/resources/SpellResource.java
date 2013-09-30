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
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.KingdomDAO;
import database.daos.ProvinceDAO;
import database.daos.SpellDAO;
import database.models.*;
import events.DurationSpellRegisteredEvent;
import events.InstantSpellRegisteredEvent;
import web.documentation.Documentation;
import web.models.RS_DurationSpell;
import web.models.RS_InstantSpell;
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
@Path("spells")
public class SpellResource {
    private static final SpellType[] EMPTY_SPELL_TYPE_ARRAY = new SpellType[0];
    private static final Function<DurationSpell, RS_DurationSpell> DURATION_SPELL_CONVERTER = new Function<DurationSpell, RS_DurationSpell>() {
        @Override
        public RS_DurationSpell apply(@Nullable final DurationSpell input) {
            return RS_DurationSpell.fromDurationSpell(input);
        }
    };
    private static final Function<InstantSpell, RS_InstantSpell> INSTANT_SPELL_CONVERTER = new Function<InstantSpell, RS_InstantSpell>() {
        @Override
        public RS_InstantSpell apply(@Nullable final InstantSpell input) {
            return RS_InstantSpell.fromInstantSpell(input);
        }
    };

    private final SpellDAO spellDAO;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider;

    @Inject
    public SpellResource(final SpellDAO spellDAO,
                         final Provider<KingdomDAO> kingdomDAOProvider,
                         final Provider<ProvinceDAO> provinceDAOProvider,
                         final Provider<BotUserDAO> botUserDAOProvider,
                         final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider) {
        this.spellDAO = spellDAO;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;
        this.afterCommitEventPosterProvider = afterCommitEventPosterProvider;
    }

    @Documentation("Adds a new duration spell, fires off a DurationSpellRegisteredEvent and returns the saved object")
    @Path("duration")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_DurationSpell addDurationSpell(@Documentation(value = "The spell to add", itemName = "spell")
                                             @Valid final RS_DurationSpell spell) {
        BotUser user = botUserDAOProvider.get().getUser(spell.getCaster().getId());
        Province province = provinceDAOProvider.get().getProvince(spell.getProvince().getId());
        SpellType spellType = spellDAO.getSpellType(spell.getType().getId());

        final DurationSpell durationSpell = getOrCreateDurationSpell(province, user, spellType, spell);
        afterCommitEventPosterProvider.get().addEventToPost(new Supplier<Object>() {
            @Override
            public Object get() {
                return new DurationSpellRegisteredEvent(durationSpell.getId(), null);
            }
        });
        return RS_DurationSpell.fromDurationSpell(durationSpell);
    }

    private static DurationSpell getOrCreateDurationSpell(final Province province,
                                                          final BotUser user,
                                                          final SpellType spellType,
                                                          final RS_DurationSpell spell) {
        DurationSpell existing = province.getDurationSpell(spellType);
        if (existing != null) {
            existing.setCommitter(user);
            existing.setExpires(spell.getExpires());
            return existing;
        } else {
            DurationSpell newSpell = new DurationSpell(user, province, spell.getExpires(), spellType);
            province.addDurationSpell(newSpell);
            return newSpell;
        }
    }

    @Documentation("Lists duration spells according to the specified criteria. " +
            "The kingdomIds, provinceIds and userId parameters are mutually exclusive, meaning if one is used, the others " +
            "will be ignored. The opTypeIds can be used in combination with all the others however. " +
            "<p/> " +
            "NOTE: The userId param leads the listing of whatever duration spells are currently active that were cast by the specified user, " +
            "not the ops he/she currently has on his/her province.")
    @Path("duration")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_DurationSpell>> getDurationSpells(@Documentation("Id's for the kingdoms to get spells for. Cannot be combined with other params")
                                                               @QueryParam("kingdomIds")
                                                               final List<Long> kingdomIds,
                                                               @Documentation("Id's for the provinces to get spells for. Cannot be combined with other params")
                                                               @QueryParam("provinceIds")
                                                               final List<Long> provinceIds,
                                                               @Documentation("Id's for the spells types to limit the response to " +
                                                                       "(applies in all situations, regardless of which other params were used)")
                                                               @QueryParam("spellTypeIds")
                                                               final List<Long> spellTypeIds,
                                                               @Documentation("Id for the user to get spells for (cast by). Cannot be combined with other params")
                                                               @QueryParam("userId")
                                                               final Long userId) {
        List<RS_DurationSpell> spells = new ArrayList<>();

        SpellType[] spellTypes = idsToTypes(spellTypeIds);
        if (isNotEmpty(kingdomIds)) {
            KingdomDAO kingdomDAO = kingdomDAOProvider.get();
            for (Long kingdomId : kingdomIds) {
                Kingdom kingdom = kingdomDAO.getKingdom(kingdomId);
                if (kingdom != null)
                    spells.addAll(transform(spellDAO.getDurationSpells(kingdom, spellTypes), DURATION_SPELL_CONVERTER));
            }
        } else if (isNotEmpty(provinceIds)) {
            ProvinceDAO provinceDAO = provinceDAOProvider.get();

            Long[] provinceIdArray = provinceIds.toArray(new Long[provinceIds.size()]);
            for (Province province : provinceDAO.getProvinces(provinceIdArray)) {
                if (spellTypes.length == 0) spells.addAll(transform(province.getDurationSpells(), DURATION_SPELL_CONVERTER));
                else {
                    for (SpellType spellType : spellTypes) {
                        spells.add(RS_DurationSpell.fromDurationSpell(province.getDurationSpell(spellType)));
                    }
                }
            }
        } else if (userId != null) {
            BotUser user = botUserDAOProvider.get().getUser(userId);
            checkNotNull(user, "No such user");
            spells.addAll(transform(spellDAO.getDurationSpellsCommittedByUser(user, spellTypes), DURATION_SPELL_CONVERTER));
        } else {
            spells.addAll(transform(spellDAO.getDurationSpells(spellTypes), DURATION_SPELL_CONVERTER));
        }

        return JResponse.ok(spells).build();
    }

    @Documentation("Deletes the specified duration spell")
    @Path("duration/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteDurationSpell(@PathParam("id") final long id) {
        DurationSpell spell = spellDAO.getDurationSpell(id);
        if (spell == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        spellDAO.delete(spell);
    }

    @Documentation("Bulk deletes duration spells of the specified spell types (all if unspecified), optionally for just a specific kingdom")
    @Path("duration")
    @DELETE
    @Transactional
    public void deleteDurationSpells(@Documentation("The id of the kingdom to delete spell for")
                                     @QueryParam("kingdomId")
                                     final Long kingdomId,
                                     @Documentation("The spell types to delete. If not specified, all types will be removed")
                                     @QueryParam("spellTypeIds")
                                     final List<Long> spellTypeIds) {
        SpellType[] spellTypes = idsToTypes(spellTypeIds);
        if (kingdomId != null) {
            Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
            checkNotNull(kingdom, "No such kingdom");
            spellDAO.deleteDurationSpells(kingdom, spellTypes);
        } else {
            spellDAO.deleteDurationSpells(spellTypes);
        }
    }

    @Documentation("Adds the specified spell, fires off an InstantSpellRegisteredEvent and returns the saved object")
    @Path("instant")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_InstantSpell addInstantSpell(@Documentation(value = "The spell to add", itemName = "spell")
                                           @Valid final RS_InstantSpell spell) {
        BotUser user = botUserDAOProvider.get().getUser(spell.getCaster().getId());
        Province province = provinceDAOProvider.get().getProvince(spell.getProvince().getId());
        SpellType spellType = spellDAO.getSpellType(spell.getType().getId());

        final InstantSpell instantSpell = province.registerInstantSpell(user, spellType, spell.getDamage());
        afterCommitEventPosterProvider.get().addEventToPost(new Supplier<Object>() {
            @Override
            public Object get() {
                return new InstantSpellRegisteredEvent(instantSpell.getId(), null);
            }
        });
        return RS_InstantSpell.fromInstantSpell(instantSpell);
    }

    @Documentation("Lists instant spells according to the specified criteria. " +
            "The kingdomIds, provinceIds and userId parameters are mutually exclusive, meaning if one is used, the others " +
            "will be ignored. The spellTypeIds can be used in combination with all the others however. " +
            "<p/> " +
            "NOTE: The userId param leads the listing of whatever instant spells were cast by the specified user, not on.")
    @Path("instant")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_InstantSpell>> getInstantSpells(@Documentation("Id's for the kingdoms to get spells for. Cannot be combined with other params")
                                                             @QueryParam("kingdomIds")
                                                             final List<Long> kingdomIds,
                                                             @Documentation("Id's for the provinces to get spells for. Cannot be combined with other params")
                                                             @QueryParam("provinceIds")
                                                             final List<Long> provinceIds,
                                                             @Documentation("Id's for the spell types to limit the response to " +
                                                                     "(applies in all situations, regardless of which other params were used)")
                                                             @QueryParam("spellTypeIds")
                                                             final List<Long> spellTypeIds,
                                                             @Documentation("Id for the user to get spells for (committed by). Cannot be combined with other params")
                                                             @QueryParam("userId")
                                                             final Long userId) {
        List<RS_InstantSpell> spells = new ArrayList<>();

        SpellType[] spellTypes = idsToTypes(spellTypeIds);
        if (isNotEmpty(kingdomIds)) {
            KingdomDAO kingdomDAO = kingdomDAOProvider.get();
            for (Long kingdomId : kingdomIds) {
                Kingdom kingdom = kingdomDAO.getKingdom(kingdomId);
                if (kingdom == null) continue;

                for (Province province : kingdom.getProvinces()) {
                    if (spellTypes.length == 0)
                        spells.addAll(transform(province.getInstantSpells(), INSTANT_SPELL_CONVERTER));
                    else {
                        for (SpellType spellType : spellTypes) {
                            spells.addAll(transform(province.getInstantSpells(spellType), INSTANT_SPELL_CONVERTER));
                        }
                    }
                }
            }
        } else if (isNotEmpty(provinceIds)) {
            ProvinceDAO provinceDAO = provinceDAOProvider.get();

            for (Province province : provinceDAO.getProvinces(provinceIds.toArray(new Long[provinceIds.size()]))) {
                if (spellTypes.length == 0) spells.addAll(transform(province.getInstantSpells(), INSTANT_SPELL_CONVERTER));
                else {
                    for (SpellType spellType : spellTypes) {
                        spells.addAll(transform(province.getInstantSpells(spellType), INSTANT_SPELL_CONVERTER));
                    }
                }
            }
        } else if (userId != null) {
            BotUser user = botUserDAOProvider.get().getUser(userId);
            checkNotNull(user, "No such user");
            spells.addAll(transform(spellDAO.getInstantSpellsCastByUser(user, spellTypes), INSTANT_SPELL_CONVERTER));
        } else {
            spells.addAll(transform(spellDAO.getInstantSpells(spellTypes), INSTANT_SPELL_CONVERTER));
        }

        return JResponse.ok(spells).build();
    }

    @Documentation("Deletes the specified instant spell")
    @Path("instant/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteInstantSpell(@PathParam("id") final long id) {
        InstantSpell spell = spellDAO.getInstantSpell(id);
        if (spell == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        spellDAO.delete(spell);
    }

    @Documentation("Bulk deletes instant spells of the specified spell types (all if unspecified), optionally for just a specific kingdom")
    @Path("instant")
    @DELETE
    @Transactional
    public void deleteInstantSpells(@Documentation("The id of the kingdom to delete spells for")
                                    @QueryParam("kingdomId")
                                    final Long kingdomId,
                                    @Documentation("The spell types to delete. If not specified, all types will be removed")
                                    @QueryParam("spellTypeIds")
                                    final List<Long> spellTypeIds) {
        SpellType[] spellTypes = idsToTypes(spellTypeIds);
        if (kingdomId != null) {
            Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
            checkNotNull(kingdom, "No such kingdom");
            spellDAO.deleteInstantSpells(kingdom, spellTypes);
        } else {
            spellDAO.deleteInstantSpells(spellTypes);
        }
    }

    private SpellType[] idsToTypes(final Collection<Long> spellTypeIds) {
        if (isEmpty(spellTypeIds)) return EMPTY_SPELL_TYPE_ARRAY;

        List<SpellType> spellTypes = spellDAO.getSpellTypes(spellTypeIds.toArray(new Long[spellTypeIds.size()]));
        return spellTypes.toArray(new SpellType[spellTypes.size()]);
    }
}
