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
import database.daos.OpDAO;
import database.models.DurationOp;
import events.TickEvent;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import tools.UtopiaPropertiesConfig;
import tools.time.UtopiaTime;

import javax.inject.Inject;
import java.util.List;

@Log4j
public class ExpiringOpsAnnouncer extends AbstractAnnouncer implements EventListener {
    private final Provider<OpDAO> opDAOProvider;
    private final PropertiesCollection properties;

    @Inject
    public ExpiringOpsAnnouncer(final TemplateManager templateManager, final IRCEntityManager ircEntityManager, final IRCAccess ircAccess,
                                final Provider<OpDAO> opDAOProvider, final PropertiesCollection properties) {
        super(templateManager, ircEntityManager, ircAccess);
        this.opDAOProvider = opDAOProvider;
        this.properties = properties;
    }

    @Subscribe
    public void onTick(final TickEvent event) {
        try {
            UtopiaTime nextTick = event.getUtoDate().increment(1);
            List<DurationOp> ops = opDAOProvider.get().deleteDurationOps(nextTick.getDate());

            if (isEnabled()) {
                String[] output = compileTemplateOutput(MapFactory.newMapWithNamedObjects("ops", ops), "announcement-expiring-ops");
                announce(ChannelType.PRIVATE, output);
            }
        } catch (HibernateException e) {
            log.error("", e);
        }
    }

    private boolean isEnabled() {
        return properties.getBoolean(UtopiaPropertiesConfig.ANNOUNCE_OPS_ENABLED);
    }
}
