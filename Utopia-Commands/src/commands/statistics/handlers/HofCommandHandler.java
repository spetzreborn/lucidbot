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

package commands.statistics.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.database.models.UserStatistic;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import com.google.common.collect.Lists;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.*;

public class HofCommandHandler implements CommandHandler {
    private final BotUserDAO userDAO;

    @Inject
    public HofCommandHandler(final BotUserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            SortedMap<String, List<NickWithNumber>> map = new TreeMap<>();
            String wantedType = params.getParameter("type");
            for (BotUser user : userDAO.getAllUsers()) {
                for (UserStatistic stat : user.getStats()) {
                    if (wantedType != null && !stat.getType().equalsIgnoreCase(wantedType)) {
                        //do nothing
                    } else if (map.containsKey(stat.getType())) {
                        map.get(stat.getType()).add(new NickWithNumber(user.getMainNick(), stat.getAmount()));
                    } else {
                        map.put(stat.getType(), Lists.newArrayList(new NickWithNumber(user.getMainNick(), stat.getAmount())));
                    }
                }
            }
            Comparator<NickWithNumber> comparator = new NickWithNumberComparator();
            for (List<NickWithNumber> nickWithNumbers : map.values()) {
                Collections.sort(nickWithNumbers, comparator);
            }
            return CommandResponse.resultResponse("hof", map);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    public static class NickWithNumber {
        private final String nick;
        private final int number;

        private NickWithNumber(String nick, int number) {
            this.nick = nick;
            this.number = number;
        }

        public String getNick() {
            return nick;
        }

        public int getNumber() {
            return number;
        }
    }

    private static class NickWithNumberComparator implements Comparator<NickWithNumber> {
        @Override
        public int compare(NickWithNumber o1, NickWithNumber o2) {
            return o2.number - o1.number;
        }
    }
}
