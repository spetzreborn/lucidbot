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

import api.tools.text.StringUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Contains IRC command formatters
 */
@ParametersAreNonnullByDefault
public interface IrcCommands {
    class AdminCommand {
        private static final String ADMIN = "ADMIN";

        private AdminCommand() {
        }

        public static String format() {
            return ADMIN;
        }
    }

    class InviteCommand {
        private static final String INVITE = "INVITE ";

        private InviteCommand() {
        }

        public static String format(final String nick, final String channel) {
            return INVITE + nick + ' ' + channel;
        }
    }

    class IsOnCommand {
        private static final String ISON = "ISON ";

        private IsOnCommand() {
        }

        public static String format(final String nick) {
            return ISON + nick;
        }
    }

    class JoinCommand {
        private static final String JOIN = "JOIN ";

        private JoinCommand() {
        }

        public static String format(final String channel, @Nullable final String password) {
            return JOIN + channel + (StringUtil.isNullOrEmpty(password) ? "" : ' ' + password);
        }
    }

    class KickCommand {
        private static final String KICK = "KICK ";

        private KickCommand() {
        }

        public static String format(final String channel, final String nick, @Nullable final String reason) {
            return KICK + channel + ' ' + nick + (StringUtil.isNullOrEmpty(reason) ? "" : ' ' + reason);
        }
    }

    class KnockCommand {
        private static final String KNOCK = "KNOCK ";

        private KnockCommand() {
        }

        public static String format(final String channel, @Nullable final String message) {
            return KNOCK + channel + (StringUtil.isNullOrEmpty(message) ? "" : ' ' + message);
        }
    }

    class ListCommand {
        private static final String LIST = "LIST";

        private ListCommand() {
        }

        public static String format(@Nullable final String filter) {
            return LIST + (StringUtil.isNullOrEmpty(filter) ? "" : ' ' + filter);
        }
    }

    class LUsersCommand {
        private static final String LUSERS = "LUSERS";

        private LUsersCommand() {
        }

        public static String format() {
            return LUSERS;
        }
    }

    class ModeCommand {
        private static final String MODE = "MODE ";

        private ModeCommand() {
        }

        public static String format(final String channel, final String modes, @Nullable final String users) {
            return MODE + channel + ' ' + modes + (StringUtil.isNullOrEmpty(users) ? "" : ' ' + users);
        }
    }

    class MotdCommand {
        private static final String MOTD = "MOTD";

        private MotdCommand() {
        }

        public static String format() {
            return MOTD;
        }
    }

    class NamesCommand {
        private static final String NAMES = "NAMES ";

        private NamesCommand() {
        }

        public static String format(final String channel) {
            return NAMES + channel;
        }
    }

    class NickCommand {
        private static final String NICK = "NICK ";

        private NickCommand() {
        }

        public static String format(final String nick) {
            return NICK + nick;
        }
    }

    class NoticeCommand {
        private static final String NOTICE = "NOTICE ";

        private NoticeCommand() {
        }

        public static String format(final String channel, final String message) {
            return NOTICE + channel + " :" + message;
        }
    }

    class PartCommand {
        private static final String PART = "PART ";

        private PartCommand() {
        }

        public static String format(final String channel, @Nullable final String message) {
            return PART + channel + (StringUtil.isNullOrEmpty(message) ? "" : ' ' + message);
        }
    }

    class PassCommand {
        private static final String PASS = "PASS ";

        private PassCommand() {
        }

        public static String format(final String password) {
            return PASS + password;
        }
    }

    class PingCommand {
        private static final String PING = "PING :";

        private PingCommand() {
        }

        public static String format(final String message) {
            return PING + message;
        }
    }

    class PongCommand {
        private static final String PONG = "PONG :";

        private PongCommand() {
        }

        public static String format(final String message) {
            return PONG + message;
        }
    }

    class PrivMsgCommand {
        private static final String PRIVMSG = "PRIVMSG ";

        private PrivMsgCommand() {
        }

        public static String format(final String target, final String message) {
            return PRIVMSG + target + " :" + message;
        }
    }

    class QuitCommand {
        private static final String QUIT = "QUIT";

        private QuitCommand() {
        }

        public static String format(@Nullable final String message) {
            return QUIT + (StringUtil.isNullOrEmpty(message) ? "" : ' ' + message);
        }
    }

    class RulesCommand {
        private static final String RULES = "RULES";

        private RulesCommand() {
        }

        public static String format() {
            return RULES;
        }
    }

    class SilenceCommand {
        private static final String SILENCE = "SILENCE ";

        private SilenceCommand() {
        }

        public static String format(final String details) {
            return SILENCE + details;
        }
    }

    class TimeToUserCommand {
        private TimeToUserCommand() {
        }

        public static String format(final String target, final String date) {
            return NoticeCommand.format(target, "\u0001TIME " + date + '\u0001');
        }
    }

    class TimeToServerCommand {
        private static final String TIME = "TIME";

        private TimeToServerCommand() {
        }

        public static String format() {
            return TIME;
        }
    }

    class TopicCommand {
        private static final String TOPIC = "TOPIC ";

        private TopicCommand() {
        }

        public static String format(final String channel, @Nullable final String topic) {
            return TOPIC + channel + (StringUtil.isNullOrEmpty(topic) ? "" : ' ' + topic);
        }
    }

    class UserCommand {
        private static final String USER = "USER ";

        private UserCommand() {
        }

        public static String format(final String details) {
            return USER + details;
        }
    }

    class UserHostCommand {
        private static final String USERHOST = "USERHOST ";

        private UserHostCommand() {
        }

        public static String format(final String nick) {
            return USERHOST + nick;
        }
    }

    class VersionToUserCommand {
        private VersionToUserCommand() {
        }

        public static String format(final String target, final String version) {
            return NoticeCommand.format(target, "\u0001VERSION " + version + '\u0001');
        }
    }

    class VersionToServerCommand {
        private static final String VERSION = "VERSION";

        private VersionToServerCommand() {
        }

        public static String format() {
            return VERSION;
        }
    }

    class WatchCommand {
        private static final String WATCH = "WATCH ";

        private WatchCommand() {
        }

        public static String format(final String details) {
            return WATCH + details;
        }
    }

    class WhoIsCommand {
        private static final String WHOIS = "WHOIS ";

        private WhoIsCommand() {
        }

        public static String format(final String nick) {
            return WHOIS + nick;
        }
    }

    class WhoWasCommand {
        private static final String WHOWAS = "WHOWAS ";

        private WhoWasCommand() {
        }

        public static String format(final String nick) {
            return WHOWAS + nick;
        }
    }
}
