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

import api.events.irc.*;
import api.irc.BotIRCInstance;
import com.google.common.eventbus.EventBus;
import org.apache.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.irc.ValidationType.CHANNEL;
import static api.irc.ValidationType.NICKNAME;
import static api.tools.text.StringUtil.*;

/**
 * Server commands
 */
@ParametersAreNonnullByDefault
public enum ServerCommand {
    //Ping and stuff, CTCP
    PING(":?(?<sender>[^!]+)![^ ]+ PRIVMSG (?<target>" + NICKNAME.getPattern() +
            ") :?\u0001PING (?<message>.+)\u0001") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            eventBus.post(new PingEvent(instance, matcher.group("sender"), matcher.group("message")));
        }
    },
    PONG(":?(?<sender>[^ ]+) PONG [^ ]+ :?(?<message>.+)") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            //Not sure if this is really interesting?
        }
    },
    VERSION(":?(?<sender>[^!]+)![^ ]+ PRIVMSG (?<target>" + NICKNAME.getPattern() +
            ") :?\u0001VERSION\u0001") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            eventBus.post(new VersionEvent(instance, matcher.group("sender")));
        }
    },

    //Joining, leaving channels etc.
    INVITE(":?(?<sender>[^!]+)![^ ]+ INVITE (?<target>" +
            NICKNAME.getPattern() + ") :?(?<message>" + CHANNEL.getPattern() + ").*") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            eventBus.post(new InviteEvent(instance, matcher.group("sender"), matcher.group("message"), matcher.group("target")));
        }
    },
    JOIN(":?(?<sender>[^!]+)![^ ]+ JOIN :?(?<target>" + CHANNEL.getPattern() + ").*") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (instance.isMainInstanceIn(matcher.group("target"))) {
                String sender = matcher.group("sender");
                eventBus.post(new JoinEvent(instance, matcher.group("target"), sender.equals(instance.getNick()) ? null : sender));
            }
        }
    },
    KICK(":?(?<sender>[^!]+)![^ ]+ KICK (?<target>" + CHANNEL.getPattern() +
            ") (?<recipient>" + NICKNAME.getPattern() + ")(?: :?(?<message>.*))?") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (instance.isMainInstanceIn(matcher.group("target"))) {
                eventBus.post(new KickEvent(instance, matcher.group("recipient"), matcher.group("sender"), matcher.group("target"),
                        matcher.group("message")));
            }
        }
    },
    PART(":?(?<sender>[^!]+)![^ ]+ PART (?<target>" + CHANNEL.getPattern() + ").*") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (instance.isMainInstanceIn(matcher.group("target")) && !instance.getNick().equals(matcher.group("sender"))) {
                eventBus.post(new PartEvent(instance, matcher.group("sender"), matcher.group("target")));
            }
        }
    },
    QUIT(":?(?<sender>[^!]+)![^ ]+ QUIT (?<target>[^ :]+) :?(?<message>.*)") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (instance.isMainInstanceIn(matcher.group("target")) && !instance.getNick().equals(matcher.group("sender"))) {
                eventBus.post(new QuitEvent(instance, matcher.group("sender"), matcher.group("message")));
            }
        }
    },
    QUIT_ALT(":?(?<sender>[^!]+)![^ ]+ QUIT :?(?<message>.*)") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (!instance.getNick().equals(matcher.group("sender"))) {
                eventBus.post(new QuitEvent(instance, matcher.group("sender"), matcher.group("message")));
            }
        }
    },

    //Modes and nicks etc.
    MODE(":?(?<sender>[^!]+)![^ ]+ MODE (?<target>" + CHANNEL.getPattern() +
            ") (?<modes>(?:[+-]{1}[a-zA-Z]+)+)\\s*(?:(?<recipients>" + NICKNAME.getPattern() + ")\\s*)*") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (instance.isMainInstanceIn(matcher.group("target")) && !instance.getNick().equals(matcher.group("sender"))) {
                String recipients = matcher.group("recipients");
                eventBus.post(new ModeEvent(instance, matcher.group("target"),
                        isNullOrEmpty(recipients) ? new String[0] : splitOnSpace(recipients), matcher.group("modes")));
            }
        }
    },
    NICK(":?(?<sender>[^!]+)![^ ]+ NICK :?(?<target>" + NICKNAME.getPattern() + ").*") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (!instance.getNick().equals(matcher.group("sender")))
                eventBus.post(new NickChangeEvent(instance, matcher.group("sender"), matcher.group("target")));
        }
    },
    TOPIC(":?(?<sender>[^!]+)![^ ]+ TOPIC (?<target>" + CHANNEL.getPattern() +
            ") :?(?<message>.*)") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (instance.isMainInstanceIn(matcher.group("target"))) {
                eventBus.post(new TopicEvent(instance, matcher.group("target"), matcher.group("message")));
            }
        }
    },

    //Messaging
    ACTION(":?(?<sender>[^!]+)![^ ]+ PRIVMSG (?<target>" + CHANNEL.getPattern() +
            ") :?\u0001ACTION (?<message>.+)\u0001") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (instance.isMainInstanceIn(matcher.group("target")) && !instance.getNick().equals(matcher.group("sender"))) {
                eventBus.post(new ActionEvent(instance, matcher.group("sender"), matcher.group("target"), matcher.group("message")));
            }
        }
    },
    MESSAGE(":?(?<sender>[^!]+)![^ ]+ PRIVMSG (?<target>" + CHANNEL.getPattern() +
            ") :?(?<message>.+)") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            if (instance.isMainInstanceIn(matcher.group("target"))) {
                String channel = matcher.group("target");
                String sender = matcher.group("sender");
                String message = matcher.group("message");
                Logger.getLogger(lowerCase(channel.substring(1))).info('<' + sender + "> " + message);
                if (!instance.getNick().equals(matcher.group("sender")))
                    eventBus.post(new MessageEvent(instance, sender, channel, message));
            }
        }
    },
    NOTICE(":?(?<sender>[^!]+)![^ ]+ NOTICE (?<target>[^ ]+) :?(?<message>.+)") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            super.fireEvent(eventBus, instance, matcher);
            if (!instance.getNick().equals(matcher.group("sender")))
                eventBus.post(new NoticeEvent(instance, matcher.group("sender"), matcher.group("target"), matcher.group("message")));
        }
    },
    PRIVATE_MESSAGE(":?(?<sender>[^!]+)![^ ]+ PRIVMSG (?<target>" +
            NICKNAME.getPattern() + ") :?(?<message>.+)") {
        @Override
        protected void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
            eventBus.post(new PrivateMessageEvent(instance, matcher.group("sender"), matcher.group("message")));
        }
    };

    private final Pattern pattern;

    ServerCommand(final String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    void fireEvent(final EventBus eventBus, final BotIRCInstance instance, final Matcher matcher) {
        instance.getInstanceLogger().info(matcher.group(0));
        //default implementation does nothing. Override for those who want to act on it. call super if logging is appropriate
    }

    /**
     * @param command the command to check
     * @return true if the specified command matches this ServerCommand
     */
    public boolean isCommand(final String command) {
        return pattern.matcher(command).matches();
    }

    /**
     * @return the Pattern for this ServerCommand
     */
    public Pattern getPattern() {
        return pattern;
    }
}
