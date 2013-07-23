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
import api.runtime.ThreadingManager;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.EventDAO;
import database.daos.NotificationDAO;
import database.models.Event;
import database.models.Notification;
import database.models.NotificationType;
import events.EventAddedEvent;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import tools.BindingsManager;
import tools.communication.NotificationDeliverer;

import javax.inject.Inject;

import static api.database.Transactions.inTransaction;

@Log4j
class NewEventsListener implements EventListener {
    private final Provider<EventDAO> eventDAOProvider;
    private final Provider<NotificationDAO> notificationDAOProvider;
    private final Provider<NotificationDeliverer> delivererProvider;
    private final BindingsManager bindingsManager;
    private final ThreadingManager threadingManager;

    @Inject
    NewEventsListener(final Provider<EventDAO> eventDAOProvider, final Provider<NotificationDAO> notificationDAOProvider,
                      final Provider<NotificationDeliverer> delivererProvider, final BindingsManager bindingsManager,
                      final ThreadingManager threadingManager) {
        this.eventDAOProvider = eventDAOProvider;
        this.notificationDAOProvider = notificationDAOProvider;
        this.delivererProvider = delivererProvider;
        this.bindingsManager = bindingsManager;
        this.threadingManager = threadingManager;
    }

    @Subscribe
    public void onEventAdded(final EventAddedEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                inTransaction(new SimpleTransactionTask() {
                    @Override
                    public void run(final DelayedEventPoster delayedEventBus) {
                        try {
                            Event kdEvent = eventDAOProvider.get().getEvent(event.getEventId());
                            NotificationDeliverer notificationDeliverer = delivererProvider.get();
                            for (Notification notification : notificationDAOProvider.get().getNotifications(NotificationType.EVENT_ADDED)) {
                                if (bindingsManager.matchesBindings(kdEvent.getBindings(), notification.getUser())) {
                                    String message = "A new event has been scheduled for " +
                                            notification.getUser().getDateInUsersLocalTime(kdEvent.getEventTime()) +
                                            " described as: " + kdEvent.getDescription();
                                    notification.getMethod()
                                            .deliver(notificationDeliverer, notification.getUser(), "New event notification", message);
                                }
                            }
                        } catch (HibernateException e) {
                            log.error("", e);
                        }
                    }
                });
            }
        };
        threadingManager.execute(runnable);
    }
}
