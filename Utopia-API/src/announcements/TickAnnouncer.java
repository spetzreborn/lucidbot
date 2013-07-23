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
import database.daos.TickChannelMessageDAO;
import database.models.TickChannelMessage;
import events.TickEvent;
import spi.events.EventListener;
import tools.UtopiaPropertiesConfig;

import javax.inject.Inject;

public class TickAnnouncer extends AbstractAnnouncer implements EventListener {
    private final PropertiesCollection properties;
    private final TickChannelMessageDAO channelMessageDAO;

    @Inject
    public TickAnnouncer(final TemplateManager templateManager, final IRCEntityManager ircEntityManager, final IRCAccess ircAccess,
                         final PropertiesCollection properties, final TickChannelMessageDAO channelMessageDAO) {
        super(templateManager, ircEntityManager, ircAccess);
        this.properties = properties;
        this.channelMessageDAO = channelMessageDAO;
    }

    @Subscribe
    public void onTick(final TickEvent event) {
        if (isEnabled()) {
            String[] output = compileTemplateOutput(MapFactory.newMapWithNamedObjects("utodate", event.getUtoDate()), "announcement-tick");
            announce(ChannelType.PRIVATE, output);
            for (TickChannelMessage tickChannelMessage : channelMessageDAO.getAllTickChannelMessages()) {
                String channelName = tickChannelMessage.getChannel().getName();
                announce(channelName, tickChannelMessage.getMessage());
            }
        }
    }

    private boolean isEnabled() {
        return properties.getBoolean(UtopiaPropertiesConfig.ANNOUNCE_NEW_TICK_ENABLED);
    }
}
