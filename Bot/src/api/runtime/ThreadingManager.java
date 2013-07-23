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

package api.runtime;

import spi.runtime.RequiresShutdown;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.*;

/**
 * Manager that offers scheduling and execution of tasks
 */
@ParametersAreNonnullByDefault
public final class ThreadingManager implements RequiresShutdown {
    private final ScheduledThreadPoolExecutor scheduledExecutor;
    private final ThreadPoolExecutor infiniteTasksExecutor;

    public ThreadingManager(final int workerThreads) {
        scheduledExecutor = new ScheduledThreadPoolExecutor(workerThreads);
        infiniteTasksExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    }

    public <E> ScheduledFuture<E> schedule(final Callable<E> task, final long delay, final TimeUnit timeUnit) {
        return scheduledExecutor.schedule(task, delay, timeUnit);
    }

    public ScheduledFuture<?> schedule(final Runnable task, final long delay, final TimeUnit timeUnit) {
        return scheduledExecutor.schedule(task, delay, timeUnit);
    }

    public ScheduledFuture<?> scheduleRecurring(final Runnable task, final long delay, final long period, final TimeUnit timeUnit) {
        return scheduledExecutor.scheduleAtFixedRate(task, delay, period, timeUnit);
    }

    public <T> Future<T> submit(final Runnable task, final T result) {
        return scheduledExecutor.submit(task, result);
    }

    public <T> Future<T> submit(final Callable<T> task) {
        return scheduledExecutor.submit(task);
    }

    public Future<?> submitInfiniteTask(final Runnable task) {
        return infiniteTasksExecutor.submit(task);
    }

    public void execute(final Runnable task) {
        scheduledExecutor.execute(task);
    }

    @Override
    public Runnable getShutdownRunner() {
        return new Runnable() {
            @Override
            public void run() {
                scheduledExecutor.shutdown();
                infiniteTasksExecutor.shutdown();
            }
        };
    }

    public int totalThreads() {
        return scheduledExecutor.getPoolSize() + infiniteTasksExecutor.getPoolSize();
    }

    public int threadsUsed() {
        return scheduledExecutor.getActiveCount() + infiniteTasksExecutor.getActiveCount();
    }
}
