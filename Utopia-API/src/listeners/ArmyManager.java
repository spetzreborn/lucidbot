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

import api.database.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import api.events.bot.StartupEvent;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import api.timers.Timer;
import api.timers.TimerManager;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.ArmyDAO;
import database.models.Army;
import events.ArmyAddedEvent;
import events.ArmyHomeEvent;
import lombok.extern.log4j.Log4j;
import spi.events.EventListener;
import spi.timers.TimerRunOutHandler;
import tools.UtopiaPropertiesConfig;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

import static api.database.Transactions.inTransaction;
import static tools.UtopiaPropertiesConfig.TIMERS_ANNOUNCE_ENEMY_ARMIES;

/**
 * A manager of army timers, which announces when armies return
 */
@Log4j
public class ArmyManager implements TimerRunOutHandler, EventListener {
    private final Provider<ArmyDAO> armyDAOProvider;
    private final TimerManager timerManager;
    private final boolean announceEnemyArmies;
    private final PropertiesCollection properties;

    @Inject
    public ArmyManager(final Provider<ArmyDAO> armyDAOProvider,
                       final TimerManager timerManager,
                       @Named(TIMERS_ANNOUNCE_ENEMY_ARMIES) final boolean announceEnemyArmies,
                       final PropertiesCollection properties) {
        this.armyDAOProvider = armyDAOProvider;
        this.timerManager = timerManager;
        this.announceEnemyArmies = announceEnemyArmies;
        this.properties = properties;
    }

    @Subscribe
    public void onStartup(final StartupEvent startupEvent) {
        try {
            ArmyDAO armyDAO = armyDAOProvider.get();
            armyDAO.clearReturnedArmies();
            for (Army army : armyDAO.getArmiesForTimerAdding()) {
                long delay = army.getReturningDate().getTime() - System.currentTimeMillis();
                timerManager.schedule(new Timer(Army.class, army.getId(), this), delay, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            ArmyManager.log.error("Could not load army timers", e);
        }
    }

    @Override
    public void register(final long itemId) {
        try {
            inTransaction(new SimpleTransactionTask() {
                @Override
                public void run(final DelayedEventPoster delayedEventBus) {
                    ArmyDAO armyDAO = armyDAOProvider.get();
                    Army army = armyDAO.getArmy(itemId);
                    if (army != null) {
                        if (army.getType() == Army.ArmyType.IRC_ARMY_OUT) armyDAO.delete(army);
                        else armyDAO.deleteIntelArmy(army);

                        String selfKD = properties.get(UtopiaPropertiesConfig.INTRA_KD_LOC);
                        if (army.getType() == Army.ArmyType.IRC_ARMY_OUT ||
                                announceEnemyArmies && !army.getKingdom().getLocation().equals(selfKD))
                            delayedEventBus.enqueue(new ArmyHomeEvent(army, army.getProvince().getProvinceOwner() != null));
                    }
                }
            });
        } catch (Exception e) {
            ArmyManager.log.error("", e);
        }
    }

    public Army saveIRCArmy(final Army army, final IRCContext context, final DelayedEventPoster delayedEventPoster) {
        ArmyDAO armyDAO = armyDAOProvider.get();
        if (army.getArmyNumber() == -1) army.setArmyNumber(armyDAO.getFirstAvailableArmyNo(army.getProvince()));

        Army saved;
        Army existing = armyDAO.getArmy(army.getProvince().getProvinceOwner(), army.getArmyNumber());
        if (existing == null || existing.getType() != army.getType()) {
            saved = armyDAO.save(army);
        } else {
            saved = existing;
            saved.setReturningDate(army.getReturningDate());
            saved.setLandGained(army.getLandGained());
        }

        saved = armyDAO.save(saved);
        timerManager.schedule(new Timer(Army.class, saved.getId(), this), saved.getReturningDate().getTime() - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS);
        delayedEventPoster.enqueue(new ArmyAddedEvent(saved.getId(), context));
        return saved;
    }
}
