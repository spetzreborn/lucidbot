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

package commands.army.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import api.tools.collections.Params;
import api.tools.time.DateUtil;
import database.daos.ArmyDAO;
import database.daos.NotificationDAO;
import database.daos.ProvinceDAO;
import database.models.*;
import listeners.ArmyManager;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static api.tools.collections.CollectionUtil.isEmpty;
import static api.tools.text.StringUtil.isNotNullOrEmpty;
import static tools.UtopiaPropertiesConfig.TICK_LENGTH;

public class ArmyCommandHandler implements CommandHandler {
    private final PropertiesCollection properties;
    private final ArmyManager armyManager;
    private final ArmyDAO armyDAO;
    private final BotUserDAO userDAO;
    private final ProvinceDAO provinceDAO;
    private final NotificationDAO notificationDAO;

    @Inject
    public ArmyCommandHandler(final PropertiesCollection properties,
                              final BotUserDAO userDAO,
                              final ArmyDAO armyDAO,
                              final ArmyManager armyManager,
                              final ProvinceDAO provinceDAO,
                              final NotificationDAO notificationDAO) {
        this.properties = properties;
        this.userDAO = userDAO;
        this.armyDAO = armyDAO;
        this.armyManager = armyManager;
        this.provinceDAO = provinceDAO;
        this.notificationDAO = notificationDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            if (params.isEmpty()) {
                return handleArmyDisplay(context.getBotUser());
            } else if (params.size() == 1) {
                BotUser user = userDAO.getClosestMatch(params.getParameter("user"));
                if (user == null) return CommandResponse.errorResponse("No such user exists");
                return handleArmyDisplay(user);
            } else if (params.size() == 2 && params.getIntParameter("armyno") > 0 && params.getDoubleParameter("returntime") > 0) {
                return handleAddingArmy(context.getBotUser(), params.getIntParameter("armyno"), params.getDoubleParameter("returntime"),
                        context, delayedEventPoster);
            } else if (params.size() == 3 && isNotNullOrEmpty(params.getParameter("user")) &&
                    params.getIntParameter("armyno") > 0 && params.getDoubleParameter("returntime") > 0) {
                BotUser user = userDAO.getClosestMatch(params.getParameter("user"));
                if (user == null) return CommandResponse.errorResponse("No such user exists");
                return handleAddingArmy(user, params.getIntParameter("armyno"), params.getDoubleParameter("returntime"), context,
                        delayedEventPoster);
            }
            throw new IllegalStateException("Not supposed to be able to get here, so there's some params error");
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private CommandResponse handleArmyDisplay(final BotUser user) {
        List<Army> armies = armyDAO.getIRCArmiesForUser(user);
        if (isEmpty(armies)) armies = armyDAO.getIntelArmiesOutForUser(user);
        if (isEmpty(armies)) return CommandResponse.errorResponse("Could not find any armies");
        Collections.sort(armies);
        return CommandResponse.resultResponse("user", user, "armies", armies, "added", false);
    }

    private CommandResponse handleAddingArmy(final BotUser user,
                                             final int armyno,
                                             final double returntime,
                                             final IRCContext context,
                                             final DelayedEventPoster delayedEventPoster) {
        Province prov = provinceDAO.getProvinceForUser(user);
        if (prov == null) return CommandResponse.errorResponse("Can't add an army without having a province");
        long armyhometime = System.currentTimeMillis() + DateUtil.minutesToMillis(returntime * properties.getInteger(TICK_LENGTH));
        Army army = armyManager
                .saveIRCArmy(new Army(prov, armyno, Army.ArmyType.IRC_ARMY_OUT, new Date(armyhometime), 0), context, delayedEventPoster);
        ensureNotifications(user);
        return CommandResponse.resultResponse("user", user, "army", army, "added", true);
    }

    private void ensureNotifications(final BotUser user) {
        Collection<Notification> notifications = notificationDAO.getNotifications(user, NotificationType.ARMY_HOME);
        if (notifications.isEmpty()) {
            notificationDAO.save(new Notification(user, NotificationType.ARMY_HOME, NotificationMethod.PM));
        }
    }
}
