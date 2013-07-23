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

import api.events.bot.StartupEvent;
import api.timers.Timer;
import api.timers.TimerManager;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.AlarmDAO;
import database.models.Alarm;
import events.AlarmSetOffEvent;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import spi.timers.TimerRunOutHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A manager of alarm timers, which announces when alarms are set off and also loads the alarm timers on startup
 */
@Log4j
public class AlarmManager implements TimerRunOutHandler, EventListener {
    private final Provider<AlarmDAO> alarmDAOProvider;
    private final EventBus eventBus;
    private final TimerManager timerManager;

    @Inject
    public AlarmManager(final Provider<AlarmDAO> alarmDAOProvider, final EventBus eventBus, final TimerManager timerManager) {
        this.alarmDAOProvider = alarmDAOProvider;
        this.eventBus = eventBus;
        this.timerManager = timerManager;
    }

    @Subscribe
    public void onStartup(final StartupEvent startupEvent) {
        try {
            List<Alarm> expired = new ArrayList<>();
            for (Alarm alarm : alarmDAOProvider.get().getAllAlarms()) {
                long delay = alarm.getAlarmTime().getTime() - System.currentTimeMillis();
                if (delay < 0) expired.add(alarm);
                else timerManager.schedule(new Timer(Alarm.class, alarm.getId(), this), delay, TimeUnit.MILLISECONDS);
            }
            alarmDAOProvider.get().delete(expired);
        } catch (HibernateException e) {
            AlarmManager.log.error("Could not load alarm timers");
        }
    }

    @Override
    public void register(final long itemId) {
        try {
            AlarmDAO alarmDAO = alarmDAOProvider.get();
            Alarm alarm = alarmDAO.getAlarm(itemId);
            if (alarm != null) {
                alarmDAO.delete(alarm);
                eventBus.post(new AlarmSetOffEvent(alarm));
            }
        } catch (Exception e) {
            AlarmManager.log.error("", e);
        }
    }

    public Alarm saveAlarm(final Alarm alarm) {
        long delay = alarm.getAlarmTime().getTime() - System.currentTimeMillis();
        if (delay <= 0) throw new IllegalArgumentException("Can't set an alarm for the past");
        Alarm saved = alarmDAOProvider.get().save(alarm);
        timerManager.schedule(new Timer(Alarm.class, alarm.getId(), this), delay, TimeUnit.MILLISECONDS);
        return saved;
    }
}
