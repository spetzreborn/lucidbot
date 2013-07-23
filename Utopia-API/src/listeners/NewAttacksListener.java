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
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.irc.communication.IRCAccess;
import api.runtime.IRCContext;
import api.runtime.ThreadingManager;
import api.tools.numbers.NumberUtil;
import api.tools.text.RegexUtil;
import api.tools.text.StringUtil;
import api.tools.time.TimeUtil;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.AttackDAO;
import database.daos.ProvinceDAO;
import database.models.Army;
import database.models.Attack;
import database.models.Province;
import events.AttackInfoPastedEvent;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static api.database.Transactions.inTransaction;

/**
 * Manages attacks as they are pasted on IRC
 */
@Log4j
class NewAttacksListener implements EventListener {
    private final Provider<AttackDAO> attackDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final ThreadingManager threadingManager;
    private final ArmyManager armyManager;
    private final IRCAccess ircAccess;

    private final ConcurrentMap<String, Long> userAttacks = new ConcurrentHashMap<>();

    @Inject
    NewAttacksListener(final Provider<AttackDAO> attackDAOProvider, final Provider<ProvinceDAO> provinceDAOProvider,
                       final Provider<BotUserDAO> botUserDAOProvider, final ThreadingManager threadingManager,
                       final ArmyManager armyManager, final IRCAccess ircAccess) {
        this.attackDAOProvider = attackDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;
        this.threadingManager = threadingManager;
        this.armyManager = armyManager;
        this.ircAccess = ircAccess;
    }

    @Subscribe
    public void onAttackInfoPastedEvent(final AttackInfoPastedEvent event) {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                try {
                    setAttackInfo(event, delayedEventPoster);
                } catch (Exception e) {
                    NewAttacksListener.log.error("Attack info event could not be handled", e);
                }
            }
        });
    }

    private void setAttackInfo(final AttackInfoPastedEvent event, final DelayedEventPoster delayedEventPoster) {
        try {
            AttackDAO attackDAO = attackDAOProvider.get();
            Long attackId = userAttacks.get(event.getAttacker().getCurrentNick());
            if (attackId == null && event.getProvinceName() != null) {
                BotUser user = botUserDAOProvider.get().getUser(event.getAttacker().getMainNick());
                ProvinceDAO provinceDAO = provinceDAOProvider.get();
                Province attacker = provinceDAO.getProvinceForUser(user);
                if (attacker == null) return;

                Province defender = provinceDAO.getOrCreateProvince(event.getProvinceName(), event.getKdLoc());

                Attack attack = new Attack(attacker, defender, event.getGain() == null ? "Nubhat" : String.valueOf(event.getGain()),
                        event.getAttackType());
                attack = attackDAO.save(attack);

                userAttacks.put(event.getAttacker().getCurrentNick(), attack.getId());
                threadingManager.schedule(new CleanUp(event.getAttacker().getCurrentNick(), attack.getId(), this), 10, TimeUnit.SECONDS);
            } else if (attackId != null) {
                Attack attack = attackDAO.getAttack(attackId);
                if (event.getKills() != null) attack.setKills(event.getKills());
                if (event.getGotPlagued() != null) attack.setGotPlagued(event.getGotPlagued());
                if (event.getSpreadPlague() != null) attack.setSpreadPlague(event.getSpreadPlague());
                if (event.getReturnTime() != null) addNewArmy(event, attack, event.getContext(), delayedEventPoster);
            }
        } catch (HibernateException e) {
            NewAttacksListener.log.error("", e);
        }
    }

    private void addNewArmy(final AttackInfoPastedEvent event, final Attack attack, final IRCContext context,
                            final DelayedEventPoster delayedEventPoster) {
        BotUser user = botUserDAOProvider.get().getUser(event.getAttacker().getMainNick());
        ProvinceDAO provinceDAO = provinceDAOProvider.get();
        Province attacker = provinceDAO.getProvinceForUser(user);
        if (attacker == null) return;
        String attackGain = RegexUtil.NON_NUMBER_PATTERN.matcher(attack.getGain()).replaceAll("");
        int gain = StringUtil.isNullOrEmpty(attackGain) ? 0 : NumberUtil.parseInt(attackGain);
        Army army = armyManager
                .saveIRCArmy(new Army(attacker, null, Army.ArmyType.IRC_ARMY_OUT, new Date(event.getReturnTime()), gain), context,
                        delayedEventPoster);
        ircAccess.sendNoticeOrPM(event.getContext(), "Army saved, returning in " + TimeUtil.compareDateToCurrent(army.getReturningDate()));
    }

    private static class CleanUp implements Runnable {
        private final String nick;
        private final Long attackId;
        private final NewAttacksListener newAttacksListener;

        private CleanUp(final String nick, final Long attackId, final NewAttacksListener newAttacksListener) {
            this.nick = nick;
            this.attackId = attackId;
            this.newAttacksListener = newAttacksListener;
        }

        @Override
        public void run() {
            newAttacksListener.userAttacks.remove(nick, attackId);
        }
    }
}
