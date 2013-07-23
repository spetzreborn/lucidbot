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

package internal.irc;

import api.events.irc.DisconnectEvent;
import api.irc.BotIRCInstance;
import com.google.common.eventbus.EventBus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicBoolean;

@ParametersAreNonnullByDefault
public final class InputThread extends Thread {
    private final BotIRCInstance bot;
    private final BufferedReader reader;
    private final EventBus eventBus;
    private final AtomicBoolean die = new AtomicBoolean();

    public InputThread(final BotIRCInstance bot, final BufferedReader reader, final EventBus eventBus) {
        this.bot = bot;
        this.reader = reader;
        this.eventBus = eventBus;
        setName(bot.getNick() + "-InputThread");
    }

    public void kill() {
        die.set(true);
    }

    @Override
    public void run() {
        while (!die.get()) {
            try {
                String line;
                while ((line = reader.readLine()) != null && !die.get()) {
                    bot.handleLine(line);
                }
                if (line == null) break;
            } catch (InterruptedIOException iioe) {
                try {
                    bot.pingServer();
                } catch (Exception e) {
                    break;
                }
            } catch (IOException e) {
                break;
            }
        }

        if (die.get()) {
            close(reader);
        } else {
            eventBus.post(new DisconnectEvent(bot));
        }
    }

    private static void close(final Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (IOException ignore) {
        }
    }
}
