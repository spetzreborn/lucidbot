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

package internal.irc.communication;

import api.events.irc.TopicEvent;
import api.events.irc.UserListEvent;
import api.irc.BotIRCInstance;
import api.irc.entities.IRCUserOpType;
import com.google.common.eventbus.EventBus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static api.tools.text.StringUtil.splitOnSpace;

/**
 * Server codes
 */
@ParametersAreNonnullByDefault
public enum ServerCode {
    WELCOME(1), YOURHOST(2), CREATED(3), MYINFO(4), BOUNCE(5),

    TRACELINK(200), TRACECONNECTING(201), TRACEHANDSHAKE(202), TRACEUNKNOWN(203), TRACEOPERATOR(204), TRACEUSER(205),
    TRACESERVER(206), TRACENEWTYPE(208), TRACECLASS(209),

    STATSLINKINFO(211), STATSCOMMANDS(212), ENDOFSTATS(219),

    UMODEIS(221),

    SERVLIST(234), SERVLISTEND(235),

    STATSUPTIME(242), STATSOLINE(243),

    LUSERCLIENT(251), LUSEROP(252), LUSERUNKNOWN(253), LUSERCHANNELS(254), LUSERME(255), ADMINME(256), ADMINLOC1(257),
    ADMINLOC2(258), ADMINEMAIL(259),

    TRACELOG(261), TRACEEND(262), TRYAGAIN(263),

    NONE(300), AWAY(301), USERHOST(302), ISON(303), UNAWAY(305), NOWAWAY(306),

    WHOISUSER(311), WHOISSERVER(312), WHOISOPERATOR(313), WHOWASUSER(314), ENDOFWHO(315), WHOISCHANOP(316), WHOISIDLE(317),
    ENDOFWHOIS(318), WHOISCHANNELS(319),

    LISTSTART(321), LIST(322), LISTEND(323), CHANNELMODEIS(324), UNIQOPIS(325),

    NOTOPIC(331), TOPIC(332) {
        @Override
        void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final String message) {
            super.fireEvent(eventBus, instance, message);
            String channel = message.substring(message.indexOf('#'), message.indexOf(':') - 1);
            String topic = message.substring(message.indexOf(':') + 1);

            eventBus.post(new TopicEvent(instance, channel, topic));
        }
    },

    INVITING(341), SUMMONING(342), INVITELIST(346), ENDOFINVITELIST(347), EXCEPTLIST(348), ENDOFEXCEPTLIST(349),

    VERSION(351), WHOREPLY(352), NAMREPLY(353) {
        @Override
        void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final String message) {
            super.fireEvent(eventBus, instance, message);
            int channelEndIndex = message.indexOf(" :");
            String channel = message.substring(message.indexOf('#'), channelEndIndex);

            String[] users = splitOnSpace(message.substring(channelEndIndex + 2));
            Map<String, Set<IRCUserOpType>> userInfo = new HashMap<>(users.length);
            for (String nick : users) {
                int actualNickStart;
                Set<IRCUserOpType> prefixes = new HashSet<>();
                for (actualNickStart = 0; actualNickStart < nick.length(); ++actualNickStart) {
                    IRCUserOpType prefix = IRCUserOpType.getOnlySymbols(nick.charAt(actualNickStart));
                    if (prefix == null) break;
                    prefixes.add(prefix);
                }
                String actualNick = nick.substring(actualNickStart);
                boolean hasImplicitOp = false;
                for (IRCUserOpType type : prefixes) {
                    if (type.compareTo(IRCUserOpType.OP) > 0) {
                        hasImplicitOp = true;
                        break;
                    }
                }
                if (hasImplicitOp) prefixes.add(IRCUserOpType.OP);
                userInfo.put(actualNick, prefixes);
            }
            eventBus.post(new UserListEvent(instance, channel, userInfo));
        }
    },

    LINKS(364), ENDOFLINKS(365), ENDOFNAMES(366), BANLIST(367), ENDOFBANLIST(368), ENDOFWHOWAS(369),

    INFO(371), MOTD(372), ENDOFINFO(374), MOTDSTART(375), ENDOFMOTD(376),

    YOUREOPER(381), REHASHING(382), YOURESERVICE(383),

    TIME(391), USERSSTART(392), USERS(393), ENDOFUSERS(394), NOUSERS(395);

    private static final Map<Integer, ServerCode> CONSTANTS_MAP;

    static {
        ServerCode[] constants = values();
        CONSTANTS_MAP = new HashMap<>();
        for (ServerCode constant : constants) {
            CONSTANTS_MAP.put(constant.code, constant);
        }
    }

    public static ServerCode getFromCode(final int code) {
        return CONSTANTS_MAP.get(code);
    }

    private final int code;

    ServerCode(final int code) {
        this.code = code;
    }

    void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final String message) {
        instance.getInstanceLogger().info(message);
        //default implementation does nothing. Override for those who want to act on it. call super if logging is appropriate
    }

    public boolean hasCode(final int code) {
        return code == this.code;
    }
}
