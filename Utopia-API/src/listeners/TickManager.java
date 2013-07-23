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
import events.TickEvent;
import spi.events.EventListener;
import spi.timers.TimerRunOutHandler;
import tools.time.UtopiaTime;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * A class that manages tick timers, meaning it announces when the timer runs out causing a tick to happen
 */
public class TickManager implements TimerRunOutHandler, EventListener {
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final EventBus eventBus;
    private final TimerManager timerManager;

    private long lastTickEvent;
    private long lastTickId;

    @Inject
    public TickManager(final UtopiaTimeFactory utopiaTimeFactory, final EventBus eventBus, final TimerManager timerManager) {
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.eventBus = eventBus;
        this.timerManager = timerManager;
    }

    @Subscribe
    public void onStartup(final StartupEvent startupEvent) {
        scheduleNextTick();
    }

    private UtopiaTime scheduleNextTick() {
        UtopiaTime utopiaTime = utopiaTimeFactory.newUtopiaTime(System.currentTimeMillis());
        UtopiaTime nextTick = utopiaTime.increment(1);

        if (utopiaTime.getTime() == lastTickEvent) { //System time has been set back, so we're a bit early
            utopiaTime = nextTick;
            nextTick = nextTick.increment(1);
        }
        lastTickEvent = utopiaTime.getTime();

        timerManager.schedule(new Timer(TickEvent.class, lastTickId++, this), nextTick.getTime() - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS);
        return utopiaTime;
    }

    @Override
    public void register(final long itemId) {
        UtopiaTime thisTick = scheduleNextTick();
        eventBus.post(new TickEvent(thisTick));
    }
}
