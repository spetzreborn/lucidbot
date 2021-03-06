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
import database.daos.KingdomDAO;
import database.daos.OpDAO;
import database.daos.SpellDAO;
import database.models.Kingdom;
import database.models.OpType;
import database.models.Province;
import database.models.SpellType;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BestMatchFinder;

import javax.inject.Inject;
import java.util.Collection;

public class ResetCommandHandler implements CommandHandler {
    private final SpellDAO spellDAO;
    private final OpDAO opDAO;
    private final KingdomDAO kingdomDAO;
    private final BestMatchFinder bestMatchFinder;

    @Inject
    public ResetCommandHandler(final KingdomDAO kingdomDAO, final OpDAO opDAO, final SpellDAO spellDAO,
                               final BestMatchFinder bestMatchFinder) {
        this.kingdomDAO = kingdomDAO;
        this.opDAO = opDAO;
        this.spellDAO = spellDAO;
        this.bestMatchFinder = bestMatchFinder;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            boolean hasUserOrProv = params.containsKey("userOrProv");
            if ("*".equals(params.getParameter("spellOrOp"))) {
                return hasUserOrProv ? handleAllTypes(params.getParameter("userOrProv").trim())
                        : handleAllTypesForUnspecifiedTarget(params.getParameter("kingdom"));
            } else {
                SpellType spellType = spellDAO.getSpellType(params.getParameter("spellOrOp"));
                if (spellType == null) {
                    OpType opType = opDAO.getOpType(params.getParameter("spellOrOp"));
                    return hasUserOrProv ? handleOpType(opType, params.getParameter("userOrProv").trim())
                            : handleOpTypeForUnspecifiedTarget(opType, params.getParameter("kingdom"));
                } else return hasUserOrProv ? handleSpellType(spellType, params.getParameter("userOrProv").trim())
                        : handleSpellTypeForUnspecifiedTarget(spellType, params.getParameter("kingdom"));
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private CommandResponse handleSpellTypeForUnspecifiedTarget(final SpellType spellType, final String kdLocation) {
        if (kdLocation == null) {
            spellDAO.deleteDurationSpells(spellType);
            spellDAO.deleteInstantSpells(spellType);
        } else {
            Kingdom kingdom = kingdomDAO.getKingdom(kdLocation);
            if (kingdom == null) return CommandResponse.errorResponse("Couldn't find kd");
            for (Province province : kingdom.getProvinces()) {
                province.removeDurationSpell(spellType);
                province.clearInstantSpells(spellType);
            }
        }
        return CommandResponse.resultResponse("result", "");
    }

    private CommandResponse handleOpTypeForUnspecifiedTarget(final OpType opType, final String kdLocation) {
        if (kdLocation == null) {
            opDAO.deleteDurationOps(opType);
            opDAO.deleteInstantOps(opType);
        } else {
            Kingdom kingdom = kingdomDAO.getKingdom(kdLocation);
            if (kingdom == null) return CommandResponse.errorResponse("Couldn't find kd");
            for (Province province : kingdom.getProvinces()) {
                province.removeDurationOp(opType);
                province.clearInstantOps(opType);
            }
        }
        return CommandResponse.resultResponse("result", "");
    }

    private CommandResponse handleAllTypesForUnspecifiedTarget(final String kdLocation) {
        if (kdLocation == null) {
            spellDAO.deleteDurationSpells();
            spellDAO.deleteInstantSpells();
            opDAO.deleteDurationOps();
            opDAO.deleteInstantOps();
        } else {
            Kingdom kingdom = kingdomDAO.getKingdom(kdLocation);
            if (kingdom == null) return CommandResponse.errorResponse("Couldn't find kd");
            for (Province province : kingdom.getProvinces()) {
                province.removeDurationSpells();
                province.clearInstantSpells();
                province.removeDurationOps();
                province.clearInstantOps();
            }
        }
        return CommandResponse.resultResponse("result", "");
    }

    private CommandResponse handleSpellType(final SpellType spellType, final String userOrProv) {
        Province province = bestMatchFinder.findBestMatch(userOrProv);
        if (province == null) return CommandResponse.errorResponse("No matching user/province found");
        province.removeDurationSpell(spellType);
        province.clearInstantSpells(spellType);
        return CommandResponse.resultResponse("result", "");
    }

    private CommandResponse handleOpType(final OpType opType, final String userOrProv) {
        Province province = bestMatchFinder.findBestMatch(userOrProv);
        if (province == null) return CommandResponse.errorResponse("No matching user/province found");
        province.removeDurationOp(opType);
        province.clearInstantOps(opType);
        return CommandResponse.resultResponse("result", "");
    }

    private CommandResponse handleAllTypes(final String userOrProv) {
        Province province = bestMatchFinder.findBestMatch(userOrProv);
        if (province == null) return CommandResponse.errorResponse("No matching user/province found");
        province.removeDurationSpells();
        province.clearInstantSpells();
        province.removeDurationOps();
        province.clearInstantOps();
        return CommandResponse.resultResponse("result", "");
    }
}
