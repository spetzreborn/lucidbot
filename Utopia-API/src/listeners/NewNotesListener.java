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

import api.database.transactions.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import api.runtime.ThreadingManager;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.NoteDAO;
import database.daos.NotificationDAO;
import database.models.Note;
import database.models.Notification;
import database.models.NotificationType;
import events.NoteAddedEvent;
import lombok.extern.log4j.Log4j;
import spi.events.EventListener;
import tools.BindingsManager;
import tools.communication.NotificationDeliverer;

import javax.inject.Inject;

import static api.database.transactions.Transactions.inTransaction;

@Log4j
class NewNotesListener implements EventListener {
    private final Provider<NoteDAO> noteDAOProvider;
    private final BindingsManager bindingsManager;
    private final Provider<NotificationDAO> notificationDAOProvider;
    private final Provider<NotificationDeliverer> delivererProvider;
    private final ThreadingManager threadingManager;

    @Inject
    NewNotesListener(final Provider<NotificationDAO> notificationDAOProvider, final Provider<NoteDAO> noteDAOProvider,
                     final BindingsManager bindingsManager, final Provider<NotificationDeliverer> delivererProvider,
                     final ThreadingManager threadingManager) {
        this.notificationDAOProvider = notificationDAOProvider;
        this.noteDAOProvider = noteDAOProvider;
        this.bindingsManager = bindingsManager;
        this.delivererProvider = delivererProvider;
        this.threadingManager = threadingManager;
    }


    @Subscribe
    public void onNoteAdded(final NoteAddedEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                inTransaction(new SimpleTransactionTask() {
                    @Override
                    public void run(final DelayedEventPoster delayedEventBus) {
                        try {
                            Note note = noteDAOProvider.get().getNote(event.getNoteId());
                            NotificationDeliverer notificationDeliverer = delivererProvider.get();
                            for (Notification notification : notificationDAOProvider.get().getNotifications(NotificationType.NOTE_ADDED)) {
                                if (bindingsManager.matchesBindings(note.getBindings(), notification.getUser())) {
                                    notification.getMethod().deliver(notificationDeliverer, notification.getUser(), "New note notification",
                                            "The following note was just added by " + note.getAddedBy() + ": " +
                                                    note.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            log.error("", e);
                        }
                    }
                });
            }
        };
        threadingManager.execute(runnable);
    }
}
