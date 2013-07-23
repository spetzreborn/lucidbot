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

package web;

import api.settings.PropertiesCollection;
import com.google.inject.multibindings.Multibinder;
import com.sun.jersey.core.util.FeaturesAndProperties;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import spi.events.EventListener;
import spi.web.ServerContextHandler;
import web.resources.*;
import web.tools.*;

import javax.inject.Inject;
import javax.inject.Singleton;

public class RESTfulWebServiceModule extends JerseyServletModule {
    private static final String REQUEST_FILTERS = "com.sun.jersey.spi.container.ContainerRequestFilters";
    private static final String RESPONSE_FILTERS = "com.sun.jersey.spi.container.ContainerResponseFilters";

    @Override
    protected void configureServlets() {
        Multibinder<ServerContextHandler> serverContextsBinder = Multibinder.newSetBinder(binder(), ServerContextHandler.class);
        serverContextsBinder.addBinding().to(WebServiceAPIContextHandler.class);

        bind(LoginHandler.class).in(Singleton.class);

        Multibinder<EventListener> eventListenerMultibinder = Multibinder.newSetBinder(binder(), EventListener.class);
        eventListenerMultibinder.addBinding().to(LoginHandler.class);

        bind(WebContextFactory.class).in(Singleton.class);

        //Exception handlers
        bind(IllegalArgumentExceptionHandler.class).in(Singleton.class);
        bind(IllegalStateExceptionHandler.class).in(Singleton.class);
        bind(NullPointerExceptionHandler.class).in(Singleton.class);
        bind(ExceptionHandler.class).in(Singleton.class);

        bind(WebContextInjectable.class).in(Singleton.class);

        //Resources
        bind(AidResource.class);
        bind(AlarmResource.class);
        bind(ArmyResource.class);
        bind(AttackResource.class);
        bind(BuildResource.class);
        bind(DragonResource.class);
        bind(EventResource.class);
        bind(ForumPostResource.class);
        bind(ForumSectionResource.class);
        bind(ForumThreadResource.class);
        bind(HelpTopicCollectionResource.class);
        bind(HelpTopicResource.class);
        bind(HonorTitleResource.class);
        bind(IntelResource.class);
        bind(KingdomResource.class);
        bind(NewsResource.class);
        bind(NicknamesResource.class);
        bind(NoteResource.class);
        bind(NotificationResource.class);
        bind(OpResource.class);
        bind(OrderCategoryResource.class);
        bind(OrderResource.class);
        bind(PersonalityResource.class);
        bind(PrivateMessageResource.class);
        bind(ProvinceResource.class);
        bind(QuoteResource.class);
        bind(RaceResource.class);
        bind(ScienceTypeResource.class);
        bind(SpellResource.class);
        bind(TargetResource.class);
        bind(TickChannelMessageResource.class);
        bind(UserActivityResource.class);
        bind(UserCheckinResource.class);
        bind(UserResource.class);
        bind(UserSpellOpTargetResource.class);
        bind(WaitResource.class);
        bind(WebLinkResource.class);
    }

    private static class WebServiceAPIContextHandler implements ServerContextHandler {
        private final ServletContextHandler contextHandler;
        private final PropertiesCollection properties;

        @Inject
        public WebServiceAPIContextHandler(final PropertiesCollection properties,
                                           final SecurityHandler securityHandler,
                                           final GuiceContainer container) {
            this.properties = properties;
            contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            contextHandler.setContextPath("/api");
            contextHandler.setResourceBase(".");
            contextHandler.setClassLoader(Thread.currentThread().getContextClassLoader());
            ServletHolder servletHolder = new ServletHolder(container);
            servletHolder.setInitParameter(FeaturesAndProperties.FEATURE_FORMATTED, "true");
            servletHolder.setInitParameter(FeaturesAndProperties.FEATURE_XMLROOTELEMENT_PROCESSING, "true");
            servletHolder.setInitParameter(REQUEST_FILTERS, "com.sun.jersey.api.container.filter.GZIPContentEncodingFilter");
            servletHolder.setInitParameter(RESPONSE_FILTERS, "com.sun.jersey.server.linking.LinkFilter," +
                    "com.sun.jersey.api.container.filter.GZIPContentEncodingFilter");
            contextHandler.addServlet(servletHolder, "/*");

            securityHandler.setupSecurity(contextHandler);
        }

        @Override
        public ContextHandler getContextHandler() {
            return contextHandler;
        }

        @Override
        public boolean isEnabled() {
            return properties.getBoolean(UtopiaWSPropertiesConfig.WEB_SERVICE_API);
        }
    }
}
