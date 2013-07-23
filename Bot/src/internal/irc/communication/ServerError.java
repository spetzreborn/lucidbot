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

import api.events.irc.InviteRequiredEvent;
import api.irc.BotIRCInstance;
import com.google.common.eventbus.EventBus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * Server error codes
 */
@ParametersAreNonnullByDefault
public enum ServerError {
    //40x
    NOSUCHNICK(401), NOSUCHSERVER(402), NOSUCHCHANNEL(403), CANNOTSENDTOCHAN(404), TOOMANYCHANNELS(405),
    WASNOSUCHNICK(406), TOOMANYTARGETS(407), NOSUCHSERVICE(408), NOORIGIN(409),

    //41x
    NORECIPIENT(411), NOTEXTTOSEND(412), NOTOPLEVEL(413), WILDTOPLEVEL(414), BADMASK(415),

    //42x
    UNKNOWNCOMMAND(421), NOMOTD(422), NOADMININFO(423), FILEERROR(424),

    //43x
    NONICKNAMEGIVEN(431), ERRONEUSNICKNAME(432), NICKNAMEINUSE(433), NICKCOLLISION(436), UNAVAILRESOURCE(437),

    //44x
    USERNOTINCHANNEL(441), NOTONCHANNEL(442), USERONCHANNEL(443), NOLOGIN(444), SUMMONDISABLED(445), USERSDISABLED(446),

    //45x
    NOTREGISTERED(451),

    //46x
    NEEDMOREPARAMS(461), ALREADYREGISTRED(462), NOPERMFORHOST(463), PASSWDMISMATCH(464), YOUREBANNEDCREEP(465),
    YOUWILLBEBANNED(466), KEYSET(467),

    //47x
    CHANNELISFULL(471) {
        @Override
        void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final String message) {
            super.fireEvent(eventBus, instance, message);
            int channelEndIndex = message.indexOf(" :");
            String channel = message.substring(message.indexOf('#'), channelEndIndex);

            eventBus.post(new InviteRequiredEvent(instance, channel));
        }
    }, UNKNOWNMODE(472), INVITEONLYCHAN(473) {
        @Override
        void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final String message) {
            super.fireEvent(eventBus, instance, message);
            int channelEndIndex = message.indexOf(" :");
            String channel = message.substring(message.indexOf('#'), channelEndIndex);

            eventBus.post(new InviteRequiredEvent(instance, channel));
        }
    }, BANNEDFROMCHAN(474), BADCHANNELKEY(475) {
        @Override
        void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final String message) {
            super.fireEvent(eventBus, instance, message);
            int channelEndIndex = message.indexOf(" :");
            String channel = message.substring(message.indexOf('#'), channelEndIndex);

            eventBus.post(new InviteRequiredEvent(instance, channel));
        }
    }, BADCHANMASK(476),
    NOCHANMODES(477), BANLISTFULL(478),

    //48x
    NOPRIVILEGES(481), CHANOPRIVSNEEDED(482), CANTKILLSERVER(483), RESTRICTED(484), UNIQOPPRIVSNEEDED(485),

    //49x
    NOOPERHOST(491),

    //50x
    UMODEUNKNOWNFLAG(501), USERSDONTMATCH(502);

    private static final Map<Integer, ServerError> CONSTANTS_MAP;

    static {
        ServerError[] constants = values();
        CONSTANTS_MAP = new HashMap<>();
        for (ServerError constant : constants) {
            CONSTANTS_MAP.put(constant.code, constant);
        }
    }

    private final int code;

    ServerError(final int code) {
        this.code = code;
    }

    public boolean hasCode(final int code) {
        return code == this.code;
    }

    void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final String message) {
        instance.getInstanceLogger().info(message);
        //default implementation does nothing. Override for those who want to act on it. call super if logging is appropriate
    }

    public static ServerError getFromCode(final int code) {
        return CONSTANTS_MAP.get(code);
    }
}
