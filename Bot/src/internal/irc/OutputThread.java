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

import api.irc.BotIRCInstance;
import api.irc.OutputQueue;
import api.irc.communication.IRCOutput;
import api.runtime.ThreadingManager;
import internal.irc.delays.DelayHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ParametersAreNonnullByDefault
public final class OutputThread extends Thread {
    private final BufferedWriter writer;
    private final BlockingQueue<IRCOutput> outputs;
    private final int maxLineLength;
    private final DelayHandler delayHandler;

    private final AtomicBoolean die = new AtomicBoolean();
    private Future<?> instanceSpecific;
    private Future<?> main;

    public OutputThread(final String botNick, final BufferedWriter writer, final int maxLineLength, final DelayHandler delayHandler) {
        this.writer = writer;
        this.maxLineLength = maxLineLength;
        this.delayHandler = delayHandler;
        setName(botNick + "-OutputThread");
        outputs = new PriorityBlockingQueue<>();
    }

    public void kill() {
        die.set(true);
        instanceSpecific.cancel(true);
        main.cancel(true);
    }

    public void attachToMainQueue(final BotIRCInstance bot, final OutputQueue outputQueue, final ThreadingManager threadingManager) {
        BlockingQueue<IRCOutput> specificHandlerQueue = outputQueue.registerAsHandler(bot);
        instanceSpecific = threadingManager.submitInfiniteTask(new SpecificHandlerQueueMonitor(specificHandlerQueue, outputs, die));
        main = threadingManager.submitInfiniteTask(new MainQueueMonitor(outputQueue, outputs, die));
    }

    @Override
    public void run() {
        while (!die.get()) {
            IRCOutput output = null;
            try {
                while (!die.get()) {
                    output = outputs.poll(100, TimeUnit.MILLISECONDS);
                    if (output != null) break;
                }
                if (die.get()) return;
            } catch (InterruptedException e) {
                if (isInterrupted()) return;
                continue;
            }
            String line;
            output.conformToMaxLength(maxLineLength);
            while ((line = output.poll()) != null) {
                try {
                    delayHandler.callWithDelay(line, new LineSender(line, this));
                } catch (InterruptedException e) {
                    if (isInterrupted()) return;
                } catch (Exception e) {
                    return;
                }
            }
        }
    }

    /**
     * Sends the specified line to the server. Cuts the message if it's too long
     *
     * @param line the line to send (should ofc be a complete and valid irc command)
     * @throws IOException if something goes "boom"
     */
    public void sendRawLine(final String line) throws IOException {
        synchronized (writer) {
            writer.write(line + "\r\n");
            writer.flush();
        }
    }

    private static class MainQueueMonitor implements Runnable {
        private final OutputQueue outputQueue;
        private final BlockingQueue<IRCOutput> outputs;
        private final AtomicBoolean die;

        private MainQueueMonitor(final OutputQueue outputQueue, final BlockingQueue<IRCOutput> outputs, final AtomicBoolean die) {
            this.outputQueue = outputQueue;
            this.outputs = outputs;
            this.die = die;
        }

        @Override
        public void run() {
            while (!die.get()) {
                IRCOutput output;
                try {
                    output = outputQueue.getNext(100);
                    if (output != null) {
                        outputs.add(output);
                    }
                } catch (InterruptedException e) {
                    if (interrupted()) return;
                }
            }
        }
    }

    private static class SpecificHandlerQueueMonitor implements Runnable {
        private final BlockingQueue<IRCOutput> queue;
        private final BlockingQueue<IRCOutput> outputs;
        private final AtomicBoolean die;

        private SpecificHandlerQueueMonitor(final BlockingQueue<IRCOutput> queue, final BlockingQueue<IRCOutput> outputs,
                                            final AtomicBoolean die) {
            this.queue = queue;
            this.outputs = outputs;
            this.die = die;
        }

        @Override
        public void run() {
            while (!die.get()) {
                IRCOutput output;
                try {
                    output = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (output != null) {
                        outputs.add(output);
                    }
                } catch (InterruptedException e) {
                    if (interrupted()) return;
                }
            }
        }
    }

    private static class LineSender implements Callable<Void> {
        private final String line;
        private final OutputThread outputThread;

        private LineSender(final String line, final OutputThread outputThread) {
            this.line = line;
            this.outputThread = outputThread;
        }

        @Override
        public Void call() throws Exception {
            outputThread.sendRawLine(line);
            return null;
        }
    }
}
