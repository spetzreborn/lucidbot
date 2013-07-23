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
import api.database.daos.BotInstanceSettingsDAO;
import api.events.DelayedEventPoster;
import api.irc.IRCEntityManager;
import api.irc.communication.IRCAccess;
import api.irc.entities.IRCChannel;
import api.irc.entities.IRCUser;
import api.irc.entities.IRCUserOpType;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import com.google.inject.Provider;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class SilenceCommandHandler implements CommandHandler {
    private final ConcurrentMap<String, List<UserWithOp>> silenceMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> keyMap = new ConcurrentHashMap<>();
    private final IRCAccess ircAccess;
    private final Provider<BotInstanceSettingsDAO> botInstanceSettingsDAOProvider;
    private final IRCEntityManager entityManager;

    @Inject
    public SilenceCommandHandler(final IRCAccess ircAccess, final Provider<BotInstanceSettingsDAO> botInstanceSettingsDAOProvider,
                                 final IRCEntityManager entityManager) {
        this.ircAccess = ircAccess;
        this.botInstanceSettingsDAOProvider = botInstanceSettingsDAOProvider;
        this.entityManager = entityManager;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        IRCChannel channel = params.containsKey("channel") ? entityManager.getChannel(params.getParameter("channel")) :
                context.getChannel();
        if (channel == null) return CommandResponse.errorResponse("Unknown channel");
        if (keyMap.get(channel.getName()) == null) keyMap.putIfAbsent(channel.getName(), new Object());
        Set<String> botNicks = botInstanceSettingsDAOProvider.get().getBotNicks();
        if (!params.containsKey("reset")) {
            synchronized (keyMap.get(channel.getName())) {
                silenceMap.putIfAbsent(channel.getName(), new ArrayList<UserWithOp>());
                List<UserWithOp> nonAdminUsers = silenceMap.get(channel.getName());
                for (IRCUser user : channel.getUsers()) {
                    if (botNicks.contains(user.getCurrentNick())) continue;

                    IRCUserOpType highestOp = channel.getHighestOp(user);
                    if (!user.isAdmin() && highestOp != null && highestOp.compareTo(IRCUserOpType.SUPER_OP) < 0) {
                        for (IRCUserOpType type : channel.getUserOps(user)) {
                            nonAdminUsers.add(new UserWithOp(user, type));
                        }
                    }
                }

                ircAccess.setMode(null, channel.getName(), "+m", null);
                for (String[] modeString : getModeChangeStrings(nonAdminUsers, '-')) {
                    ircAccess.setMode(null, channel.getName(), modeString[0], modeString[1]);
                }
            }
            return CommandResponse.resultResponse("silenceSet", true);
        } else {
            synchronized (keyMap.get(channel.getName())) {
                ircAccess.setMode(null, channel.getName(), "-m", null);
                List<UserWithOp> nonAdminUsers = silenceMap.remove(channel.getName());
                if (nonAdminUsers != null && !nonAdminUsers.isEmpty()) {
                    Collections.reverse(nonAdminUsers);
                    for (String[] modeString : getModeChangeStrings(nonAdminUsers, '+')) {
                        ircAccess.setMode(null, channel.getName(), modeString[0], modeString[1]);
                    }
                }
            }
            return CommandResponse.resultResponse("silenceSet", false);
        }
    }

    private static Iterable<String[]> getModeChangeStrings(final Iterable<UserWithOp> list, final char actionSign) {
        StringBuilder ops = new StringBuilder(13).append(actionSign);
        StringBuilder nicks = new StringBuilder(350);
        Collection<String[]> out = new ArrayList<>();
        for (UserWithOp uwo : list) {
            if (ops.length() == 13) {
                out.add(new String[]{ops.toString(), nicks.toString()});
                ops = new StringBuilder(13).append(actionSign);
                nicks = new StringBuilder(350);
            }
            ops.append(uwo.op.getLetter());
            nicks.append(uwo.user.getName()).append(' ');
        }
        if (ops.length() > 0) out.add(new String[]{ops.toString(), nicks.toString()});
        return out;
    }

    private static class UserWithOp {
        public final IRCUser user;
        public final IRCUserOpType op;

        private UserWithOp(IRCUser user, IRCUserOpType op) {
            this.user = user;
            this.op = op;
        }
    }
}
