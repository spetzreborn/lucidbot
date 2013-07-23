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
import tools.UtopiaPropertiesConfig;
import tools.communication.NotificationDeliverer;
import tools.time.UtopiaTime;

import javax.inject.Inject;
import java.util.List;

@Log4j
public class WaveAnnouncer extends AbstractAnnouncer implements EventListener {
    private final Provider<EventDAO> eventDAOProvider;
    private final PropertiesCollection properties;
    private final Provider<NotificationDAO> notificationDAOProvider;
    private final Provider<NotificationDeliverer> delivererProvider;

    @Inject
    public WaveAnnouncer(final Provider<EventDAO> eventDAOProvider, final TemplateManager templateManager,
                         final IRCEntityManager ircEntityManager, final IRCAccess ircAccess, final PropertiesCollection properties,
                         final Provider<NotificationDAO> notificationDAOProvider, final Provider<NotificationDeliverer> delivererProvider) {
        super(templateManager, ircEntityManager, ircAccess);
        this.eventDAOProvider = eventDAOProvider;
        this.properties = properties;
        this.notificationDAOProvider = notificationDAOProvider;
        this.delivererProvider = delivererProvider;
    }

    @Subscribe
    public void onTick(final TickEvent event) {
        try {
            UtopiaTime nextTick = event.getUtoDate().increment(1);
            Event wave = eventDAOProvider.get().getWave();

            if (wave == null || wave.getEventTime().getTime() >= nextTick.getTime()) return;

            eventDAOProvider.get().delete(wave);
            if (isEnabled()) {
                String[] output = compileTemplateOutput(MapFactory.newMapWithNamedObjects("wave", wave), "announcement-wave");
                announce(ChannelType.PRIVATE, output);
            }

            List<Notification> notifications = notificationDAOProvider.get().getNotifications(NotificationType.WAVE);
            delivererProvider.get().deliverNotifications(notifications, "Wave time!", "Wave time!");
        } catch (HibernateException e) {
            log.error("", e);
        }
    }

    private boolean isEnabled() {
        return properties.getBoolean(UtopiaPropertiesConfig.ANNOUNCE_WAVE_ENABLED);
    }
}
