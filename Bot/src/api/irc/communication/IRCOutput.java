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

import api.irc.BotIRCInstance;
import api.irc.entities.IRCEntity;
import api.tools.collections.ListUtil;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import static api.tools.text.StringUtil.slice;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper around irc messages with the same target/recipient meant to be sent by the same bot instance.
 * It's possible to observ this object and be notified when it's empty.
 */
@ParametersAreNonnullByDefault
public final class IRCOutput extends Observable implements Comparable<IRCOutput> {
    private final List<IRCMessage> outputs = new ArrayList<>();
    private final Long created = System.currentTimeMillis();

    /**
     * The bot instance meant to handle this output
     */
    @Getter
    @Setter
    private BotIRCInstance handler;
    /**
     * The target of this output
     */
    @Getter
    private final IRCEntity target;

    private final int priority;
    private final boolean requiresBlocking;

    public IRCOutput(final IRCMessage output, @Nullable final IRCMessage... more) {
        this(null, output, more);
    }

    public IRCOutput(@Nullable final BotIRCInstance handler, final IRCMessage output, @Nullable final IRCMessage... more) {
        outputs.add(checkNotNull(output));
        priority = output.getPriority();
        requiresBlocking = output.isBlockingRequired();
        if (more != null) Collections.addAll(outputs, more);
        this.handler = handler;
        target = output.getTarget();
    }

    /**
     * Adds the specified IRCMessage to this output
     *
     * @param output the message to add
     */
    public void addOutput(final IRCMessage output) {
        outputs.add(checkNotNull(output));
    }

    /**
     * @return the priority of the messages contained in this output
     */
    private int getPriority() {
        return priority;
    }

    /**
     * @return the blocking policy of the contained messages
     */
    public boolean requiresBlocking() {
        return requiresBlocking;
    }

    /**
     * @return removes and returns the first message (in raw form) in this output. Returns null if there are no messages left
     */
    public String poll() {
        if (outputs.isEmpty()) return null;
        String output = outputs.remove(0).getIrcCommand();
        if (outputs.isEmpty()) {
            notifyObservers();
        }
        return output;
    }

    /**
     * Goes through all the messages in this output and makes sure none of them are longer than the specified maxLength.
     * If they are, they're split in as many parts as required.
     *
     * @param maxLength the max length of the messages
     */
    public void conformToMaxLength(final int maxLength) {
        List<IRCMessage> conformed = new ArrayList<>();
        for (IRCMessage output : outputs) {
            if (output.getRawMessage().length() <= maxLength) conformed.add(output);
            else {
                List<String> slices = slice(output.getRawMessage(), maxLength - 1);
                for (String slice : slices) {
                    conformed.add(new IRCMessage(output.getType(), output.getTarget(), output.getPriority(), slice,
                                                 output.isHandlingReceiverUsed()));
                }
            }
        }
        outputs.clear();
        outputs.addAll(conformed);
    }

    @Override
    public int compareTo(final IRCOutput o) {
        int test = Integer.compare(priority, o.priority);
        return test != 0 ? test : created.compareTo(o.created);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IRCOutput)) return false;
        IRCOutput other = (IRCOutput) obj;
        return Objects.equal(this.handler, other.handler) &&
               ListUtil.listContentsAreEqual(outputs, other.outputs) && compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(handler, outputs, priority, created);
    }
}
