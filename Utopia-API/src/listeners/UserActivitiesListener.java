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
import api.events.irc.IRCMessageEvent;
import api.events.irc.KickEvent;
import api.events.irc.PartEvent;
import api.events.irc.QuitEvent;
import api.irc.IRCEntityManager;
import api.runtime.ThreadingManager;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.UserActivitiesDAO;
import database.models.UserActivities;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.Date;

/**
 * A class that manages user activities
 */
@Log4j
class UserActivitiesListener implements EventListener {
    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<UserActivitiesDAO> userActivitiesDAOProvider;
    private final ThreadingManager threadingManager;
    private final IRCEntityManager ircEntityManager;

    @Inject
    UserActivitiesListener(final Provider<BotUserDAO> userDAOProvider,
                           final Provider<UserActivitiesDAO> userActivitiesDAOProvider,
                           final ThreadingManager threadingManager,
                           final IRCEntityManager ircEntityManager) {
        this.userDAOProvider = userDAOProvider;
        this.userActivitiesDAOProvider = userActivitiesDAOProvider;
        this.threadingManager = threadingManager;
        this.ircEntityManager = ircEntityManager;
    }

    @Subscribe
    public void onIRCMessage(final IRCMessageEvent event) {
        threadingManager.execute(new ActivityTask(event.getSender(), new LastActivitySetter(), this));
    }

    @Subscribe
    public void onKick(final KickEvent event) {
        threadingManager.execute(new ActivityTask(event.getUser(), new LastSeenSetter(), this));
    }

    @Subscribe
    public void onPart(final PartEvent event) {
        threadingManager.execute(new ActivityTask(event.getUser(), new LastSeenSetter(), this));
    }

    @Subscribe
    public void onQuit(final QuitEvent event) {
        threadingManager.execute(new ActivityTask(event.getUser(), new LastSeenSetter(), this));
    }

    private static class ActivityTask implements Runnable {
        private final String nick;
        private final ActivitySetter activitySetter;
        private final UserActivitiesListener userActivitiesListener;

        private ActivityTask(final String nick,
                             final ActivitySetter activitySetter,
                             final UserActivitiesListener userActivitiesListener) {
            this.nick = nick;
            this.activitySetter = activitySetter;
            this.userActivitiesListener = userActivitiesListener;
        }

        @Override
        public void run() {
            try {
                BotUserDAO userDAO = userActivitiesListener.userDAOProvider.get();
                BotUser user = userDAO.getUser(nick);
                if (user != null) {
                    UserActivitiesDAO userActivitiesDAO = userActivitiesListener.userActivitiesDAOProvider.get();
                    UserActivities userActivities = userActivitiesDAO.getUserActivities(user);
                    activitySetter.setActivity(userActivities);
                    userActivitiesDAO.save(userActivities);
                }
            } catch (HibernateException e) {
                UserActivitiesListener.log.error("Could not save activity for user", e);
            }
        }
    }

    private interface ActivitySetter {
        void setActivity(UserActivities userActivities);
    }

    private static class LastSeenSetter implements ActivitySetter {
        @Override
        public void setActivity(final UserActivities userActivities) {
            userActivities.setLastSeen(new Date());
        }
    }

    private static class LastActivitySetter implements ActivitySetter {
        @Override
        public void setActivity(final UserActivities userActivities) {
            userActivities.setLastActivity(new Date());
        }
    }
}
