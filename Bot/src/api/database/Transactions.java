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

package api.database;

import api.events.DelayedEventPoster;
import com.google.inject.Provider;

import javax.inject.Inject;

public class Transactions {
    @Inject
    private static Provider<TransactionManager> transactionManagerProvider;
    @Inject
    private static Provider<DelayedEventPoster> delayedEventPosterProvider;

    private Transactions() {
    }

    /**
     * Runs the specified task inside a database transaction
     *
     * @param task the task to execute in the transaction
     * @throws RuntimeException any runtime exceptions the task throws
     */
    public static void inTransaction(final SimpleTransactionTask task) {
        inTransaction(task, false);
    }

    /**
     * Runs the specified task inside a database transaction
     *
     * @param task              the task to execute in the transaction
     * @param onlyFlushOnCommit whether to hold flushing until the commit
     * @throws RuntimeException any runtime exceptions the task throws
     */
    public static void inTransaction(final SimpleTransactionTask task, final boolean onlyFlushOnCommit) {
        TransactionManager transactionManager = transactionManagerProvider.get();
        transactionManager.setOnlyFlushOnCommit(onlyFlushOnCommit);
        transactionManager.beginTransaction();
        DelayedEventPoster delayedEventBus = delayedEventPosterProvider.get();
        try {
            task.run(delayedEventBus);
        } catch (final RuntimeException e) {
            transactionManager.setEncounteredException(true);
            throw e;
        } finally {
            transactionManager.endTransaction();
        }
        delayedEventBus.execute();
    }

    /**
     * Runs the specified task inside a database transaction, and then returns the result
     *
     * @param task the task to execute in the transaction
     * @param <E>  the return type
     * @return the result of the task
     * @throws Exception whatever exception the task throws is propagated
     */
    public static <E> E inTransaction(final CallableTransactionTask<E> task) throws Exception {
        return inTransaction(task, false);
    }

    /**
     * Runs the specified task inside a database transaction, and then returns the result
     *
     * @param task              the task to execute in the transaction
     * @param onlyFlushOnCommit whether to hold flushing until the commit
     * @param <E>               the return type
     * @return the result of the task
     * @throws Exception whatever exception the task throws is propagated
     */
    public static <E> E inTransaction(final CallableTransactionTask<E> task, final boolean onlyFlushOnCommit) throws Exception {
        TransactionManager transactionManager = transactionManagerProvider.get();
        transactionManager.setOnlyFlushOnCommit(onlyFlushOnCommit);
        transactionManager.beginTransaction();
        DelayedEventPoster delayedEventPoster = delayedEventPosterProvider.get();
        E result;
        try {
            result = task.call(delayedEventPoster);
        } catch (final Exception e) {
            transactionManager.setEncounteredException(true);
            throw e;
        } finally {
            transactionManager.endTransaction();
        }
        delayedEventPoster.execute();
        return result;
    }
}
