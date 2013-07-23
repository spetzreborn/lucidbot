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
import api.database.models.AccessLevel;
import api.events.DelayedEventPoster;
import api.events.bot.NonCommandEvent;
import api.irc.ValidationType;
import api.irc.communication.IRCAccess;
import api.runtime.IRCContext;
import api.runtime.ThreadingManager;
import api.tools.numbers.NumberUtil;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.ProvinceDAO;
import database.models.Province;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import tools.parsing.UtopiaValidationType;

import javax.inject.Inject;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.database.Transactions.inTransaction;

@Log4j
class InfiltrationListener implements EventListener {
    private static final Pattern INFILTRATE = Pattern.compile("Our thieves have infiltrated the Thieves' Guilds of (?<target>[^(]+" +
            UtopiaValidationType.KDLOC.getPatternString() + "). They appear to have about (?<result>" + ValidationType.INT.getPattern() +
            ") thieves employed across their lands");

    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final IRCAccess ircAccess;
    private final ThreadingManager threadingManager;

    @Inject
    InfiltrationListener(final Provider<ProvinceDAO> provinceDAOProvider, final IRCAccess ircAccess,
                         final ThreadingManager threadingManager) {
        this.provinceDAOProvider = provinceDAOProvider;
        this.ircAccess = ircAccess;
        this.threadingManager = threadingManager;
    }

    @Subscribe
    public void onNonCommandEvent(final NonCommandEvent event) {
        IRCContext context = event.getContext();
        if (!AccessLevel.USER.allows(context.getUser(), context.getChannel())) return;

        threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                Matcher matcher = INFILTRATE.matcher(event.getContext().getInput());
                if (matcher.find()) {
                    String target = matcher.group("target");
                    final String provinceName = target.substring(0, target.indexOf('(')).trim();
                    final String kingdom = target.substring(target.indexOf('('));
                    final int result = NumberUtil.parseInt(matcher.group("result"));

                    inTransaction(new SimpleTransactionTask() {
                        @Override
                        public void run(final DelayedEventPoster delayedEventPoster) {
                            try {
                                String newTPA = registerInfiltrate(provinceName, kingdom, result);
                                ircAccess.sendNoticeOrPM(event.getContext(), "Thieves updated successfully! Raw tpa set to: " + newTPA);
                            } catch (final HibernateException e) {
                                InfiltrationListener.log.error("Could not update thieves", e);
                            }
                        }
                    });
                }
            }
        });
    }

    private String registerInfiltrate(final String provinceName, final String kingdom, final int result) {
        ProvinceDAO provinceDAO = provinceDAOProvider.get();
        Province province = provinceDAO.getOrCreateProvince(provinceName, kingdom);
        province.setThieves(result);
        province.setThievesLastUpdated(new Date());
        return province.getLand() == 0 ? "? (unknown land)" : String.valueOf(result * 1.0 / province.getLand());
    }
}
