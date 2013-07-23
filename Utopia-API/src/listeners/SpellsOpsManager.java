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

import api.events.bot.StartupEvent;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.OpDAO;
import database.daos.SpellDAO;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.Date;

@Log4j
public class SpellsOpsManager implements EventListener {
    private final Provider<SpellDAO> spellDAOProvider;
    private final Provider<OpDAO> opDAOProvider;
    private final UtopiaTimeFactory utopiaTimeFactory;

    @Inject
    public SpellsOpsManager(final Provider<SpellDAO> spellDAOProvider, final Provider<OpDAO> opDAOProvider,
                            final UtopiaTimeFactory utopiaTimeFactory) {
        this.spellDAOProvider = spellDAOProvider;
        this.opDAOProvider = opDAOProvider;
        this.utopiaTimeFactory = utopiaTimeFactory;
    }

    @Subscribe
    public void onStartup(final StartupEvent startupEvent) {
        try {
            Date currentTickStart = utopiaTimeFactory.newUtopiaTime(System.currentTimeMillis()).getDate();
            spellDAOProvider.get().deleteDurationSpells(currentTickStart);
            opDAOProvider.get().deleteDurationOps(currentTickStart);
        } catch (HibernateException e) {
            SpellsOpsManager.log.error("Could not remove expired spells and ops");
        }
    }
}
