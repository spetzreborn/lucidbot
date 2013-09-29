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

package internal.web;

import api.tools.files.FileMatcher;
import api.tools.files.FileUtil;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import lombok.extern.log4j.Log4j;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import spi.runtime.RequiresShutdown;
import spi.web.ServerContextHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static api.settings.PropertiesConfig.WEB_SERVER_PORT;

/**
 * A Jetty Server that may be run in a thread pool
 */
@ParametersAreNonnullByDefault
@Log4j
@Singleton
public final class JettyServer implements Runnable, RequiresShutdown {
    private final Server server;
    private final List<ContextHandler> handlers = new ArrayList<>();

    @Inject
    public JettyServer(@Named(WEB_SERVER_PORT) final int port) {
        this.server = new Server();
        ServerConnector serverConnector = new ServerConnector(server, new HttpConnectionFactory());
        serverConnector.setPort(port);
        server.setConnectors(new Connector[]{serverConnector});
    }

    @Inject
    public void init(final Injector injector) {
        try {
            addPluginWebapps(injector);
            addBuiltinHandlers(injector);
            addStaticHtmlHandler();
        } catch (final Exception e) {
            JettyServer.log.error("Failed to load webapps", e);
        }
    }

    private void addPluginWebapps(final Injector injector) throws IOException {
        FileUtil.BotFileVisitor fileVisitor = FileUtil.visitDirectory(Paths.get("webapps"), true, FileMatcher.getFileEndingMatcher(".war"));

        for (Path path : fileVisitor.getFiles()) {
            WebAppContext webapp = new WebAppContext();
            webapp.addEventListener(new GuiceServletContextListener() {
                @Override
                protected Injector getInjector() {
                    return injector;
                }
            });
            webapp.addFilter(GuiceFilter.class, "/*", null);
            String fileName = path.getFileName().toString();
            webapp.setContextPath('/' + fileName.substring(0, fileName.indexOf(".war")));
            webapp.setWar(path.toAbsolutePath().toString());
            webapp.setClassLoader(Thread.currentThread().getContextClassLoader());
            handlers.add(webapp);
        }
    }

    private void addBuiltinHandlers(final Injector injector) {
        for (ServerContextHandler handler : injector.getInstance(Key.get(new TypeLiteral<Set<ServerContextHandler>>() {
        }))) {
            if (handler.isEnabled()) handlers.add(handler.getContextHandler());
        }
    }

    private void addStaticHtmlHandler() {
        ResourceHandler staticResourcesHandler = new ResourceHandler();
        staticResourcesHandler.setDirectoriesListed(false);
        staticResourcesHandler.setWelcomeFiles(new String[0]);
        staticResourcesHandler.setResourceBase("./html");
        ContextHandler staticContextHandler = new ContextHandler("/static");
        staticContextHandler.setHandler(staticResourcesHandler);
        handlers.add(staticContextHandler);
    }

    @Override
    public void run() {
        ContextHandlerCollection collection = new ContextHandlerCollection();
        collection.setHandlers(handlers.toArray(new Handler[handlers.size()]));
        server.setHandler(collection);

        try {
            server.start();
        } catch (Exception e) {
            JettyServer.log.error("", e);
        }
    }

    @Override
    public Runnable getShutdownRunner() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    if (server != null) server.stop();
                } catch (Exception ignore) {
                }
            }
        };
    }
}
