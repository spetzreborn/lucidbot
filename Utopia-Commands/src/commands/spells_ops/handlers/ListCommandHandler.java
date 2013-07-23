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
import api.settings.PropertiesCollection;
import api.tools.collections.Params;
import api.tools.files.FilterUtil;
import commands.spells_ops.ProvinceWithDurationSpellsOrOps;
import database.daos.KingdomDAO;
import database.daos.OpDAO;
import database.daos.SpellDAO;
import database.models.*;
import lombok.extern.log4j.Log4j;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BestMatchFinder;

import javax.inject.Inject;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tools.UtopiaPropertiesConfig.SPELL_OP_MATRIX_COLUMNS;

@Log4j
public class ListCommandHandler implements CommandHandler {
    private final SpellDAO spellDAO;
    private final OpDAO opDAO;
    private final KingdomDAO kingdomDAO;
    private final BestMatchFinder bestMatchFinder;
    private final PropertiesCollection properties;

    @Inject
    public ListCommandHandler(final KingdomDAO kingdomDAO, final OpDAO opDAO, final SpellDAO spellDAO,
                              final BestMatchFinder bestMatchFinder, final PropertiesCollection properties) {
        this.kingdomDAO = kingdomDAO;
        this.opDAO = opDAO;
        this.spellDAO = spellDAO;
        this.bestMatchFinder = bestMatchFinder;
        this.properties = properties;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            boolean hasUserOrProv = params.containsKey("userOrProv");
            if ("*".equals(params.getParameter("spellOrOp"))) {
                return hasUserOrProv ? handleAllTypesForSpecifiedTarget(params.getParameter("userOrProv").trim())
                        : handleAllTypesForUnspecifiedTarget(params.getParameter("kingdom"),
                        properties.getList(SPELL_OP_MATRIX_COLUMNS, ","), filters);
            } else {
                SpellType spellType = spellDAO.getSpellType(params.getParameter("spellOrOp"));
                if (spellType == null) {
                    OpType opType = opDAO.getOpType(params.getParameter("spellOrOp"));
                    return hasUserOrProv ? handleOpTypeForSpecifiedTarget(opType, params.getParameter("userOrProv").trim())
                            : handleOpTypeForUnspecifiedTarget(opType, params.getParameter("kingdom"), filters);
                } else
                    return hasUserOrProv ? handleSpellTypeForSpecifiedTarget(spellType, params.getParameter("userOrProv").trim())
                            : handleSpellTypeForUnspecifiedTarget(spellType, params.getParameter("kingdom"), filters);
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private CommandResponse handleSpellTypeForUnspecifiedTarget(final SpellType spellType, final String kdLocation,
                                                                final Collection<Filter<?>> filters) {
        if (kdLocation == null) {
            List<DurationSpell> durationSpells = spellDAO.getDurationSpells(spellType);
            FilterUtil.applyFilters(durationSpells, filters, Province.class);
            Collections.sort(durationSpells);
            return CommandResponse.resultResponse("spellUnspecified", durationSpells, "type", spellType, "kdSpecified", false);
        } else {
            Kingdom kingdom = kingdomDAO.getKingdom(kdLocation);
            if (kingdom == null) return CommandResponse.errorResponse("Couldn't find kd");

            List<DurationSpell> durationSpells = spellDAO.getDurationSpells(kingdom, spellType);
            FilterUtil.applyFilters(durationSpells, filters, Province.class);
            Collections.sort(durationSpells);

            List<Province> missing = new ArrayList<>(kingdom.getProvinces());
            FilterUtil.applyFilters(missing, filters);
            for (DurationSpell durationSpell : durationSpells) {
                missing.remove(durationSpell.getProvince());
            }
            Collections.sort(missing);

            return CommandResponse
                    .resultResponse("spellUnspecified", durationSpells, "type", spellType, "kdSpecified", true, "missing", missing);
        }
    }

    private CommandResponse handleOpTypeForUnspecifiedTarget(final OpType opType, final String kdLocation,
                                                             final Collection<Filter<?>> filters) {
        if (kdLocation == null) {
            List<DurationOp> durationOps = opDAO.getDurationOps(opType);
            FilterUtil.applyFilters(durationOps, filters, Province.class);
            Collections.sort(durationOps);
            return CommandResponse.resultResponse("opUnspecified", durationOps, "type", opType, "kdSpecified", false);
        } else {
            Kingdom kingdom = kingdomDAO.getKingdom(kdLocation);
            if (kingdom == null) return CommandResponse.errorResponse("Couldn't find kd");

            List<DurationOp> durationOps = opDAO.getDurationOps(kingdom, opType);
            FilterUtil.applyFilters(durationOps, filters, Province.class);
            Collections.sort(durationOps);

            List<Province> missing = new ArrayList<>(kingdom.getProvinces());
            FilterUtil.applyFilters(missing, filters);
            for (DurationOp durationOp : durationOps) {
                missing.remove(durationOp.getProvince());
            }
            Collections.sort(missing);

            return CommandResponse.resultResponse("opUnspecified", durationOps, "type", opType, "kdSpecified", true, "missing", missing);
        }
    }


    private static final Pattern COLUMN_SPEC_PATTERN = Pattern.compile("(.+):(.+)");

    private CommandResponse handleAllTypesForUnspecifiedTarget(final String kdLocation,
                                                               final List<String> columnTypesInOrder,
                                                               final Collection<Filter<?>> filters) {
        List<String> columnSpellOpTypes = new ArrayList<>(columnTypesInOrder.size());
        List<String> columnNamesInMatrix = new ArrayList<>(columnTypesInOrder.size());
        extractColumnInfo(columnTypesInOrder, columnSpellOpTypes, columnNamesInMatrix);

        List<ProvinceWithDurationSpellsOrOps> list = new ArrayList<>();
        if (kdLocation == null) {
            Set<Province> provincesWithSpellsOrOps = new HashSet<>();
            for (DurationSpell spell : spellDAO.getDurationSpells()) {
                if (columnSpellOpTypes.contains(spell.getType().getName()))
                    provincesWithSpellsOrOps.add(spell.getProvince());
            }
            for (DurationOp op : opDAO.getDurationOps()) {
                if (columnSpellOpTypes.contains(op.getType().getName()))
                    provincesWithSpellsOrOps.add(op.getProvince());
            }
            for (Province province : provincesWithSpellsOrOps) {
                list.add(ProvinceWithDurationSpellsOrOps.create(columnSpellOpTypes, province));
            }
            FilterUtil.applyFilters(list, filters, Province.class);
            return CommandResponse.resultResponse("allUnspecified", list);
        } else {
            Kingdom kingdom = kingdomDAO.getKingdom(kdLocation);
            if (kingdom == null) return CommandResponse.errorResponse("Couldn't find kd");
            for (Province province : kingdom.getProvinces()) {
                list.add(ProvinceWithDurationSpellsOrOps.create(columnSpellOpTypes, province));
            }
            FilterUtil.applyFilters(list, filters, Province.class);
        }

        List<Integer> columnLengths = compileColumnLengths(columnTypesInOrder, list);

        return CommandResponse.resultResponse("columnLengths", columnLengths, "columnDefinitions", columnNamesInMatrix, "allUnspecified", list);
    }

    private static void extractColumnInfo(final List<String> columnTypesInOrder,
                                          final List<String> columnSpellOpTypes,
                                          final List<String> columnNamesInMatrix) {
        Matcher matcher;
        for (String columnDef : columnTypesInOrder) {
            matcher = COLUMN_SPEC_PATTERN.matcher(columnDef);
            if (matcher.matches()) {
                columnSpellOpTypes.add(matcher.group(1));
                columnNamesInMatrix.add(matcher.group(2));
            } else ListCommandHandler.log.warn("Column definition for spell/op matrix does not conform to the specified format " +
                    "<Spell/op type name>:<Name in matrix> :" + columnDef);
        }
    }

    private static List<Integer> compileColumnLengths(final List<String> columnTypesInOrder,
                                                      final List<ProvinceWithDurationSpellsOrOps> list) {
        List<Integer> columnLengths = new ArrayList<>(columnTypesInOrder.size() + 1);
        int maxProvNameLength = 15;
        for (ProvinceWithDurationSpellsOrOps entry : list) {
            int nameLength = entry.getProvince().getName().length();
            if (nameLength > maxProvNameLength) {
                maxProvNameLength = nameLength;
            }
        }
        columnLengths.add(maxProvNameLength);
        for (int i = 0; i < columnTypesInOrder.size(); ++i) {
            columnLengths.add(4);
        }
        return columnLengths;
    }

    private CommandResponse handleSpellTypeForSpecifiedTarget(final SpellType spellType, final String userOrProv) {
        Province province = bestMatchFinder.findBestMatch(userOrProv);
        if (province == null) return CommandResponse.errorResponse("No matching user/province found");
        DurationSpell spell = province.getDurationSpell(spellType);
        return spell == null ? CommandResponse.errorResponse("That spell isn't up at the moment")
                : CommandResponse.resultResponse("spellSpecified", spell, "type", spellType);
    }

    private CommandResponse handleOpTypeForSpecifiedTarget(final OpType opType, final String userOrProv) {
        Province province = bestMatchFinder.findBestMatch(userOrProv);
        if (province == null) return CommandResponse.errorResponse("No matching user/province found");
        DurationOp op = province.getDurationOp(opType);
        return op == null ? CommandResponse.errorResponse("That op isn't up at the moment")
                : CommandResponse.resultResponse("opSpecified", op, "type", opType);
    }

    private CommandResponse handleAllTypesForSpecifiedTarget(final String userOrProv) {
        Province province = bestMatchFinder.findBestMatch(userOrProv);
        if (province == null) return CommandResponse.errorResponse("No matching user/province found");

        Set<DurationSpell> durationSpells = new TreeSet<>(province.getDurationSpells());
        Set<DurationOp> durationOps = new TreeSet<>(province.getDurationOps());

        if (durationSpells.isEmpty() && durationOps.isEmpty())
            return CommandResponse.errorResponse("No spells or ops are up for that province");

        return CommandResponse.resultResponse("allSpellsSpecified", durationSpells, "allOpsSpecified", durationOps,
                "province", province);
    }
}
