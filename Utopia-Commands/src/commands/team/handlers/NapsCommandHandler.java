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

package commands.team.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.time.TimeUtil;
import database.daos.KingdomDAO;
import database.daos.UserActivitiesDAO;
import database.models.Kingdom;
import database.models.UserActivities;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.RecentActivitiesFinder;
import tools.time.UtopiaTimeFactory;
import tools.user_activities.RecentActivityType;
import tools.user_activities.UnseenInfo;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class NapsCommandHandler implements CommandHandler {
    private final KingdomDAO kingdomDAO;
    private final UserActivitiesDAO userActivitiesDAO;
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final RecentActivitiesFinder recentActivitiesFinder;

    @Inject
    public NapsCommandHandler(final KingdomDAO kingdomDAO, final UserActivitiesDAO userActivitiesDAO,
                              final UtopiaTimeFactory utopiaTimeFactory, final RecentActivitiesFinder recentActivitiesFinder) {
        this.kingdomDAO = kingdomDAO;
        this.userActivitiesDAO = userActivitiesDAO;
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.recentActivitiesFinder = recentActivitiesFinder;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            List<Kingdom> nappedKingdoms = new ArrayList<>();
            for (Kingdom kingdom : kingdomDAO.getAllKingdoms()) {
                if (kingdom.getNapDescription() != null) nappedKingdoms.add(kingdom);
            }
            if (nappedKingdoms.isEmpty()) return CommandResponse.errorResponse("No naps exist");

            BotUser botUser = context.getBotUser();
            UserActivities userActivities = userActivitiesDAO.getUserActivities(botUser);
            userActivities.setLastNapsCheck(new Date());

            List<NapInfo> naps = new ArrayList<>(nappedKingdoms.size());
            for (Kingdom kingdom : nappedKingdoms) {
                String utopiaTime = kingdom.getNapEndDate() == null ? null
                        : utopiaTimeFactory.newUtopiaTime(kingdom.getNapEndDate().getTime())
                        .formattedUT();
                String expiry = kingdom.getNapEndDate() == null ? null : TimeUtil.compareDateToCurrent(kingdom.getNapEndDate());
                naps.add(new NapInfo(kingdom.getLocation(), utopiaTime, expiry, kingdom.getNapDescription()));
            }

            List<UnseenInfo> unseenInfoOfInterest = recentActivitiesFinder
                    .mapUnseenActivities(botUser, userActivities);
            return CommandResponse.resultResponse("naps", naps, "unseenInfoOfInterest", unseenInfoOfInterest, "thisActivityType",
                    RecentActivityType.NAPS);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    public static class NapInfo {
        private final String kdLocation;
        private final String expiryUtoDate;
        private final String expiry;
        private final String details;

        public NapInfo(final String kdLocation, final String expiryUtoDate, final String expiry, final String details) {
            this.kdLocation = kdLocation;
            this.expiryUtoDate = expiryUtoDate;
            this.expiry = expiry;
            this.details = details;
        }

        public String getKdLocation() {
            return kdLocation;
        }

        public String getExpiryUtoDate() {
            return expiryUtoDate;
        }

        public String getExpiry() {
            return expiry;
        }

        public String getDetails() {
            return details;
        }
    }
}
