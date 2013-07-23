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

package api.irc.communication;

import api.irc.entities.IRCEntity;
import lombok.Getter;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A message/command that may be sent to IRC, also containing information about priority and such
 */
@Getter
@ParametersAreNonnullByDefault
public final class IRCMessage {
    /**
     * The priority of this message
     */
    private final int priority;
    /**
     * The target of the message
     */
    private final IRCEntity target;
    /**
     * Whether the target of this message may only receive messages from a single bot instance at a time
     * (meaning it needs to be blocked from communication for everybody else while we're sending this message)
     */
    private final boolean blockingRequired;
    /**
     * The raw message/command to send
     */
    private final String ircCommand;
    /**
     * Whether the bot instance that received the message that prompted this response should be used to send the response
     */
    private final boolean handlingReceiverUsed;

    private final String rawMessage;
    private final IRCMessageType type;

    IRCMessage(final IRCMessageType type, final IRCEntity target, final int priority, final String message,
               final boolean useReceivingHandler) {
        this.type = type;
        this.priority = priority;
        this.target = target;
        this.handlingReceiverUsed = useReceivingHandler;
        this.blockingRequired = target.requiresOutputBlocking(type);
        this.ircCommand = type.formatCommand(target.getName(), message);
        this.rawMessage = message;
    }

    IRCMessage(final int priority, final String ircCommand, final boolean useReceivingHandler) {
        this.priority = priority;
        this.handlingReceiverUsed = useReceivingHandler;
        this.target = null;
        this.blockingRequired = false;
        this.ircCommand = checkNotNull(ircCommand);
        this.rawMessage = "";
        this.type = null;
    }
}
