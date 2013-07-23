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

package commands.spells_ops.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.files.FilterUtil;
import commands.spells_ops.ProvinceWithInstantSpellsOrOps;
import database.daos.KingdomDAO;
import database.daos.OpDAO;
import database.daos.ProvinceDAO;
import database.daos.SpellDAO;
import database.models.*;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BestMatchFinder;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ResultsCommandHandler implements CommandHandler {
    private final SpellDAO spellDAO;
    private final OpDAO opDAO;
    private final KingdomDAO kingdomDAO;
    private final ProvinceDAO provinceDAO;
    private final BestMatchFinder bestMatchFinder;

    @Inject
    public ResultsCommandHandler(final ProvinceDAO provinceDAO,
                                 final KingdomDAO kingdomDAO,
                                 final OpDAO opDAO,
                                 final SpellDAO spellDAO,
                                 final BestMatchFinder bestMatchFinder) {
        this.provinceDAO = provinceDAO;
        this.kingdomDAO = kingdomDAO;
        this.opDAO = opDAO;
        this.spellDAO = spellDAO;
        this.bestMatchFinder = bestMatchFinder;
    }

    @Override
    public CommandResponse handleCommand(final IRCContext context,
                                         final Params params,
                                         final Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            SpellType spellType = spellDAO.getSpellType(params.getParameter("spellOrOp"));

            OpType opType = spellType == null ? opDAO.getOpType(params.getParameter("spellOrOp")) : null;

            if (params.containsKey("userOrProv")) {
                Kingdom kingdom = params.containsKey("kingdom") ? kingdomDAO.getKingdom(params.getParameter("kingdom")) : null;

                if (kingdom != null) { //we know it's supposed to be a prov
                    Province province = provinceDAO.getClosestMatch(params.getParameter("userOrProv"), kingdom);
                    if (province == null) return CommandResponse.errorResponse("No such province found");
                    return getResultsFromProvince(spellType, opType, province);
                } else { //could be either a user or a prov
                    Province province = bestMatchFinder.findBestMatch(params.getParameter("userOrProv"));

                    if (province == null) return CommandResponse.errorResponse("No matching user or province found");
                    return getResultsFromProvince(spellType, opType, province);
                }
            } else {
                Kingdom kingdom = params.containsKey("kingdom") ? kingdomDAO.getKingdom(params.getParameter("kingdom")) : null;

                if (kingdom == null) return CommandResponse.errorResponse("No such kd registered");

                if (spellType == null) {
                    return getOpResultsForKingdom(opType, kingdom, filters);
                } else {
                    return getSpellResultsForKingdom(spellType, kingdom, filters);
                }
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static CommandResponse getResultsFromProvince(final SpellType spellType,
                                                          final OpType opType,
                                                          final Province province) {
        int totalDamage = 0;
        int totalAttempts = 0;
        if (spellType != null) {
            ArrayList<InstantSpell> instantSpells = new ArrayList<>(province.getInstantSpells(spellType));
            for (InstantSpell spell : instantSpells) {
                totalDamage += spell.getDamage();
                totalAttempts += spell.getAmount();
            }
            return CommandResponse
                    .resultResponse("province", province, "type", spellType.getName(), "spellsAndOps", instantSpells, "totalDamage",
                            totalDamage, "totalAttempts", totalAttempts);
        } else {
            ArrayList<InstantOp> instantOps = new ArrayList<>(province.getInstantOps(opType));
            for (InstantOp op : instantOps) {
                totalDamage += op.getDamage();
                totalAttempts += op.getAmount();
            }
            return CommandResponse
                    .resultResponse("province", province, "type", opType.getName(), "spellsAndOps", instantOps, "totalDamage", totalDamage,
                            "totalAttempts", totalAttempts);
        }
    }

    private static CommandResponse getSpellResultsForKingdom(final SpellType spellType,
                                                             final Kingdom kingdom,
                                                             final Collection<Filter<?>> filters) {
        List<ProvinceWithInstantSpellsOrOps> list = new ArrayList<>();
        for (Province province : kingdom.getProvinces()) {
            Set<InstantSpell> instantSpells = province.getInstantSpells(spellType);
            if (!instantSpells.isEmpty())
                list.add(ProvinceWithInstantSpellsOrOps.create(province, new ArrayList<>(instantSpells)));
        }
        FilterUtil.applyFilters(list, filters, Province.class);
        return CommandResponse.resultResponse("columnLengths", compileColumnLengths(list), "infoList", list);
    }

    private static CommandResponse getOpResultsForKingdom(final OpType opType,
                                                          final Kingdom kingdom,
                                                          final Collection<Filter<?>> filters) {
        List<ProvinceWithInstantSpellsOrOps> list = new ArrayList<>();
        for (Province province : kingdom.getProvinces()) {
            Set<InstantOp> instantOps = province.getInstantOps(opType);
            if (!instantOps.isEmpty())
                list.add(ProvinceWithInstantSpellsOrOps.create(province, new ArrayList<>(instantOps)));
        }
        FilterUtil.applyFilters(list, filters, Province.class);
        return CommandResponse.resultResponse("columnLengths", compileColumnLengths(list), "infoList", list);
    }

    private static List<Integer> compileColumnLengths(final List<ProvinceWithInstantSpellsOrOps> list) {
        List<Integer> columnLengths = new ArrayList<>(3);
        int maxProvNameLength = 15, maxAttemptsLength = 15, maxDamageLength = 15;
        for (ProvinceWithInstantSpellsOrOps entry : list) {
            int nameLength = entry.getProvince().getName().length();
            if (nameLength > maxProvNameLength) maxProvNameLength = nameLength;

            int damageLength = String.valueOf(entry.getTotalDamage()).length();
            if (damageLength > maxDamageLength) maxDamageLength = damageLength;

            int attemptsLength = String.valueOf(entry.getTotalAttempts()).length();
            if (attemptsLength > maxAttemptsLength) maxAttemptsLength = attemptsLength;
        }
        columnLengths.add(maxProvNameLength);
        columnLengths.add(maxDamageLength);
        columnLengths.add(maxAttemptsLength);
        return columnLengths;
    }
}
