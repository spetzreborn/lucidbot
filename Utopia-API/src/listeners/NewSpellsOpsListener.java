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

package listeners;

import api.database.CallableTransactionTask;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.irc.communication.IRCAccess;
import api.runtime.ThreadingManager;
import api.tools.collections.Pair;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.OpDAO;
import database.daos.ProvinceDAO;
import database.daos.SpellDAO;
import database.models.*;
import events.*;
import lombok.extern.log4j.Log4j;
import spi.events.EventListener;
import tools.time.UtopiaTime;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.Date;

import static api.database.Transactions.inTransaction;

@Log4j
class NewSpellsOpsListener implements EventListener {
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<SpellDAO> spellDAOProvider;
    private final Provider<OpDAO> opDAOProvider;
    private final EventBus eventBus;
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final ThreadingManager threadingManager;
    private final IRCAccess ircAccess;

    @Inject
    NewSpellsOpsListener(final Provider<BotUserDAO> botUserDAOProvider, final Provider<ProvinceDAO> provinceDAOProvider,
                         final Provider<SpellDAO> spellDAOProvider, final Provider<OpDAO> opDAOProvider, final EventBus eventBus,
                         final UtopiaTimeFactory utopiaTimeFactory, final ThreadingManager threadingManager, final IRCAccess ircAccess) {
        this.botUserDAOProvider = botUserDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
        this.spellDAOProvider = spellDAOProvider;
        this.opDAOProvider = opDAOProvider;
        this.eventBus = eventBus;
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.threadingManager = threadingManager;
        this.ircAccess = ircAccess;
    }

    @Subscribe
    public void onOpAdded(final OpPastedEvent event) {
        threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                String reply = saveOp(event);
                ircAccess.sendNoticeOrPM(event.getContext(), reply);
            }
        });
    }

    private String saveOp(final OpPastedEvent event) {
        try {
            Pair<InstantOp, DurationOp> savedOpPair = inTransaction(new CallableTransactionTask<Pair<InstantOp, DurationOp>>() {
                @Override
                public Pair<InstantOp, DurationOp> call(final DelayedEventPoster delayedEventPoster) {
                    BotUser user = botUserDAOProvider.get().getUser(event.getContext().getBotUser().getId());
                    ProvinceDAO provinceDAO = provinceDAOProvider.get();
                    Province target = provinceDAO.getProvince(event.getProvinceId());
                    if (target == null) throw new IllegalStateException("Could not find the province");
                    OpDAO opDAO = opDAOProvider.get();
                    OpType opType = opDAO.getOpType(event.getOpTypeId());
                    if (opType.getOpCharacter().isInstant()) {
                        InstantOp instantOp = target.registerInstantOp(user, opType, event.getResult());
                        provinceDAO.save(target);
                        if (event.getResult() > 0) user.incrementStat(opType.getName() + " damage", event.getResult());
                        user.incrementStat(opType.getName(), 1);
                        return new Pair<>(instantOp, null);
                    } else {
                        UtopiaTime currentTime = utopiaTimeFactory.newUtopiaTime(System.currentTimeMillis());
                        UtopiaTime expires = currentTime.increment(event.getResult() + 1);
                        DurationOp durationOp = target.addDurationOp(new DurationOp(user, target, new Date(expires.getTime()), opType));
                        if (opType.getOpCharacter() != SpellOpCharacter.SELF_SPELLOP) user.incrementStat(opType.getName(), 1);
                        return new Pair<>(null, durationOp);
                    }
                }
            });

            if (savedOpPair.getLeft() == null)
                eventBus.post(new DurationOpRegisteredEvent(savedOpPair.getRight().getId(), event.getContext()));
            else eventBus.post(new InstantOpRegisteredEvent(savedOpPair.getLeft().getId(), event.getContext()));
            return "Op saved successfully";
        } catch (Exception e) {
            NewSpellsOpsListener.log.error("Error while attempting to handle pasted op", e);
            return "Op could not be saved";
        }
    }

    @Subscribe
    public void onSpellAdded(final SpellPastedEvent event) {
        threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                String reply = saveSpell(event);
                ircAccess.sendNoticeOrPM(event.getContext(), reply);
            }
        });
    }

    private String saveSpell(final SpellPastedEvent event) {
        try {
            Pair<InstantSpell, DurationSpell> savedSpellPair = inTransaction(
                    new CallableTransactionTask<Pair<InstantSpell, DurationSpell>>() {
                        @Override
                        public Pair<InstantSpell, DurationSpell> call(final DelayedEventPoster delayedEventPoster) {
                            BotUser user = botUserDAOProvider.get().getUser(event.getContext().getBotUser().getId());
                            ProvinceDAO provinceDAO = provinceDAOProvider.get();
                            Province target = provinceDAO.getProvince(event.getProvinceId());
                            if (target == null) throw new IllegalStateException("Could not find the province");
                            SpellDAO spellDAO = spellDAOProvider.get();
                            SpellType spellType = spellDAO.getSpellType(event.getSpellTypeId());
                            if (spellType.getSpellCharacter().isInstant()) {
                                InstantSpell instantSpell = target.registerInstantSpell(user, spellType, event.getResult());
                                provinceDAO.save(target);
                                if (event.getResult() > 0) user.incrementStat(spellType.getName() + " damage", event.getResult());
                                user.incrementStat(spellType.getName(), 1);
                                return new Pair<>(instantSpell, null);
                            } else {
                                UtopiaTime currentTime = utopiaTimeFactory.newUtopiaTime(System.currentTimeMillis());
                                UtopiaTime expires = currentTime.increment(event.getResult() + 1);
                                DurationSpell durationSpell = target
                                        .addDurationSpell(new DurationSpell(user, target, new Date(expires.getTime()), spellType));
                                if (spellType.getSpellCharacter() != SpellOpCharacter.SELF_SPELLOP)
                                    user.incrementStat(spellType.getName(), 1);
                                return new Pair<>(null, durationSpell);
                            }
                        }
                    });

            if (savedSpellPair.getLeft() == null)
                eventBus.post(new DurationSpellRegisteredEvent(savedSpellPair.getRight().getId(), event.getContext()));
            else eventBus.post(new InstantSpellRegisteredEvent(savedSpellPair.getLeft().getId(), event.getContext()));
            return "Spell saved successfully";
        } catch (Exception e) {
            NewSpellsOpsListener.log.error("Error while attempting to handle pasted spell", e);
            return "Spell could not be saved";
        }
    }
}
