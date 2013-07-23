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
import database.daos.ProvinceDAO;
import database.daos.SpellDAO;
import database.models.*;
import events.DurationSpellRegisteredEvent;
import events.InstantSpellRegisteredEvent;
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

@ValidationEnabled
@Path("spells")
public class SpellResource {
    private static final SpellType[] EMPTY_SPELL_TYPE_ARRAY = new SpellType[0];
    private static final Function<Collection<DurationSpell>, Collection<RS_DurationSpell>> DURATION_SPELL_CONVERTER = new Function<Collection<DurationSpell>, Collection<RS_DurationSpell>>() {
        @Override
        public Collection<RS_DurationSpell> apply(@Nullable final Collection<DurationSpell> input) {
            Collection<RS_DurationSpell> out = new ArrayList<>(input.size());
            for (DurationSpell spell : input) {
                out.add(RS_DurationSpell.fromDurationSpell(spell));
            }
            return out;
        }
    };
    private static final Function<Collection<InstantSpell>, Collection<RS_InstantSpell>> INSTANT_SPELL_CONVERTER = new Function<Collection<InstantSpell>, Collection<RS_InstantSpell>>() {
        @Override
        public Collection<RS_InstantSpell> apply(@Nullable final Collection<InstantSpell> input) {
            Collection<RS_InstantSpell> out = new ArrayList<>(input.size());
            for (InstantSpell spell : input) {
                out.add(RS_InstantSpell.fromInstantSpell(spell));
            }
            return out;
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

    /**
     * Adds a duration spell
     *
     * @param spell the spell to add
     * @return the added spell
     */
    @Path("duration")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_DurationSpell addDurationSpell(@Valid final RS_DurationSpell spell) {
        BotUser user = botUserDAOProvider.get().getUser(spell.getCaster().getId());
        Province province = provinceDAOProvider.get().getProvince(spell.getProvince().getId());
        SpellType spellType = spellDAO.getSpellType(spell.getType().getId());

        DurationSpell durationSpell = getOrCreateDurationSpell(province, user, spellType, spell);
        spellDAO.save(durationSpell);
        afterCommitEventPosterProvider.get().addEventToPost(new DurationSpellRegisteredEvent(durationSpell.getId(), null));
        return RS_DurationSpell.fromDurationSpell(durationSpell);
    }

    private static DurationSpell getOrCreateDurationSpell(final Province province,
                                                          final BotUser user,
                                                          final SpellType spellType,
                                                          final RS_DurationSpell op) {
        DurationSpell existing = province.getDurationSpell(spellType);
        if (existing != null) {
            existing.setCommitter(user);
            existing.setExpires(op.getExpires());
            return existing;
        }
        return new DurationSpell(user, province, op.getExpires(), spellType);
    }

    /**
     * Lists duration spell according to the specified criteria.
     * The kingdomIds, provinceIds and userId parameters are mutually exclusive, meaning if one is used, the others
     * will be ignored. The spellTypeIds can be used in combination with all the others however.
     * <p/>
     * NOTE: The userId param leads the listing of whatever duration spell are currently active that were committed by the specified user,
     * not the spell he/she currently has on his/her province.
     *
     * @param kingdomIds   the id's of kingdoms to get spell for
     * @param provinceIds  the id's of province to get spell for
     * @param spellTypeIds the type of spells to get
     * @param userId       the id of the user to get committed spells for
     * @return a list of spells
     */
    @Path("duration")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_DurationSpell>> getDurationSpells(@QueryParam("kingdomIds") final List<Long> kingdomIds,
                                                               @QueryParam("provinceIds") final List<Long> provinceIds,
                                                               @QueryParam("spellTypeIds") final List<Long> spellTypeIds,
                                                               @QueryParam("userId") final Long userId) {
        List<RS_DurationSpell> spells = new ArrayList<>();

        SpellType[] spellTypes = idsToTypes(spellTypeIds);
        if (isNotEmpty(kingdomIds)) {
            KingdomDAO kingdomDAO = kingdomDAOProvider.get();
            for (Long kingdomId : kingdomIds) {
                Kingdom kingdom = kingdomDAO.getKingdom(kingdomId);
                if (kingdom != null)
                    spells.addAll(DURATION_SPELL_CONVERTER.apply(spellDAO.getDurationSpells(kingdom, spellTypes)));
            }
        } else if (isNotEmpty(provinceIds)) {
            ProvinceDAO provinceDAO = provinceDAOProvider.get();

            for (Province province : provinceDAO.getProvinces(provinceIds.toArray(new Long[provinceIds.size()]))) {
                if (spellTypes.length == 0) spells.addAll(DURATION_SPELL_CONVERTER.apply(province.getDurationSpells()));
                else {
                    for (SpellType spellType : spellTypes) {
                        spells.add(RS_DurationSpell.fromDurationSpell(province.getDurationSpell(spellType)));
                    }
                }
            }
        } else if (userId != null) {
            BotUser user = botUserDAOProvider.get().getUser(userId);
            checkNotNull(user, "No such user");
            spells.addAll(DURATION_SPELL_CONVERTER.apply(spellDAO.getDurationSpellsCommittedByUser(user, spellTypes)));
        } else {
            spells.addAll(DURATION_SPELL_CONVERTER.apply(spellDAO.getDurationSpells(spellTypes)));
        }

        return JResponse.ok(spells).build();
    }

    /**
     * Deletes a duration spell
     *
     * @param id the id of the spell
     */
    @Path("duration/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteDurationSpell(@PathParam("id") final long id) {
        DurationSpell spell = spellDAO.getDurationSpell(id);
        if (spell == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        spellDAO.delete(spell);
    }

    /**
     * Deletes duration spells in bulk
     *
     * @param kingdomId    the kingdom to delete spells for
     * @param spellTypeIds the types of spells to delete
     */
    @Path("duration")
    @DELETE
    @Transactional
    public void deleteDurationSpells(@QueryParam("kingdomId") final Long kingdomId,
                                     @QueryParam("spellTypeIds") final List<Long> spellTypeIds) {
        SpellType[] spellTypes = idsToTypes(spellTypeIds);
        if (kingdomId != null) {
            Kingdom kingdom = kingdomDAOProvider.get().getKingdom(kingdomId);
            checkNotNull(kingdom, "No such kingdom");
            spellDAO.deleteDurationSpells(kingdom, spellTypes);
        } else {
            spellDAO.deleteDurationSpells(spellTypes);
        }
    }

    /**
     * Adds an instant spell
     *
     * @param spell the spell to add
     * @return the added spell
     */
    @Path("instant")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_InstantSpell addInstantSpell(@Valid final RS_InstantSpell spell) {
        BotUser user = botUserDAOProvider.get().getUser(spell.getCaster().getId());
        Province province = provinceDAOProvider.get().getProvince(spell.getProvince().getId());
        SpellType spellType = spellDAO.getSpellType(spell.getType().getId());

        InstantSpell instantSpell = getOrCreateInstantSpell(province, user, spellType, spell);
        spellDAO.save(instantSpell);
        afterCommitEventPosterProvider.get().addEventToPost(new InstantSpellRegisteredEvent(instantSpell.getId(), null));
        return RS_InstantSpell.fromInstantSpell(instantSpell);
    }

    private static InstantSpell getOrCreateInstantSpell(final Province province,
                                                        final BotUser user,
                                                        final SpellType spellType,
                                                        final RS_InstantSpell spell) {
        for (InstantSpell instantSpell : province.getInstantSpells(spellType)) {
            if (instantSpell.getCommitter().equals(user)) {
                instantSpell.setAmount(instantSpell.getAmount() + spell.getAmount());
                instantSpell.setDamage(instantSpell.getDamage() + spell.getDamage());
                return instantSpell;
            }
        }
        return new InstantSpell(user, province, spell.getDamage(), spell.getAmount(), spellType);
    }

    /**
     * Lists instant spells according to the specified criteria.
     * The kingdomIds, provinceIds and userId parameters are mutually exclusive, meaning if one is used, the others
     * will be ignored. The spellTypeIds can be used in combination with all the others however.
     * <p/>
     * NOTE: The userId param leads the listing of whatever instant spells were committed by the specified user,
     * not the spells he/she received on his/her province.
     *
     * @param kingdomIds   the id's of kingdoms to get spells for
     * @param provinceIds  the id's of province to get spells for
     * @param spellTypeIds the type of spells to get
     * @param userId       the id of the user to get committed spells for
     * @return a list of spells
     */
    @Path("instant")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_InstantSpell>> getInstantSpells(@QueryParam("kingdomIds") final List<Long> kingdomIds,
                                                             @QueryParam("provinceIds") final List<Long> provinceIds,
                                                             @QueryParam("spellTypeIds") final List<Long> spellTypeIds,
                                                             @QueryParam("userId") final Long userId) {
        List<RS_InstantSpell> spells = new ArrayList<>();

        SpellType[] spellTypes = idsToTypes(spellTypeIds);
        if (isNotEmpty(kingdomIds)) {
            KingdomDAO kingdomDAO = kingdomDAOProvider.get();
            for (Long kingdomId : kingdomIds) {
                Kingdom kingdom = kingdomDAO.getKingdom(kingdomId);
                if (kingdom == null) continue;

                for (Province province : kingdom.getProvinces()) {
                    if (spellTypes.length == 0)
                        spells.addAll(INSTANT_SPELL_CONVERTER.apply(province.getInstantSpells()));
                    else {
                        for (SpellType spellType : spellTypes) {
                            spells.addAll(INSTANT_SPELL_CONVERTER.apply(province.getInstantSpells(spellType)));
                        }
                    }
                }
            }
        } else if (isNotEmpty(provinceIds)) {
            ProvinceDAO provinceDAO = provinceDAOProvider.get();

            for (Province province : provinceDAO.getProvinces(provinceIds.toArray(new Long[provinceIds.size()]))) {
                if (spellTypes.length == 0) spells.addAll(INSTANT_SPELL_CONVERTER.apply(province.getInstantSpells()));
                else {
                    for (SpellType spellType : spellTypes) {
                        spells.addAll(INSTANT_SPELL_CONVERTER.apply(province.getInstantSpells(spellType)));
                    }
                }
            }
        } else if (userId != null) {
            BotUser user = botUserDAOProvider.get().getUser(userId);
            checkNotNull(user, "No such user");
            spells.addAll(INSTANT_SPELL_CONVERTER.apply(spellDAO.getInstantSpellsCastByUser(user, spellTypes)));
        } else {
            spells.addAll(INSTANT_SPELL_CONVERTER.apply(spellDAO.getInstantSpells(spellTypes)));
        }

        return JResponse.ok(spells).build();
    }

    /**
     * Deletes an instant spell
     *
     * @param id the id of the spell
     */
    @Path("instant/{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteInstantSpell(@PathParam("id") final long id) {
        InstantSpell spell = spellDAO.getInstantSpell(id);
        if (spell == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        spellDAO.delete(spell);
    }

    /**
     * Deletes instant spells in bulk
     *
     * @param kingdomId    the kingdom to delete spells for
     * @param spellTypeIds the types of spells to delete
     */
    @Path("instant")
    @DELETE
    @Transactional
    public void deleteInstantSpells(@QueryParam("kingdomId") final Long kingdomId, @QueryParam("spellTypeIds") final List<Long> spellTypeIds) {
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
