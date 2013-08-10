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

import api.irc.BotIRCInstance;
import com.google.common.eventbus.EventBus;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that can parse messages with server codes from the IRC server
 */
@ParametersAreNonnullByDefault
public final class ServerCodedCommunication {
    private final Pattern pattern = Pattern.compile(":[^ ]+ (\\d{3}) (.+)");
    private final EventBus eventBus;

    @Inject
    public ServerCodedCommunication(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Parses and handles the message if it's recognized
     *
     * @param instance   the bot instance that received the message
     * @param rawMessage the message received
     * @return true if the message was handled, false if not
     */
    public boolean parseAndHandle(final BotIRCInstance instance, final String rawMessage) {
        Matcher matcher = pattern.matcher(rawMessage);
        if (!matcher.matches()) return false;
        int code = Integer.parseInt(matcher.group(1));
        ServerCode type = ServerCode.getFromCode(code);
        if (type == null) return false;
        type.fireEvent(eventBus, instance, matcher.group(2));
        return true;
    }
}
