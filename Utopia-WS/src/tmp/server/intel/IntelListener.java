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
package tmp.server.intel;

import api.runtime.ThreadingManager;
import api.settings.PropertiesCollection;
import api.tools.common.CleanupUtil;
import lombok.extern.log4j.Log4j;
import spi.web.WebService;
import tools.UtopiaPropertiesConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * The server socket listening for connections from the forum agent site
 */
@Singleton
@Log4j
public class IntelListener implements WebService {
    private final PropertiesCollection properties;
    private final int webServerPort;
    private final ThreadingManager threadingManager;
    private final IntelHostFactory intelHostFactory;

    private ServerSocket serverSocket;

    @Inject
    public IntelListener(final PropertiesCollection properties, final ThreadingManager threadingManager,
                         final IntelHostFactory intelHostFactory) {
        this.properties = properties;
        this.webServerPort = properties.getInteger(UtopiaPropertiesConfig.SEPERATE_INTEL_SERVER_PORT);
        this.threadingManager = threadingManager;
        this.intelHostFactory = intelHostFactory;
    }

    @Override
    public void start() {
        try {
            serverSocket = new ServerSocket(webServerPort);
            while (true) {
                try {
                    threadingManager.execute(intelHostFactory.createIntelHost(serverSocket.accept()));
                } catch (final IOException e) {
                    return;
                }
            }
        } catch (IOException ignore) {
            // we've been interrupted, so it's time to shut down
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.getBoolean(UtopiaPropertiesConfig.SEPERATE_INTEL_SERVER);
    }

    @Override
    public Runnable getShutdownRunner() {
        return new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        };
    }

    private void shutdown() {
        CleanupUtil.closeSilently(serverSocket);
    }
}
