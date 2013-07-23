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

import api.irc.IRCEntityManager;
import api.irc.OutputQueue;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import spi.events.EventListener;

import javax.inject.Singleton;

public class IRCModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(OutputQueue.class).in(Singleton.class);
        bind(IRCEntityManager.class).in(Singleton.class);
        bind(IRCInputParser.class).in(Singleton.class);
        bind(Authenticator.class).in(Singleton.class);

        Multibinder<EventListener> eventListenerMultibinder = Multibinder.newSetBinder(binder(), EventListener.class);
        eventListenerMultibinder.addBinding().to(IRCEntityManager.class);
        eventListenerMultibinder.addBinding().to(IRCInputParser.class);
        eventListenerMultibinder.addBinding().to(Authenticator.class);
    }
}
