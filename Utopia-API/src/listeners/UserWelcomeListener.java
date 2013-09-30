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

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.database.transactions.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import api.events.bot.UserLoginEvent;
import api.events.irc.PrivateMessageEvent;
import api.irc.UserAuthenticationRequestSource;
import api.irc.communication.IRCAccess;
import api.runtime.ThreadingManager;
import api.settings.PropertiesCollection;
import api.settings.PropertiesConfig;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.PrivateMessageDAO;
import database.models.PrivateMessage;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.List;

import static api.database.transactions.Transactions.inTransaction;
import static api.tools.text.StringUtil.isNotNullOrEmpty;

@Log4j
class UserWelcomeListener implements EventListener {
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<PrivateMessageDAO> privateMessageDAOProvider;
    private final ThreadingManager threadingManager;
    private final IRCAccess ircAccess;
    private final PropertiesCollection properties;
    private final EventBus eventBus;

    @Inject
    UserWelcomeListener(final Provider<BotUserDAO> botUserDAOProvider,
                        final Provider<PrivateMessageDAO> privateMessageDAOProvider,
                        final ThreadingManager threadingManager,
                        final IRCAccess ircAccess,
                        final PropertiesCollection properties,
                        final EventBus eventBus) {
        this.botUserDAOProvider = botUserDAOProvider;
        this.privateMessageDAOProvider = privateMessageDAOProvider;
        this.threadingManager = threadingManager;
        this.ircAccess = ircAccess;
        this.properties = properties;
        this.eventBus = eventBus;
    }

    @Subscribe
    public void onUserLogin(final UserLoginEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                inTransaction(new SimpleTransactionTask() {
                    @Override
                    public void run(final DelayedEventPoster delayedEventBus) {
                        try {
                            BotUser user = botUserDAOProvider.get().getUser(event.getUserId());

                            if (event.getSource() != UserAuthenticationRequestSource.USER_LIST) {
                                ircAccess.sendPrivateMessage(event.getUser(), "You have been authenticated and can now use the bot");

                                String loginCommand = properties.get(PropertiesConfig.LOGIN_COMMAND);
                                if (isNotNullOrEmpty(loginCommand)) {
                                    eventBus.post(new PrivateMessageEvent(event.getReceiver(), event.getUser().getCurrentNick(), loginCommand));
                                }
                            }

                            List<PrivateMessage> unreadMessages = privateMessageDAOProvider.get().getUnread(user);
                            if (!unreadMessages.isEmpty()) {
                                ircAccess.sendPrivateMessage(event.getUser(), "You have " + unreadMessages.size() + " unread private messages!");
                            }
                        } catch (HibernateException e) {
                            UserWelcomeListener.log.error("", e);
                        }
                    }
                });
            }
        };
        threadingManager.execute(runnable);
    }
}
