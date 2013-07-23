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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Contains chanserv command formatters
 */
@ParametersAreNonnullByDefault
public interface ChanServCommands {
    class AccessCommand {
        private AccessCommand() {}

        public static String format(final String channel, final String subCommand, @Nullable final String... params) {
            StringBuilder message = new StringBuilder(100);
            message.append("ACCESS ").append(channel).append(' ').append(subCommand);
            if (params != null) {
                for (String param : params) {
                    message.append(' ').append(param);
                }
            }
            return IrcCommands.PrivMsgCommand.format("ChanServ", message.toString());
        }
    }

    class AKickCommand {
        private AKickCommand() {}

        public static String format(final String channel, final String subCommand, @Nullable final String... params) {
            StringBuilder message = new StringBuilder(100);
            message.append("AKICK ").append(channel).append(' ').append(subCommand);
            if (params != null) {
                for (String param : params) {
                    message.append(' ').append(param);
                }
            }
            return IrcCommands.PrivMsgCommand.format("ChanServ", message.toString());
        }
    }

    class AopCommand {
        private AopCommand() {}

        public static String format(final String channel, final String subCommand, @Nullable final String... params) {
            StringBuilder message = new StringBuilder(100);
            message.append("AOP ").append(channel).append(' ').append(subCommand);
            if (params != null) {
                for (String param : params) {
                    message.append(' ').append(param);
                }
            }
            return IrcCommands.PrivMsgCommand.format("ChanServ", message.toString());
        }
    }

    class ClearCommand {
        private ClearCommand() {}

        public static String format(final String subCommand) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "CLEAR " + subCommand);
        }
    }

    class DehalfopCommand {
        private DehalfopCommand() {}

        public static String format(final String channel, final String nick) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "DEHALFOP " + channel + ' ' + nick);
        }
    }

    class DeopCommand {
        private DeopCommand() {}

        public static String format(final String channel, final String nick) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "DEOP " + channel + ' ' + nick);
        }
    }

    class DeprotectCommand {
        private DeprotectCommand() {}

        public static String format(final String channel, final String nick) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "DEPROTECT " + channel + ' ' + nick);
        }
    }

    class DevoiceCommand {
        private DevoiceCommand() {}

        public static String format(final String channel, final String nick) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "DEVOICE " + channel + ' ' + nick);
        }
    }

    class HalfopCommand {
        private HalfopCommand() {}

        public static String format(final String channel, final String nick) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "HALFOP " + channel + ' ' + nick);
        }
    }

    class HopCommand {
        private HopCommand() {}

        public static String format(final String channel, final String subCommand, @Nullable final String... params) {
            StringBuilder message = new StringBuilder(100);
            message.append("HOP ").append(channel).append(' ').append(subCommand);
            if (params != null) {
                for (String param : params) {
                    message.append(' ').append(param);
                }
            }
            return IrcCommands.PrivMsgCommand.format("ChanServ", message.toString());
        }
    }

    class IdentifyCommand {
        private IdentifyCommand() {}

        public static String format(final String channel, final String password) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "IDENTIFY " + channel + ' ' + password);
        }
    }

    class InviteCommand {
        private InviteCommand() {}

        public static String format(final String channel, final String commandDef) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", commandDef.replace("$CHANNEL$", channel));
        }
    }

    class KickCommand {
        private KickCommand() {}

        public static String format(final String channel, final String nick, final String reason) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "KICK " + channel + ' ' + nick + ' ' + reason);
        }
    }

    class OpCommand {
        private OpCommand() {}

        public static String format(final String channel, final String nick) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "OP " + channel + ' ' + nick);
        }
    }

    class ProtectCommand {
        private ProtectCommand() {}

        public static String format(final String channel, final String nick) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "PROTECT " + channel + ' ' + nick);
        }
    }

    class SopCommand {
        private SopCommand() {}

        public static String format(final String channel, final String subCommand, @Nullable final String... params) {
            StringBuilder message = new StringBuilder(100);
            message.append("SOP ").append(channel).append(' ').append(subCommand);
            if (params != null) {
                for (String param : params) {
                    message.append(' ').append(param);
                }
            }
            return IrcCommands.PrivMsgCommand.format("ChanServ", message.toString());
        }
    }

    class StatusCommand {
        private StatusCommand() {}

        public static String format(final String channel, final String nick) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "STATUS " + channel + ' ' + nick);
        }
    }

    class TopicCommand {
        private TopicCommand() {}

        public static String format(final String channel, final String topic) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "TOPIC " + channel + ' ' + topic);
        }
    }

    class UnbanCommand {
        private UnbanCommand() {}

        public static String format(final String channel) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "UNBAN " + channel);
        }
    }

    class VoiceCommand {
        private VoiceCommand() {}

        public static String format(final String channel, final String nick) {
            return IrcCommands.PrivMsgCommand.format("ChanServ", "VOICE " + channel + ' ' + nick);
        }
    }

    class VopCommand {
        private VopCommand() {}

        public static String format(final String channel, final String subCommand, @Nullable final String... params) {
            StringBuilder message = new StringBuilder(100);
            message.append("VOP ").append(channel).append(' ').append(subCommand);
            if (params != null) {
                for (String param : params) {
                    message.append(' ').append(param);
                }
            }
            return IrcCommands.PrivMsgCommand.format("ChanServ", message.toString());
        }
    }
}
