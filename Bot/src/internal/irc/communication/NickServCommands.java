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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains nickserv command formatters
 */
@ParametersAreNonnullByDefault
public interface NickServCommands {
    class GhostCommand {
        private GhostCommand() {}

        public static String format(final String nick, final String password) {
            return IrcCommands.PrivMsgCommand.format("NickServ", "GHOST " + nick + ' ' + password);
        }
    }

    class IdentifyCommand {
        private IdentifyCommand() {}

        public static String format(final String password) {
            return IrcCommands.PrivMsgCommand.format("NickServ", "IDENTIFY " + password);
        }
    }

    class InfoCommand {
        private InfoCommand() {}

        public static String format(final String nick) {
            return IrcCommands.PrivMsgCommand.format("NickServ", "INFO " + nick);
        }
    }

    class StatusCommand {
        private StatusCommand() {}

        public static String format(final String nick) {
            return IrcCommands.PrivMsgCommand.format("NickServ", "STATUS " + nick);
        }
    }

    class BulkStatusCommand {
        private BulkStatusCommand() {}

        public static List<String> format(final List<String> nicks, int maxUserPerLine, int maxLineLength) {
            List<String> out = new ArrayList<>();
            StringBuilder builder = new StringBuilder(maxLineLength);
            int counter = 0;
            for (String nick : nicks) {
                if (counter == maxUserPerLine || builder.length() + nick.length() + 1 > maxLineLength) {
                    out.add(IrcCommands.PrivMsgCommand.format("NickServ", "STATUS" + builder));
                    builder = new StringBuilder(maxLineLength);
                    counter = 0;
                }
                builder.append(' ').append(nick);
                ++counter;
            }
            if (counter > 0) out.add(IrcCommands.PrivMsgCommand.format("NickServ", "STATUS" + builder));
            return out;
        }
    }
}
