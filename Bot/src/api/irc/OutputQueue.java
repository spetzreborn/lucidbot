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

package api.irc;

import api.irc.communication.IRCOutput;
import api.irc.entities.IRCEntity;
import internal.irc.WaitingQueuesManager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A queue that handles outgoing messages to the IRC server
 */
@ParametersAreNonnullByDefault
public final class OutputQueue implements Observer {
    private final BlockingQueue<IRCOutput> mainQueue = new PriorityBlockingQueue<>();
    private final ConcurrentMap<BotIRCInstance, BlockingQueue<IRCOutput>> handlerSpecificQueues = new ConcurrentHashMap<>();
    private final WaitingQueuesManager waitingQueuesManager = new WaitingQueuesManager();
    private final Set<IRCEntity> blockedEntities = new HashSet<>();

    /**
     * Puts the specified output in the queue
     *
     * @param output the output to enqueue
     */
    public void enqueueOutput(final IRCOutput output) {
        if (output.requiresBlocking()) {
            output.addObserver(this);
        }
        handleOutputEnqueuing(output);
    }

    /**
     * @return the output from the front of the queue
     * @throws InterruptedException .
     */
    public IRCOutput getNext(final long wait) throws InterruptedException {
        return mainQueue.poll(wait, TimeUnit.MILLISECONDS);
    }

    /**
     * Clears the queue, removing all enqueued items
     */
    public void clear() {
        mainQueue.clear();
        for (BlockingQueue<IRCOutput> queue : handlerSpecificQueues.values()) {
            queue.clear();
        }
        synchronized (blockedEntities) {
            waitingQueuesManager.clearQueues();
            blockedEntities.clear();
        }
    }

    private void handleOutputEnqueuing(final IRCOutput output) {
        synchronized (blockedEntities) {
            if (output.requiresBlocking() && blockedEntities.contains(output.getTarget())) {
                waitingQueuesManager.add(output);
            } else if (output.getHandler() == null) {
                if (waitingQueuesManager.containsHigherPriority(output)) {
                    waitingQueuesManager.add(output);
                } else {
                    mainQueue.add(output);
                }
            } else {
                handlerSpecificQueues.get(output.getHandler()).add(output);
            }
        }
    }

    /**
     * Lets a bot instance register itself as a consumer of this queue
     *
     * @param handler the bot instance
     * @return a BlockingQueue where outputs routed to the specific bot instance are added, meaning outputs that
     *         must be delivered by that instance and no other
     */
    public BlockingQueue<IRCOutput> registerAsHandler(final BotIRCInstance handler) {
        BlockingQueue<IRCOutput> out = new PriorityBlockingQueue<>();
        handlerSpecificQueues.put(checkNotNull(handler), out);
        return out;
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (o instanceof IRCOutput) {
            IRCOutput bo = (IRCOutput) o;
            IRCOutput headOfWaitingQueue;
            synchronized (blockedEntities) {
                blockedEntities.remove(bo.getTarget());
                headOfWaitingQueue = waitingQueuesManager.poll(bo.getTarget());
            }
            if (headOfWaitingQueue != null) {
                handleOutputEnqueuing(headOfWaitingQueue);
            }
        }
    }
}
