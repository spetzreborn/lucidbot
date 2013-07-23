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

package commands.irc.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.irc.entities.IRCChannel;
import api.irc.entities.IRCUser;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.files.FilterUtil;
import database.CommonEntitiesAccess;
import database.daos.ArmyDAO;
import database.daos.EventDAO;
import database.daos.KingdomDAO;
import database.models.*;
import filtering.filters.PersonalityFilter;
import filtering.filters.RaceFilter;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlapCommandHandler implements CommandHandler {
    private final CommonEntitiesAccess access;
    private final RaceFilter.Builder raceFilterBuilder;
    private final PersonalityFilter.Builder personalityFilterBuilder;
    private final BotUserDAO userDAO;
    private final KingdomDAO kingdomDAO;
    private final ArmyDAO armyDAO;
    private final EventDAO eventDAO;

    @Inject
    public SlapCommandHandler(final CommonEntitiesAccess access, final RaceFilter.Builder raceFilterBuilder,
                              final PersonalityFilter.Builder personalityFilterBuilder, final EventDAO eventDAO, final ArmyDAO armyDAO,
                              final KingdomDAO kingdomDAO, final BotUserDAO userDAO) {
        this.access = access;
        this.raceFilterBuilder = raceFilterBuilder;
        this.personalityFilterBuilder = personalityFilterBuilder;
        this.eventDAO = eventDAO;
        this.armyDAO = armyDAO;
        this.kingdomDAO = kingdomDAO;
        this.userDAO = userDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            if (context.getChannel() == null) return CommandResponse.errorResponse("Command can only be used in a channel on IRC");

            if (params.containsKey("armies_home")) {
                return slapArmiesHome(context, filters);
            } else if (params.containsKey("admins")) {
                Collection<BotUser> adminUsers = userDAO.getAdminUsers();
                return CommandResponse.resultResponse("users", getOnlineUsers(context.getChannel(), adminUsers));
            } else if (params.containsKey("all")) {
                IRCChannel channel = context.getChannel();
                return CommandResponse.resultResponse("users", channel.getUsers());
            } else if (params.containsKey("event")) {
                return slapEvent(context, params);
            } else if (params.containsKey("racepers")) {
                return slapRacePers(context, params, filters);
            } else {
                IRCChannel channel = context.getChannel();
                List<IRCUser> users = new ArrayList<>(channel.getUsers().size());
                for (IRCUser ircUser : channel.getUsers()) {
                    if (ircUser.isAuthenticated()) users.add(ircUser);
                }
                return CommandResponse.resultResponse("users", users);
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private CommandResponse slapArmiesHome(IRCContext context, Collection<Filter<?>> filters) {
        Kingdom selfKd = kingdomDAO.getSelfKD();
        if (selfKd == null) return CommandResponse.errorResponse("No self kd added, so no provinces either");
        List<Province> provs = armyDAO.getProvincesWithFullArmyHome(selfKd.getLocation());
        FilterUtil.applyFilters(provs, filters);
        List<BotUser> users = new ArrayList<>(provs.size());
        for (Province prov : provs) {
            if (prov.getProvinceOwner() != null) users.add(prov.getProvinceOwner());
        }
        List<IRCUser> onlineUsers = getOnlineUsers(context.getChannel(), users);
        if (onlineUsers.isEmpty()) return CommandResponse.errorResponse("No users to slap");
        return CommandResponse.resultResponse("users", onlineUsers);
    }

    private CommandResponse slapEvent(IRCContext context, Params params) {
        Event.EventType eventType = Event.EventType.fromName(params.getParameter("event"));
        if (eventType == Event.EventType.EVENT && !params.containsKey("id")) return CommandResponse.errorResponse("No id specified...");
        Event event = eventType == Event.EventType.EVENT ? eventDAO.getEvent(params.getLongParameter("id")) : eventDAO.getWave();
        if (event == null) return CommandResponse.errorResponse("No event found");

        AttendanceType attendanceType = AttendanceType.fromString(params.getParameter("attendanceStatus"));
        List<BotUser> users = new ArrayList<>(event.getAttendanceInformation().size());
        for (AttendanceStatus status : event.getAttendanceInformation()) {
            if (status.getType() == attendanceType) users.add(status.getUser());
        }
        List<IRCUser> onlineUsers = getOnlineUsers(context.getChannel(), users);
        if (onlineUsers.isEmpty()) return CommandResponse.errorResponse("No users to slap");
        return CommandResponse.resultResponse("users", onlineUsers);
    }

    private CommandResponse slapRacePers(IRCContext context, Params params, Collection<Filter<?>> filters) {
        Kingdom selfKd = kingdomDAO.getSelfKD();
        if (selfKd == null) return CommandResponse.errorResponse("No self kd added, so no provinces either");
        List<Province> provinces = selfKd.getSortedProvinces();

        Pattern racePattern = Pattern.compile(access.getRaceGroup(), Pattern.CASE_INSENSITIVE);
        Matcher matcher = racePattern.matcher(params.getParameter("racepers"));
        while (matcher.find()) {
            filters.add(raceFilterBuilder.parseAndBuild(matcher.group(0)));
        }

        Pattern persPattern = Pattern.compile(access.getPersonalityGroup(), Pattern.CASE_INSENSITIVE);
        matcher = persPattern.matcher(params.getParameter("racepers"));
        while (matcher.find()) {
            filters.add(personalityFilterBuilder.parseAndBuild(matcher.group(0)));
        }

        FilterUtil.applyFilters(provinces, filters);
        List<BotUser> users = new ArrayList<>(provinces.size());
        for (Province prov : provinces) {
            if (prov.getProvinceOwner() != null) users.add(prov.getProvinceOwner());
        }
        List<IRCUser> onlineUsers = getOnlineUsers(context.getChannel(), users);
        if (onlineUsers.isEmpty()) return CommandResponse.errorResponse("No users to slap");
        return CommandResponse.resultResponse("users", onlineUsers);
    }

    private static List<IRCUser> getOnlineUsers(final IRCChannel channel, final Collection<BotUser> botUsers) {
        List<IRCUser> users = new ArrayList<>(botUsers.size());
        for (BotUser botUser : botUsers) {
            Set<IRCUser> currentUsers = channel.getUsersFromMainnick(botUser.getMainNick());
            if (!currentUsers.isEmpty()) users.addAll(currentUsers);
        }
        return users;
    }
}
