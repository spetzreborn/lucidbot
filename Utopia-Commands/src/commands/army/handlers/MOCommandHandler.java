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

package commands.army.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.numbers.NumberUtil;
import api.tools.text.StringUtil;
import database.daos.ArmyDAO;
import database.daos.AttackDAO;
import database.models.Army;
import database.models.Attack;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class MOCommandHandler implements CommandHandler {
    private final ArmyDAO armyDAO;
    private final AttackDAO attackDAO;

    @Inject
    public MOCommandHandler(final ArmyDAO armyDAO, final AttackDAO attackDAO) {
        this.armyDAO = armyDAO;
        this.attackDAO = attackDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            int armyno = params.containsKey("armyno") ? params.getIntParameter("armyno") : -1;
            int gens = params.containsKey("generals") ? params.getIntParameter("generals") : -1;
            return saveInfo(context.getBotUser(), armyno, params.getParameter("mo"), gens);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private CommandResponse saveInfo(final BotUser user, final int armyno, final String mo, final int generals) {
        List<Army> armies = armyDAO.getIRCArmiesForUser(user);
        if (armies == null || armies.isEmpty()) return CommandResponse.errorResponse("You have no armies registered");
        Army toEdit = armyno < 0 ? getLastAddedArmy(armies) : getArmyToEdit(armyno, armies);
        if (toEdit == null) return CommandResponse.errorResponse("Could not find the army");
        if (generals > 0) toEdit.setGenerals(generals);
        Double modOff = NumberUtil.parseDouble(mo.replace('k', ' ').trim());
        modOff *= Math.pow(1000, StringUtil.countOccurance(mo, "k"));
        toEdit.setModOffense(modOff.intValue());
        toEdit = armyDAO.save(toEdit);
        saveMoToLastMadeAttack(user, modOff);
        return CommandResponse.resultResponse("army", toEdit);
    }

    private static Army getArmyToEdit(final int armyno, final Iterable<Army> candidates) {
        for (Army army : candidates) {
            if (army.getArmyNumber() == armyno) return army;
        }
        return null;
    }

    private static Army getLastAddedArmy(final List<Army> candidates) {
        Army out = candidates.get(0);
        for (Army army : candidates) {
            if (army.getId() > out.getId()) out = army;
        }
        return out;
    }

    private void saveMoToLastMadeAttack(final BotUser user, final Double mo) {
        Attack lastHitMadeByUser = attackDAO.getLastHitMadeByUser(user);
        if (lastHitMadeByUser != null) {
            lastHitMadeByUser.setOffenseSent(mo.intValue());
        }
    }
}
