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
import database.daos.IntelDAO;
import database.daos.KingdomDAO;
import database.models.*;
import events.*;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import tools.UtopiaPropertiesConfig;

import javax.inject.Inject;

@Log4j
public class IntelSavedAnnouncer extends AbstractAnnouncer implements EventListener {
    private final Provider<IntelDAO> intelDAOProvider;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final PropertiesCollection properties;

    @Inject
    public IntelSavedAnnouncer(final TemplateManager templateManager, final IRCEntityManager ircEntityManager, final IRCAccess ircAccess,
                               final Provider<IntelDAO> intelDAOProvider, final Provider<KingdomDAO> kingdomDAOProvider,
                               final PropertiesCollection properties) {
        super(templateManager, ircEntityManager, ircAccess);
        this.intelDAOProvider = intelDAOProvider;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.properties = properties;
    }

    @Subscribe
    public void onKingdomSaved(final KingdomSavedEvent event) {
        try {
            Kingdom kingdom = kingdomDAOProvider.get().getKingdom(event.getId());
            if (kingdom != null && isEnabled()) {
                String[] output = compileTemplateOutput(MapFactory.newMapWithNamedObjects("kingdom", kingdom),
                        "announcement-kingdom-saved");
                announce(ChannelType.PRIVATE, output);
            }
        } catch (HibernateException e) {
            IntelSavedAnnouncer.log.error("", e);
        }
    }

    @Subscribe
    public void onSoMSaved(final SoMSavedEvent event) {
        try {
            SoM soM = intelDAOProvider.get().getSoM(event.getId());
            if (soM != null && isEnabled()) {
                String[] output = compileTemplateOutput(MapFactory.newMapWithNamedObjects("som", soM), "announcement-som-saved");
                announce(ChannelType.PRIVATE, output);
            }
        } catch (HibernateException e) {
            IntelSavedAnnouncer.log.error("", e);
        }
    }

    @Subscribe
    public void onSoSSaved(final SoSSavedEvent event) {
        try {
            SoS soS = intelDAOProvider.get().getSoS(event.getId());
            if (soS != null && isEnabled()) {
                String[] output = compileTemplateOutput(MapFactory.newMapWithNamedObjects("sos", soS), "announcement-sos-saved");
                announce(ChannelType.PRIVATE, output);
            }
        } catch (HibernateException e) {
            IntelSavedAnnouncer.log.error("", e);
        }
    }

    @Subscribe
    public void onSoTSaved(final SoTSavedEvent event) {
        try {
            SoT soT = intelDAOProvider.get().getSoT(event.getId());
            if (soT != null && isEnabled()) {
                String[] output = compileTemplateOutput(MapFactory.newMapWithNamedObjects("sot", soT), "announcement-sot-saved");
                announce(ChannelType.PRIVATE, output);
            }
        } catch (HibernateException e) {
            IntelSavedAnnouncer.log.error("", e);
        }
    }

    @Subscribe
    public void onSurveySaved(final SurveySavedEvent event) {
        try {
            Survey survey = intelDAOProvider.get().getSurvey(event.getId());
            if (survey != null && isEnabled()) {
                String[] output = compileTemplateOutput(MapFactory.newMapWithNamedObjects("survey", survey), "announcement-survey-saved");
                announce(ChannelType.PRIVATE, output);
            }
        } catch (HibernateException e) {
            IntelSavedAnnouncer.log.error("", e);
        }
    }

    private boolean isEnabled() {
        return properties.getBoolean(UtopiaPropertiesConfig.ANNOUNCE_INTEL_SAVED_ENABLED);
    }
}
