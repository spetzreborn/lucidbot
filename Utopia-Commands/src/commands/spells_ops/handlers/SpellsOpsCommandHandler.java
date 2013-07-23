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
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import com.google.inject.Provider;
import database.CommonEntitiesAccess;
import database.daos.ProvinceDAO;
import database.models.*;
import events.DurationOpRegisteredEvent;
import events.DurationSpellRegisteredEvent;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.time.UtopiaTime;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;

public class SpellsOpsCommandHandler implements CommandHandler {
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;
    private final UtopiaTimeFactory utopiaTimeFactory;

    @Inject
    public SpellsOpsCommandHandler(final CommonEntitiesAccess commonEntitiesAccess, final Provider<ProvinceDAO> provinceDAOProvider,
                                   final UtopiaTimeFactory utopiaTimeFactory) {
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.provinceDAOProvider = provinceDAOProvider;
        this.utopiaTimeFactory = utopiaTimeFactory;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Province province = provinceDAOProvider.get().getProvince(params.getParameter("province").trim());
            if (province == null) return CommandResponse.errorResponse("Could not find such a province");

            String name = context.getCommand().getName();
            BotUser user = context.getBotUser();
            OpType opType = commonEntitiesAccess.getOpType(name);
            boolean isDuration = false;
            int amount = params.getIntParameter("amount");
            if (opType == null) {
                SpellType spellType = commonEntitiesAccess.getSpellType(name);
                if (spellType == null) return CommandResponse.errorResponse("Unknown spell/op");

                if (spellType.getSpellCharacter().isInstant()) {
                    province.registerInstantSpell(user, spellType, amount);
                    if (amount > 0) user.incrementStat(spellType.getName() + " damage", amount);
                } else {
                    UtopiaTime currentTime = utopiaTimeFactory.newUtopiaTime(System.currentTimeMillis());
                    UtopiaTime expires = currentTime.increment(amount + 1);
                    DurationSpell durationSpell = province
                            .addDurationSpell(new DurationSpell(user, province, new Date(expires.getTime()), spellType));
                    provinceDAOProvider.get().getProvince(province.getId()); //Cause auto flush
                    delayedEventPoster.enqueue(new DurationSpellRegisteredEvent(durationSpell.getId(), context));
                    isDuration = true;
                }
                if (spellType.getSpellCharacter() != SpellOpCharacter.SELF_SPELLOP) user.incrementStat(spellType.getName(), 1);
                return CommandResponse
                        .resultResponse("province", province, "result", params.getParameter("amount"), "type", spellType.getName(),
                                "isDuration", isDuration);
            } else {
                if (opType.getOpCharacter().isInstant()) {
                    province.registerInstantOp(user, opType, amount);
                    if (amount > 0) user.incrementStat(opType.getName() + " damage", amount);
                } else {
                    UtopiaTime currentTime = utopiaTimeFactory.newUtopiaTime(System.currentTimeMillis());
                    UtopiaTime expires = currentTime.increment(amount + 1);
                    DurationOp durationOp = province.addDurationOp(new DurationOp(user, province, new Date(expires.getTime()), opType));
                    provinceDAOProvider.get().getProvince(province.getId()); //Cause auto flush
                    delayedEventPoster.enqueue(new DurationOpRegisteredEvent(durationOp.getId(), context));
                    isDuration = true;
                }
                if (opType.getOpCharacter() != SpellOpCharacter.SELF_SPELLOP) user.incrementStat(opType.getName(), 1);
                return CommandResponse
                        .resultResponse("province", province, "result", params.getParameter("amount"), "type", opType.getName(),
                                "isDuration", isDuration);
            }
        } catch (Exception e) {
            throw new CommandHandlingException(e);
        }
    }
}
