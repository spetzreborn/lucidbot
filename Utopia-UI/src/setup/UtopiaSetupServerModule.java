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

package setup;

import api.settings.BasicSetup;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import setup.ui.SettingsServlet;
import setup.ui.SetupModule;
import setup.ui.custom.LazyTab;
import setup.ui.custom.VerticalLayoutLazyTab;
import setup.ui.panel.CommandsSettingsPanel;
import setup.ui.panel.StatusPanel;
import spi.web.ServerContextHandler;

import javax.inject.Inject;

@BasicSetup
public class UtopiaSetupServerModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new SetupModule());

        Multibinder<LazyTab> lazyTabMultibinder = Multibinder.newSetBinder(binder(), LazyTab.class);
        lazyTabMultibinder.addBinding().toProvider(CommandSettingsPanelProvider.class);
        lazyTabMultibinder.addBinding().toProvider(StatusSettingsPanelProvider.class);

        Multibinder<ServerContextHandler> multibinder = Multibinder.newSetBinder(binder(), ServerContextHandler.class);
        multibinder.addBinding().to(SetupUIContextHandler.class);
    }

    private static class CommandSettingsPanelProvider implements Provider<VerticalLayoutLazyTab> {
        private final VerticalLayoutLazyTab tab;

        @Inject
        public CommandSettingsPanelProvider(final Provider<CommandsSettingsPanel> panelProvider) {
            this.tab = new VerticalLayoutLazyTab(panelProvider, "Commands");
        }

        @Override
        public VerticalLayoutLazyTab get() {
            return tab;
        }
    }

    private static class StatusSettingsPanelProvider implements Provider<VerticalLayoutLazyTab> {
        private final VerticalLayoutLazyTab tab;

        @Inject
        public StatusSettingsPanelProvider(final Provider<StatusPanel> panelProvider) {
            this.tab = new VerticalLayoutLazyTab(panelProvider, "Status");
        }

        @Override
        public VerticalLayoutLazyTab get() {
            return tab;
        }
    }

    private static class SetupUIContextHandler implements ServerContextHandler {
        private final ServletContextHandler contextHandler;

        @Inject
        public SetupUIContextHandler(final SettingsServlet servlet) {
            contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            contextHandler.setContextPath("/Setup");
            contextHandler.setResourceBase(".");
            contextHandler.setClassLoader(Thread.currentThread().getContextClassLoader());
            contextHandler.addServlet(new ServletHolder(servlet), "/*");
        }

        @Override
        public ContextHandler getContextHandler() {
            return contextHandler;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
