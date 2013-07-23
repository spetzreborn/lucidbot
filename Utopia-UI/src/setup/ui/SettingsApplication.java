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

import api.settings.PropertiesConfig;
import api.tools.text.StringUtil;
import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
import com.vaadin.ui.Window;
import setup.ui.custom.InstallTab;
import setup.ui.custom.LazyTab;
import setup.ui.custom.LazyTabSheet;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

public class SettingsApplication extends Application {
    private final Set<InstallTab> installTabProviders;
    private final Set<LazyTab> lazyTabProviders;
    private final String allowedIps;
    private boolean installationMode;

    @Inject
    public SettingsApplication(final Set<InstallTab> installTabProviders, final Set<LazyTab> lazyTabProviders,
                               @Named(PropertiesConfig.SETUP_ALLOWED_IPS) final String allowedIps) {
        this.installTabProviders = installTabProviders;
        this.lazyTabProviders = lazyTabProviders;
        this.allowedIps = allowedIps;
    }

    void setInstallationMode(final boolean installationMode) {
        this.installationMode = installationMode;
    }

    @Override
    public void init() {
        Window mainWindow = new Window("LucidBot Setup");
        setMainWindow(mainWindow);

        WebApplicationContext context = (WebApplicationContext) getContext();
        String ipAddress = context.getBrowser().getAddress();
        if (!isAllowedIP(ipAddress)) {
            mainWindow.showNotification("You're not allowed access to this page!", Window.Notification.TYPE_ERROR_MESSAGE);
            return;
        }

        final LazyTabSheet tabSheet = new LazyTabSheet();
        if (installationMode) {
            for (InstallTab tab : installTabProviders) {
                tabSheet.addInstallerTab(tab);
            }
        } else {
            for (LazyTab lazyTab : lazyTabProviders) {
                tabSheet.addLazyTab(lazyTab);
            }
            tabSheet.init();
        }

        mainWindow.addComponent(tabSheet);
    }

    private boolean isAllowedIP(final String ip) {
        String[] ips = StringUtil.splitOnComma(allowedIps);
        if (ips.length == 0) return false;
        for (String allowed : ips) {
            String trimmedAllowed = allowed.trim();
            if (trimmedAllowed.equals(ip) || (containsWildcard(trimmedAllowed) && matchesWithWildcards(trimmedAllowed, ip))) return true;
        }
        return false;
    }

    private static boolean containsWildcard(final String allowed) {
        return allowed.indexOf('*') != -1;
    }

    private static boolean matchesWithWildcards(final String allowed, final String actual) {
        String[] allowedPosts = StringUtil.splitOnPeriod(allowed);
        String[] actualPosts = StringUtil.splitOnPeriod(actual);

        if (allowedPosts.length != actualPosts.length) return false;

        for (int i = 0; i < actualPosts.length; i++) {
            String allowedPost = allowedPosts[i];
            String actualPost = actualPosts[i];
            if (!"*".equals(allowedPost) && !allowedPost.equals(actualPost)) return false;
        }

        return true;
    }
}
