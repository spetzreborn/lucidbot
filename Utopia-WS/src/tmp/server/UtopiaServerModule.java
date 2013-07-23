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

package tmp.server;

import api.settings.PropertiesCollection;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.eclipse.jetty.server.handler.ContextHandler;
import spi.web.ServerContextHandler;
import spi.web.WebService;
import tmp.server.intel.IntelListener;
import tmp.server.intel.JettyIntelHandler;
import tmp.server.intel.JettyNewsHandler;
import tmp.server.intel.JettyPastedIntelHandler;
import tools.UtopiaPropertiesConfig;

import javax.inject.Inject;

public class UtopiaServerModule extends AbstractModule {
    @Override
    protected void configure() {
        requireBinding(PropertiesCollection.class);

        Multibinder<ServerContextHandler> serverContextsBinder = Multibinder.newSetBinder(binder(), ServerContextHandler.class);
        serverContextsBinder.addBinding().to(ForumAgentContextHandler.class);
        serverContextsBinder.addBinding().to(NewsContextHandler.class);
        serverContextsBinder.addBinding().to(PastedIntelContextHandler.class);

        Multibinder<WebService> servicesBinder = Multibinder.newSetBinder(binder(), WebService.class);
        servicesBinder.addBinding().to(IntelListener.class);
    }

    private static class ForumAgentContextHandler implements ServerContextHandler {
        private final ContextHandler contextHandler;
        private final PropertiesCollection properties;

        @Inject
        public ForumAgentContextHandler(final JettyIntelHandler handler, final PropertiesCollection properties) {
            this.properties = properties;
            contextHandler = new ContextHandler();
            contextHandler.setContextPath("/ForumAgent");
            contextHandler.setResourceBase(".");
            contextHandler.setClassLoader(Thread.currentThread().getContextClassLoader());
            contextHandler.setHandler(handler);
        }

        @Override
        public ContextHandler getContextHandler() {
            return contextHandler;
        }

        @Override
        public boolean isEnabled() {
            return !properties.getBoolean(UtopiaPropertiesConfig.SEPERATE_INTEL_SERVER);
        }
    }

    private static class NewsContextHandler implements ServerContextHandler {
        private final ContextHandler contextHandler;
        private final PropertiesCollection properties;

        @Inject
        public NewsContextHandler(final JettyNewsHandler handler, final PropertiesCollection properties) {
            this.properties = properties;
            contextHandler = new ContextHandler();
            contextHandler.setContextPath("/News");
            contextHandler.setResourceBase(".");
            contextHandler.setClassLoader(Thread.currentThread().getContextClassLoader());
            contextHandler.setHandler(handler);
        }

        @Override
        public ContextHandler getContextHandler() {
            return contextHandler;
        }

        @Override
        public boolean isEnabled() {
            return !properties.getBoolean(UtopiaPropertiesConfig.SEPERATE_INTEL_SERVER);
        }
    }

    private static class PastedIntelContextHandler implements ServerContextHandler {
        private final ContextHandler contextHandler;
        private final PropertiesCollection properties;

        @Inject
        public PastedIntelContextHandler(final JettyPastedIntelHandler handler, final PropertiesCollection properties) {
            this.properties = properties;
            contextHandler = new ContextHandler();
            contextHandler.setContextPath("/Intel");
            contextHandler.setResourceBase(".");
            contextHandler.setClassLoader(Thread.currentThread().getContextClassLoader());
            contextHandler.setHandler(handler);
        }

        @Override
        public ContextHandler getContextHandler() {
            return contextHandler;
        }

        @Override
        public boolean isEnabled() {
            return !properties.getBoolean(UtopiaPropertiesConfig.SEPERATE_INTEL_SERVER);
        }
    }
}
