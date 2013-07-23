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

package announcements;

import api.database.models.ChannelType;
import api.irc.IRCEntityManager;
import api.irc.communication.IRCAccess;
import api.settings.PropertiesCollection;
import api.templates.TemplateManager;
import api.tools.collections.MapFactory;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.EventDAO;
import database.daos.NotificationDAO;
import database.models.Event;
import database.models.Notification;
import database.models.NotificationType;
import events.TickEvent;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import tools.BindingsManager;
import tools.UtopiaPropertiesConfig;
import tools.communication.NotificationDeliverer;
import tools.time.UtopiaTime;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;

@Log4j
public class EventAnnouncer extends AbstractAnnouncer implements EventListener {
    private final Provider<EventDAO> eventDAOProvider;
    private final PropertiesCollection properties;
    private final Provider<NotificationDAO> notificationDAOProvider;
    private final Provider<NotificationDeliverer> delivererProvider;
    private final BindingsManager bindingsManager;

    @Inject
    public EventAnnouncer(final Provider<EventDAO> eventDAOProvider, final TemplateManager templateManager,
                          final IRCEntityManager ircEntityManager, final IRCAccess ircAccess, final PropertiesCollection properties,
                          final Provider<NotificationDAO> notificationDAOProvider, final Provider<NotificationDeliverer> delivererProvider,
                          final BindingsManager bindingsManager) {
        super(templateManager, ircEntityManager, ircAccess);
        this.eventDAOProvider = eventDAOProvider;
        this.properties = properties;
        this.notificationDAOProvider = notificationDAOProvider;
        this.delivererProvider = delivererProvider;
        this.bindingsManager = bindingsManager;
    }

    @Subscribe
    public void onTick(final TickEvent event) {
        try {
            UtopiaTime nextTick = event.getUtoDate().increment(1);
            List<Event> expiringEvents = eventDAOProvider.get().getExpiringEvents(nextTick.getDate());

            eventDAOProvider.get().delete(expiringEvents);

            if (isEnabled()) {
                String[] output = compileTemplateOutput(MapFactory.newMapWithNamedObjects("events", expiringEvents), "announcement-events");
                announce(ChannelType.PRIVATE, output);
            }

            for (Event expiringEvent : expiringEvents) {
                List<Notification> notifications = notificationDAOProvider.get().getNotifications(NotificationType.EVENT);

                for (Iterator<Notification> iter = notifications.iterator(); iter.hasNext(); ) {
                    if (!bindingsManager.matchesBindings(expiringEvent.getBindings(), iter.next().getUser())) iter.remove();
                }

                delivererProvider.get().deliverNotifications(notifications, "Event time!",
                                                             "An event was scheduled for this tick: " + expiringEvent.getDescription());
            }
        } catch (HibernateException e) {
            EventAnnouncer.log.error("", e);
        }
    }

    private boolean isEnabled() {
        return properties.getBoolean(UtopiaPropertiesConfig.ANNOUNCE_EVENTS_ENABLED);
    }
}
