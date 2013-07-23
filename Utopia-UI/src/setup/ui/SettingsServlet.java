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

package setup.ui;

import com.google.inject.Provider;
import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.AbstractApplicationServlet;
import internal.main.Main;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class SettingsServlet extends AbstractApplicationServlet {
    // Private fields
    private final Provider<SettingsApplication> applicationProvider;
    private boolean installationMode;

    @Inject
    public SettingsServlet(final Provider<SettingsApplication> applicationProvider) {
        this.applicationProvider = applicationProvider;
    }

    /**
     * Called by the servlet container to indicate to a servlet that the servlet
     * is being placed into service.
     *
     * @param servletConfig the object containing the servlet's configuration and
     *                      initialization parameters
     * @throws javax.servlet.ServletException if an exception has occurred that interferes with the
     *                                        servlet's normal operation.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void init(final javax.servlet.ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        installationMode = Boolean.parseBoolean(System.getProperty(Main.INSTALLATION_MODE, "false"));
    }

    @Override
    protected Application getNewApplication(final HttpServletRequest request) throws ServletException {
        SettingsApplication application = applicationProvider.get();
        application.setInstallationMode(installationMode);
        return application;
    }

    @Override
    protected Class<? extends Application> getApplicationClass() throws ClassNotFoundException {
        return SettingsApplication.class;
    }
}
