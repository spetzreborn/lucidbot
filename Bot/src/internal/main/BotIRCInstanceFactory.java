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

package internal.main;

import api.database.models.BotInstanceSettings;
import api.irc.BotIRCInstance;
import api.irc.OutputQueue;
import api.runtime.ThreadingManager;
import api.settings.PropertiesCollection;
import com.google.common.eventbus.EventBus;
import com.google.inject.Injector;
import internal.irc.ReconnectScheduler;
import internal.irc.communication.ServerCodedCommunication;
import internal.irc.communication.ServerCommandCommunication;
import internal.irc.communication.ServerErrorCommunication;
import internal.irc.delays.DelayHandler;
import internal.irc.delays.DelayStrategy;

import javax.inject.Inject;

import static api.settings.PropertiesConfig.IRC_DELAY_STRATEGY;

public class BotIRCInstanceFactory {
    private final ThreadingManager threadingManager;
    private final PropertiesCollection propertiesCollection;
    private final ServerErrorCommunication serverErrorCommunication;
    private final ServerCodedCommunication serverCodedCommunication;
    private final ServerCommandCommunication serverCommandCommunication;
    private final EventBus eventBus;
    private final OutputQueue outputQueue;
    private final DelayHandler delayHandler;
    private final ReconnectScheduler reconnectScheduler;

    @Inject
    public BotIRCInstanceFactory(final Injector injector) {
        this.reconnectScheduler = injector.getInstance(ReconnectScheduler.class);
        this.threadingManager = injector.getInstance(ThreadingManager.class);
        this.propertiesCollection = injector.getInstance(PropertiesCollection.class);
        this.serverErrorCommunication = injector.getInstance(ServerErrorCommunication.class);
        this.serverCodedCommunication = injector.getInstance(ServerCodedCommunication.class);
        this.serverCommandCommunication = injector.getInstance(ServerCommandCommunication.class);
        this.eventBus = injector.getInstance(EventBus.class);
        this.outputQueue = injector.getInstance(OutputQueue.class);
        DelayStrategy delayStrategy = DelayStrategy.fromNameOrId(propertiesCollection.get(IRC_DELAY_STRATEGY));
        this.delayHandler = injector.getInstance(delayStrategy.getClazz());
    }

    public BotIRCInstance create(final BotInstanceSettings settings) {
        BotIRCInstance instance = new BotIRCInstance(serverErrorCommunication, serverCodedCommunication, serverCommandCommunication,
                eventBus, outputQueue, threadingManager, propertiesCollection, delayHandler,
                reconnectScheduler);
        instance.setSettings(settings);
        eventBus.register(instance);
        return instance;
    }
}
