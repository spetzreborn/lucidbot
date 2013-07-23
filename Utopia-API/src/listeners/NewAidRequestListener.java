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

import api.runtime.ThreadingManager;
import api.tools.text.StringUtil;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.AidDAO;
import database.daos.NotificationDAO;
import database.models.Aid;
import database.models.Notification;
import database.models.NotificationType;
import events.AidAddedEvent;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import tools.communication.NotificationDeliverer;

import javax.inject.Inject;
import java.util.List;

@Log4j
class NewAidRequestListener implements EventListener {
    private final Provider<AidDAO> aidDAOProvider;
    private final ThreadingManager threadingManager;
    private final Provider<NotificationDAO> notificationDAOProvider;
    private final Provider<NotificationDeliverer> delivererProvider;

    @Inject
    NewAidRequestListener(final Provider<AidDAO> aidDAOProvider, final ThreadingManager threadingManager,
                          final Provider<NotificationDeliverer> delivererProvider,
                          final Provider<NotificationDAO> notificationDAOProvider) {
        this.aidDAOProvider = aidDAOProvider;
        this.threadingManager = threadingManager;
        this.delivererProvider = delivererProvider;
        this.notificationDAOProvider = notificationDAOProvider;
    }


    @Subscribe
    public void onAidAdded(final AidAddedEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Aid aid = aidDAOProvider.get().getAid(event.getAidId());
                String details = aid.getAmount() + " " + StringUtil.prettifyEnumName(aid.getType());
                String importance = StringUtil.prettifyEnumName(aid.getImportanceType());
                String province = aid.getProvince().getName();
                try {
                    List<Notification> notifications = notificationDAOProvider.get().getNotifications(NotificationType.AID_ADDED);
                    delivererProvider.get().deliverNotifications(notifications, "New aid request notification",
                                                                 "An " + importance + " aid request for " + details +
                                                                 " was just added for " +
                                                                 province);
                } catch (HibernateException e) {
                    NewAidRequestListener.log.error("", e);
                }
            }
        };
        threadingManager.execute(runnable);
    }
}