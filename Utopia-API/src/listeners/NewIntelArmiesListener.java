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
import api.timers.Timer;
import api.timers.TimerManager;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.IntelDAO;
import database.models.Army;
import database.models.SoM;
import events.SoMSavedEvent;
import lombok.extern.log4j.Log4j;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static api.database.Transactions.inTransaction;

@Log4j
class NewIntelArmiesListener implements EventListener {
    private final Provider<IntelDAO> intelDAOProvider;
    private final TimerManager timerManager;
    private final ArmyManager armyManager;

    @Inject
    NewIntelArmiesListener(final Provider<IntelDAO> intelDAOProvider, final TimerManager timerManager, final ArmyManager armyManager) {
        this.intelDAOProvider = intelDAOProvider;
        this.timerManager = timerManager;
        this.armyManager = armyManager;
    }

    @Subscribe
    public void onSoMAdded(final SoMSavedEvent event) {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                SoM soM = intelDAOProvider.get().getSoM(event.getId());

                if (soM != null && !soM.getArmiesOut().isEmpty()) {
                    long currentTime = System.currentTimeMillis();
                    long delay;
                    for (Army army : soM.getArmiesOut()) {
                        delay = army.getReturningDate().getTime() - currentTime;
                        timerManager.schedule(new Timer(Army.class, army.getId(), armyManager), delay, TimeUnit.MILLISECONDS);
                    }
                }
            }
        });
    }
}
