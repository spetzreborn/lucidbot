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

package api.database.transactions;

import api.tools.database.DBUtil;
import lombok.extern.log4j.Log4j;
import org.hibernate.FlushMode;
import org.hibernate.Session;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class that manages transactions for the current thread. It can be used to control
 * the span of a transaction.
 */
@Log4j
public class TransactionManager {
    static final ThreadLocal<Boolean> HAS_LIVE_TRANSACTION = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    static final ThreadLocal<List<Runnable>> AFTER_COMMIT_ACTIONS = new ThreadLocal<List<Runnable>>() {
        @Override
        protected List<Runnable> initialValue() {
            return new ArrayList<>();
        }
    };
    private final Session session;
    private boolean started;
    private boolean encounteredException;
    private boolean onlyFlushOnCommit;

    @Inject
    TransactionManager(final Session session) {
        this.session = checkNotNull(session);
    }

    public void setOnlyFlushOnCommit(final boolean onlyFlushOnCommit) {
        this.onlyFlushOnCommit = onlyFlushOnCommit;
    }

    public static void addAfterCommitAction(final Runnable action) {
        AFTER_COMMIT_ACTIONS.get().add(action);
    }

    /**
     * Begins a transaction if one isn't already in progress for the current thread
     */
    public void beginTransaction() {
        if (!HAS_LIVE_TRANSACTION.get()) {
            session.beginTransaction();
            if (onlyFlushOnCommit) session.setFlushMode(FlushMode.COMMIT);
            HAS_LIVE_TRANSACTION.set(true);
            started = true;
        }
    }

    /**
     * Ends the current transaction, given that one is active and was started by this manager. Rollbacks if the commit fails or
     * if setEncounteredException was called with 'true'
     */
    public void endTransaction() {
        if (started && HAS_LIVE_TRANSACTION.get()) {
            HAS_LIVE_TRANSACTION.set(false);
            if (encounteredException) rollback();
            else commit();
        }
    }

    public void setEncounteredException(final boolean encounteredException) {
        this.encounteredException = encounteredException;
    }

    private void commit() {
        try {
            session.getTransaction().commit();
            DBUtil.closeSilently(session);
            runAfterCommitActions();
        } catch (final Exception e) {
            TransactionManager.log.error("Database exception, attempting rollback");
            rollback();
            throw e;
        }
    }

    private static void runAfterCommitActions() {
        try {
            for (Runnable afterCommitAction : AFTER_COMMIT_ACTIONS.get()) {
                afterCommitAction.run();
            }
        } finally {
            AFTER_COMMIT_ACTIONS.get().clear();
        }
    }

    private void rollback() {
        try {
            DBUtil.rollback(session);
        } finally {
            DBUtil.closeSilently(session);
            AFTER_COMMIT_ACTIONS.get().clear();
        }
    }
}
