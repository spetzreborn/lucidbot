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

package commands.province_management.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.time.TimeUtil;
import database.daos.ArmyDAO;
import database.daos.ProvinceDAO;
import database.models.*;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BestMatchFinder;

import javax.inject.Inject;
import java.util.*;

import static api.tools.collections.CollectionUtil.isNotEmpty;

public class InfoCommandHandler implements CommandHandler {
    private final ProvinceDAO provinceDAO;
    private final ArmyDAO armyDAO;
    private final BestMatchFinder bestMatchFinder;

    @Inject
    public InfoCommandHandler(final ArmyDAO armyDAO,
                              final ProvinceDAO provinceDAO,
                              final BestMatchFinder bestMatchFinder) {
        this.armyDAO = armyDAO;
        this.provinceDAO = provinceDAO;
        this.bestMatchFinder = bestMatchFinder;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            CommandResponse response = CommandResponse.emptyResponse();

            BotUser user;
            Province province;
            if (params.isEmpty()) {
                user = context.getBotUser();
                province = provinceDAO.getProvinceForUser(user);
            } else if (params.containsKey("kingdom")) {
                province = provinceDAO.getClosestMatch(params.getParameter("userOrProv"));
                if (province == null) return CommandResponse.errorResponse("No such province found");
                user = province.getProvinceOwner();
            } else {
                String userOrProv = params.getParameter("userOrProv");
                province = bestMatchFinder.findBestMatch(userOrProv);
                if (province == null)
                    return CommandResponse.errorResponse("This command requires you to have a province to work");
                user = province.getProvinceOwner();
            }

            if (user != null && user.getStatus() != null) {
                response.put("mainnick", user.getMainNick());
                response.put("status", user.getStatus());
            }

            Collection<Army> armies = null;
            if (province != null) {
                armies = armyDAO.getIRCArmiesForUser(user);

                List<ItemWithTime<DurationOp>> timeSortedOps = timeSortOps(province.getDurationOps());
                if (!timeSortedOps.isEmpty()) response.put("ops", timeSortedOps);

                List<ItemWithTime<DurationSpell>> timeSortedSpells = timeSortSpells(province.getDurationSpells());
                if (!timeSortedSpells.isEmpty()) response.put("spells", timeSortedSpells);

                List<ItemWithTime<Aid>> timeSortedAid = timeSortAid(province.getAid());
                if (!timeSortedAid.isEmpty()) response.put("aid", timeSortedAid);

                if ((armies == null || armies.isEmpty()) && province.getSom() != null) {
                    armies = province.getSom().getArmiesOut();
                }
            }

            if (isNotEmpty(armies)) response.put("armies", timeSortArmies(armies));

            if (response.isEmpty())
                return CommandResponse.errorResponse("That province has no interesting info available");

            return response;
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static List<ItemWithTime<Army>> timeSortArmies(final Collection<Army> armies) {
        List<Army> temp = new ArrayList<>(armies);
        Collections.sort(temp);
        List<ItemWithTime<Army>> out = new ArrayList<>(temp.size());
        for (Army army : temp) {
            out.add(new ItemWithTime(army, TimeUtil.compareTimeToCurrent(army.getReturningDate().getTime())));
        }
        return out;
    }

    private static List<ItemWithTime<DurationOp>> timeSortOps(final Collection<DurationOp> ops) {
        List<DurationOp> temp = new ArrayList<>(ops);
        Collections.sort(temp, new OpRemainderComparator());
        List<ItemWithTime<DurationOp>> out = new ArrayList<>(temp.size());
        for (DurationOp DurationOp : temp) {
            out.add(new ItemWithTime(DurationOp, TimeUtil.compareDateToCurrent(DurationOp.getExpires())));
        }
        return out;
    }

    private static List<ItemWithTime<DurationSpell>> timeSortSpells(final Collection<DurationSpell> ops) {
        List<DurationSpell> temp = new ArrayList<>(ops);
        Collections.sort(temp, new SpellRemainderComparator());
        List<ItemWithTime<DurationSpell>> out = new ArrayList<>(temp.size());
        for (DurationSpell DurationSpell : temp) {
            out.add(new ItemWithTime(DurationSpell, TimeUtil.compareDateToCurrent(DurationSpell.getExpires())));
        }
        return out;
    }

    private static List<ItemWithTime<Aid>> timeSortAid(final Collection<Aid> ops) {
        List<Aid> temp = new ArrayList<>(ops);
        Collections.sort(temp);
        List<ItemWithTime<Aid>> out = new ArrayList<>(temp.size());
        for (Aid aid : temp) {
            out.add(new ItemWithTime(aid, TimeUtil.compareTimeToCurrent(aid.getAdded().getTime())));
        }
        return out;
    }

    private static class OpRemainderComparator implements Comparator<DurationOp> {
        @Override
        public int compare(final DurationOp o1, final DurationOp o2) {
            return o1.getExpires().compareTo(o2.getExpires());
        }
    }

    private static class SpellRemainderComparator implements Comparator<DurationSpell> {
        @Override
        public int compare(final DurationSpell o1, final DurationSpell o2) {
            return o1.getExpires().compareTo(o2.getExpires());
        }
    }

    public static class ItemWithTime<E> {
        private final E item;
        private final String time;

        private ItemWithTime(final E item, final String time) {
            this.item = item;
            this.time = time;
        }

        public E getItem() {
            return item;
        }

        public String getTime() {
            return time;
        }
    }
}
