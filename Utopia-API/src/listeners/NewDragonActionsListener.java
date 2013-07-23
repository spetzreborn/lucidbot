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
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.irc.communication.IRCAccess;
import api.runtime.ThreadingManager;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.DragonProjectDAO;
import database.models.DragonAction;
import database.models.DragonProject;
import database.models.DragonProjectType;
import events.DragonActionEvent;
import events.DragonProjectUpdateEvent;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.Date;
import java.util.Set;

import static api.database.Transactions.inTransaction;
import static api.tools.text.StringUtil.prettifyEnumName;

/**
 * Manages dragon related information
 */
@Log4j
class NewDragonActionsListener implements EventListener {
    private final Provider<DragonProjectDAO> dragonProjectDAOProvider;
    private final IRCAccess ircAccess;
    private final ThreadingManager threadingManager;

    @Inject
    NewDragonActionsListener(final Provider<DragonProjectDAO> dragonProjectDAOProvider, final IRCAccess ircAccess,
                             final ThreadingManager threadingManager) {
        this.dragonProjectDAOProvider = dragonProjectDAOProvider;
        this.ircAccess = ircAccess;
        this.threadingManager = threadingManager;
    }

    @Subscribe
    public void onDragonActionEvent(final DragonActionEvent event) {
        threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                inTransaction(new SimpleTransactionTask() {
                    @Override
                    public void run(final DelayedEventPoster delayedEventPoster) {
                        try {
                            BotUser botUser = event.getContext().getBotUser();
                            botUser.getStats(); //make sure they get loaded (Hibernate was whining for some reason)

                            String reply = registerDragonAction(event.getProjectType(), botUser, event.getContribution());
                            if (reply != null) ircAccess.sendNoticeOrPM(event.getContext(), reply);
                        } catch (Exception e) {
                            NewDragonActionsListener.log.error("Dragon action event could not be handled", e);
                        }
                    }
                });
            }
        });
    }

    private String registerDragonAction(final DragonProjectType type, final BotUser user, final int contribution) {
        try {
            DragonProjectDAO dao = dragonProjectDAOProvider.get();
            DragonProject project = dao.getProjectOfType(type);
            if (project == null || project.getStatus() <= 0) return null;

            int actualContribution = Math.min(contribution, project.getStatus());
            if (actualContribution == 0) return "This dragon project is already finished";
            registerDragonStats("Dragon " + prettifyEnumName(project.getType()), actualContribution, user);

            Set<DragonAction> actions = project.getActions();
            Date now = new Date();
            for (DragonAction action : actions) {
                if (action.getUser().equals(user)) {
                    action.setContribution(action.getContribution() + actualContribution);
                    project.setStatus(Math.max(0, project.getStatus() - contribution));
                    project.setUpdated(now);
                    action.setUpdated(now);
                    return "Contribution saved";
                }
            }

            actions.add(new DragonAction(user, actualContribution, now, project));
            project.setStatus(Math.max(0, project.getStatus() - contribution));
            project.setUpdated(now);

            return "Contribution saved";
        } catch (HibernateException e) {
            NewDragonActionsListener.log.error("", e);
        }
        return "Failed to save dragon actions";
    }

    private static void registerDragonStats(final String statName, final int statValue, final BotUser user) {
        try {
            if (user != null) user.incrementStat(statName, statValue);
        } catch (HibernateException e) {
            NewDragonActionsListener.log.error("", e);
        }
    }

    @Subscribe
    public void onDragonProjectUpdateEvent(final DragonProjectUpdateEvent event) {
        threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                inTransaction(new SimpleTransactionTask() {
                    @Override
                    public void run(final DelayedEventPoster delayedEventPoster) {
                        try {
                            String reply = registerDragonProjectStatus(event.getType(), event.getStatus());
                            ircAccess.sendNoticeOrPM(event.getContext(), reply);
                        } catch (Exception e) {
                            NewDragonActionsListener.log.error("Dragon update could not be handled", e);
                        }
                    }
                });
            }
        });
    }

    private String registerDragonProjectStatus(final DragonProjectType type, final int status) {
        try {
            DragonProjectDAO dragonProjectDAO = dragonProjectDAOProvider.get();
            DragonProject project = dragonProjectDAO.getProjectOfType(type);
            if (project == null || project.getStatus() == 0) {
                project = new DragonProject(type, status);
                dragonProjectDAO.save(project);
                return "New dragon project created";
            } else {
                project.setStatus(status);
                project.setUpdated(new Date());
                return "Dragon project status updated";
            }
        } catch (HibernateException e) {
            NewDragonActionsListener.log.error("Dragon project status could not be updated", e);
        }
        return "Failed to update";
    }
}
