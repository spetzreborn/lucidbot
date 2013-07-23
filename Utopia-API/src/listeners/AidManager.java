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
import api.irc.communication.IRCAccess;
import api.runtime.ThreadingManager;
import api.timers.Timer;
import api.timers.TimerManager;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.AidDAO;
import database.daos.ProvinceDAO;
import database.models.Aid;
import database.models.Province;
import events.AidSentEvent;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import spi.timers.TimerRunOutHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static api.database.Transactions.inTransaction;

/**
 * Manages aid, meaning it takes care of loading timers for any aid requests with expiry dates on startup, and also
 * listens for when aid is sent and reacts to that (if enabled)
 */
@Log4j
public class AidManager implements TimerRunOutHandler, EventListener {
    private final ThreadingManager threadingManager;
    private final Provider<AidDAO> aidDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final IRCAccess ircAccess;
    private final TimerManager timerManager;

    @Inject
    public AidManager(final ThreadingManager threadingManager, final Provider<AidDAO> aidDAOProvider,
                      final Provider<ProvinceDAO> provinceDAOProvider, final IRCAccess ircAccess, final TimerManager timerManager) {
        this.threadingManager = threadingManager;
        this.aidDAOProvider = aidDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
        this.ircAccess = ircAccess;
        this.timerManager = timerManager;
    }

    @Subscribe
    public void onStartup(final StartupEvent startupEvent) {
        try {
            List<Aid> expired = new ArrayList<>();
            for (Aid aid : aidDAOProvider.get().getAllAid()) {
                if (aid.getExpiryDate() != null) {
                    long delay = aid.getExpiryDate().getTime() - System.currentTimeMillis();
                    if (delay <= 0) expired.add(aid);
                    else timerManager.schedule(new Timer(Aid.class, aid.getId(), this), delay, TimeUnit.MILLISECONDS);
                }
            }
            aidDAOProvider.get().delete(expired);
        } catch (HibernateException e) {
            AidManager.log.error("Could not load aid timers");
        }
    }

    @Override
    public void register(final long itemId) {
        try {
            AidDAO dao = aidDAOProvider.get();
            Aid aid = dao.getAid(itemId);
            if (aid != null) dao.delete(aid);
        } catch (HibernateException e) {
            AidManager.log.error("", e);
        }
    }

    @Subscribe
    public void onAidSentEvent(final AidSentEvent event) {
        threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                inTransaction(new SimpleTransactionTask() {
                    @Override
                    public void run(final DelayedEventPoster delayedEventPoster) {
                        try {
                            String reply = handleRegisteredAid(event);
                            if (reply != null) ircAccess.sendNoticeOrPM(event.getContext(), reply);
                        } catch (Exception e) {
                            log.error("Aid event could not be handled", e);
                        }
                    }
                });
            }
        });
    }

    private String handleRegisteredAid(final AidSentEvent event) {
        try {
            ProvinceDAO provinceDAO = provinceDAOProvider.get();
            Province province = provinceDAO.getProvince(event.getProvinceName());
            if (province == null) return "Could not find that province";

            Aid ofInterest = null;
            List<Aid> requests = province.getAid();
            for (Aid aid : requests) {
                if (aid.getType() == event.getAidType()) {
                    ofInterest = aid;
                    break;
                }
            }

            if (ofInterest != null) {
                String message;
                int amountLeft = ofInterest.getAmount() - event.getAmount();
                if (amountLeft > 0) {
                    ofInterest.setAmount(amountLeft);
                    message = "Your contribution of " + ofInterest.getType().getTypeName() + " has been saved!";
                } else {
                    requests.remove(ofInterest);
                    message = "Aid request for " + ofInterest.getType().getTypeName() + " removed!";
                }
                provinceDAO.save(province);
                return message;
            } else return null;
        } catch (HibernateException e) {
            AidManager.log.error("", e);
        }
        return "Something went wrong, so your aid was not registered";
    }
}
