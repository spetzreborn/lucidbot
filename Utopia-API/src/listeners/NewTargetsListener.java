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
import database.daos.NotificationDAO;
import database.daos.TargetDAO;
import database.models.Notification;
import database.models.NotificationType;
import database.models.Province;
import database.models.Target;
import events.TargetAddedEvent;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import tools.BindingsManager;
import tools.communication.NotificationDeliverer;

import javax.inject.Inject;

import static api.database.transactions.Transactions.inTransaction;

@Log4j
class NewTargetsListener implements EventListener {
    private final Provider<TargetDAO> targetDAOProvider;
    private final Provider<NotificationDAO> notificationDAOProvider;
    private final Provider<NotificationDeliverer> delivererProvider;
    private final BindingsManager bindingsManager;
    private final ThreadingManager threadingManager;

    @Inject
    NewTargetsListener(final Provider<TargetDAO> targetDAOProvider, final Provider<NotificationDAO> notificationDAOProvider,
                       final Provider<NotificationDeliverer> delivererProvider, final BindingsManager bindingsManager,
                       final ThreadingManager threadingManager) {
        this.targetDAOProvider = targetDAOProvider;
        this.notificationDAOProvider = notificationDAOProvider;
        this.delivererProvider = delivererProvider;
        this.bindingsManager = bindingsManager;
        this.threadingManager = threadingManager;
    }

    @Subscribe
    public void onTargetAdded(final TargetAddedEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                inTransaction(new SimpleTransactionTask() {
                    @Override
                    public void run(final DelayedEventPoster delayedEventBus) {
                        try {
                            Target target = targetDAOProvider.get().getTarget(event.getTargetId());
                            Province province = target.getProvince();
                            NotificationDeliverer notificationDeliverer = delivererProvider.get();
                            for (Notification notification : notificationDAOProvider.get()
                                    .getNotifications(NotificationType.TARGET_ADDED)) {
                                if (bindingsManager.matchesBindings(target.getBindings(), notification.getUser())) {
                                    String message = province.getName() + ' ' + province.getKingdom().getLocation() +
                                            " was just added as a target";
                                    notification.getMethod()
                                            .deliver(notificationDeliverer, notification.getUser(), "New target notification", message);
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
