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

package api.timers;

import api.runtime.ThreadingManager;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A manager for timers, capable of scheduling and canceling the tasks
 */
@ParametersAreNonnullByDefault
@Singleton
public final class TimerManager {
    private final ThreadingManager threadingManager;
    private final ConcurrentMap<String, ScheduledFuture<?>> currentlyScheduled = new ConcurrentHashMap<>();

    @Inject
    public TimerManager(final ThreadingManager threadingManager) {
        this.threadingManager = checkNotNull(threadingManager);
    }

    /**
     * Schedules a task on a timer, with the specified delay. Cancels any existing timer with the same characteristics first
     *
     * @param task  the task to schedule as a timer
     * @param delay the delay
     * @param unit  the unit of the delay
     * @return a ScheduledFuture for the Timer
     */
    public ScheduledFuture<?> schedule(final Timer task, final long delay, final TimeUnit unit) {
        String uniqueId = task.getItemType().getSimpleName() + ' ' + task.getItemId();
        ScheduledFuture<?> old = currentlyScheduled.remove(uniqueId);
        if (old != null) old.cancel(false);
        ScheduledFuture<?> sf = threadingManager.schedule(getProxiedRunnable(task, uniqueId), delay, unit);
        currentlyScheduled.put(uniqueId, sf);
        return sf;
    }

    /**
     * Schedules a task on a timer, with the specified delay and then recurring every specified period
     *
     * @param task         the task to schedule as a timer
     * @param initialDelay the delay
     * @param period       the interval for the recurring execution
     * @param unit         the unit of the delay and period
     * @return a ScheduledFuture for the Timer
     */
    public ScheduledFuture<?> scheduleRecurring(final Timer task, final long initialDelay, final long period, final TimeUnit unit) {
        return threadingManager.scheduleRecurring(task, initialDelay, period, unit);
    }

    /**
     * Cancels the timer operating on the specified type of item with the specified id
     *
     * @param itemType the type of item the Timer task is acting on
     * @param itemId   the id of the item
     */
    public void cancelTimer(final Class<?> itemType, final long itemId) {
        String uniqueId = itemType.getSimpleName() + ' ' + itemId;
        ScheduledFuture<?> sf = currentlyScheduled.remove(uniqueId);
        if (sf != null) sf.cancel(false);
    }

    /**
     * Cancels the timer with the specified id
     *
     * @param timerId the unique id of the timer
     */
    public void cancelTimer(final String timerId) {
        ScheduledFuture<?> sf = currentlyScheduled.remove(timerId);
        if (sf != null) sf.cancel(false);
    }

    public Set<String> getCurrentTimers() {
        return Collections.unmodifiableSet(currentlyScheduled.keySet());
    }

    public long getTimeLeft(final String timerId, final TimeUnit timeUnit) {
        ScheduledFuture<?> scheduledFuture = currentlyScheduled.get(timerId);
        return scheduledFuture == null ? -1 : scheduledFuture.getDelay(timeUnit);
    }

    private Runnable getProxiedRunnable(final Runnable runnable, final String id) {
        return (Runnable) Proxy
                .newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{Runnable.class}, new RunHandler(runnable, id, this));
    }

    @ParametersAreNonnullByDefault
    private static class RunHandler implements InvocationHandler {
        private final Runnable runnable;
        private final String id;
        private final TimerManager manager;

        private RunHandler(final Runnable runnable, final String id, final TimerManager manager) {
            this.runnable = checkNotNull(runnable);
            this.id = checkNotNull(id);
            this.manager = checkNotNull(manager);
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            runnable.run();
            manager.currentlyScheduled.remove(id);
            return null;
        }
    }
}
