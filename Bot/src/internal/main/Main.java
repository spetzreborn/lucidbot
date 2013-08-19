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

import api.database.DatabaseManager;
import api.database.DatabaseState;
import api.database.daos.BotInstanceSettingsDAO;
import api.database.models.BotInstanceSettings;
import api.events.DirectoryChangeEventObserver;
import api.events.FileSystemWatcher;
import api.events.bot.StartupEvent;
import api.irc.BotIRCInstance;
import api.irc.OutputQueue;
import api.runtime.ServiceLocator;
import api.runtime.ThreadingManager;
import api.settings.BasicSetup;
import api.settings.PluginServiceLoader;
import api.settings.PropertiesCollection;
import api.settings.PropertiesConfig;
import com.google.common.eventbus.EventBus;
import com.google.inject.*;
import internal.web.JettyServer;
import lombok.extern.log4j.Log4j;
import spi.events.EventListener;
import spi.runtime.RequiresShutdown;
import spi.web.WebService;

import javax.inject.Singleton;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static api.settings.PropertiesConfig.AUTO_CONNECT_STARTUP;
import static api.settings.PropertiesConfig.TRAY_TOOL_TIP;
import static api.tools.collections.CollectionUtil.isNotEmpty;

@Log4j
@Singleton
public final class Main {
    public static final String VERSION = "3.0RC2";
    public static final int DB_VERSION = 10;
    public static final String INSTALLATION_MODE = "lucidbot.installationMode";
    public static final long STARTUP_TIME = System.currentTimeMillis();

    private final Set<BotIRCInstance> botIRCInstances = new HashSet<>();
    private Injector injector;
    private BotTrayControl trayControl;
    private volatile boolean isFirstConnect = true;

    Main() {
    }

    private void start() {
        trayControl = new BotTrayControl(this);

        injector = Guice.createInjector(new StartupModule(), new BasicsModule());

        DatabaseManager databaseManager = injector.getInstance(DatabaseManager.class);
        DatabaseState databaseState = databaseManager.getDatabaseState();
        switch (databaseState) {
            case NOT_CONNECTED:
            case CONNECTED_UNINSTALLED_DB:
                Main.log.warn("Database is either broken or not installed yet/correctly. Running in installation mode...");
                loadInstallationMode();
                break;
            case CONNECTED_OUTDATED_DB:
                Main.log.warn("Database is outdated, attempting to upgrade...");
                databaseManager.updateSchema();
                databaseState = databaseManager.getDatabaseState();
                if (databaseState != DatabaseState.CONNECTED_FULLY) {
                    Main.log.error("Failed to update the database to the latest version.");
                } else Main.log.warn("Upgrade completed successfully. Continuing...");
                break;
            case CONNECTED_INCOMPLETE_DB:
                Main.log.warn("Database is incomplete, attempting to repair...");
                databaseManager.repairSchema();
                databaseState = databaseManager.getDatabaseState();
                if (databaseState != DatabaseState.CONNECTED_FULLY) {
                    Main.log.error("Failed to repair the database. Please try to install again");
                } else Main.log.warn("Repair completed successfully. Continuing...");
                break;
        }
        if (databaseState == DatabaseState.CONNECTED_FULLY) {
            cleanupTempInjector(injector);
            injector = Guice.createInjector(new StartupModule(), new BasicsModule(), new MainModule(this));
            //Register file monitoring
            FileSystemWatcher fileSystemWatcher = injector.getInstance(FileSystemWatcher.class);
            Set<DirectoryChangeEventObserver> dirObservers = injector
                    .getInstance(Key.get(new TypeLiteral<Set<DirectoryChangeEventObserver>>() {
                    }));
            for (DirectoryChangeEventObserver dirObserver : dirObservers) {
                fileSystemWatcher.registerForDirectoryMonitoring(dirObserver);
            }
            //Start web server
            JettyServer jettyServer = injector.getInstance(JettyServer.class);
            ThreadingManager threadingManager = injector.getInstance(ThreadingManager.class);
            threadingManager.submitInfiniteTask(jettyServer);
            Set<WebService> webServices = injector.getInstance(Key.get(new TypeLiteral<Set<WebService>>() {
            }));
            for (WebService webService : webServices) {
                if (webService.isEnabled()) webService.start();
            }
            addBrowseOptionToTray();

            loadBotInstances();
            //Connect to IRC if we're supposed to do that on startup
            PropertiesCollection propertiesCollection = injector.getInstance(PropertiesCollection.class);
            trayControl.setToolTip(propertiesCollection.get(TRAY_TOOL_TIP));
            if (propertiesCollection.getBoolean(AUTO_CONNECT_STARTUP)) {
                connectBots();
            }
        }
    }

    private static void cleanupTempInjector(final Injector injector) {
        for (Key<?> key : injector.getAllBindings().keySet()) {
            if (RequiresShutdown.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
                RequiresShutdown shutdown = (RequiresShutdown) injector.getInstance(key);
                shutdown.getShutdownRunner().run();
            }
        }
    }

    private void loadInstallationMode() {
        JettyServer jettyServer = injector.getInstance(JettyServer.class);
        System.setProperty(INSTALLATION_MODE, "true");
        injector.getInstance(ThreadingManager.class).submitInfiniteTask(jettyServer);
        addBrowseOptionToTray();
    }

    private void addBrowseOptionToTray() {
        if (Desktop.isDesktopSupported() && !GraphicsEnvironment.isHeadless() &&
                Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            MenuItem item = new MenuItem("Open Setup");
            PropertiesCollection properties = injector.getInstance(PropertiesCollection.class);
            final int webServerPort = properties.getInteger(PropertiesConfig.WEB_SERVER_PORT);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent event) {
                    try {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.browse(URI.create("http://127.0.0.1:" + webServerPort + "/Setup/"));
                    } catch (IOException e) {
                        Main.log.error("Failed to open browser", e);
                    }
                }
            });
            trayControl.installMenuItem(item);
        }
    }

    private void registerListeners() {
        EventBus eventBus = injector.getInstance(EventBus.class);
        Set<EventListener> eventListeners = injector.getInstance(Key.get(new TypeLiteral<Set<EventListener>>() {
        }));
        for (EventListener eventListener : eventListeners) {
            eventBus.register(eventListener);
        }
        eventBus.post(new StartupEvent());
    }

    public void loadBotInstances() {
        BotIRCInstanceFactory factory = injector.getInstance(BotIRCInstanceFactory.class);
        MAIN:
        for (BotInstanceSettings settings : injector.getInstance(BotInstanceSettingsDAO.class).getAll()) {
            for (BotIRCInstance botIRCInstance : botIRCInstances) {
                if (botIRCInstance.getNick().equals(settings.getNick())) continue MAIN;
            }
            botIRCInstances.add(factory.create(settings));
        }
    }

    public void removeBotInstance(final String nick) {
        for (Iterator<BotIRCInstance> iter = botIRCInstances.iterator(); iter.hasNext(); ) {
            if (iter.next().getNick().equalsIgnoreCase(nick)) {
                iter.remove();
                break;
            }
        }
    }

    public void syncBotInstance(final String nick, final BotInstanceSettings settings) {
        for (BotIRCInstance instance : botIRCInstances) {
            if (instance.getNick().equals(nick)) {
                instance.setSettings(settings);
                break;
            }
        }
    }

    public void botConnected() {
        if (isFirstConnect) {
            registerListeners();
            isFirstConnect = false;
        }
    }

    public void connectBots() {
        if (isNotEmpty(botIRCInstances)) {
            disconnectBots();
            try {
                for (BotIRCInstance instance : botIRCInstances) {
                    instance.connect();
                    botConnected();
                }
            } catch (Exception e) {
                log.error("Could not connect", e);
            }
        }
    }

    public void disconnectBots() {
        if (isNotEmpty(botIRCInstances)) {
            for (BotIRCInstance instance : botIRCInstances) {
                instance.setDoNotAttemptReconnect(true);
                instance.getShutdownRunner().run();
            }
            OutputQueue outputQueue = injector.getInstance(OutputQueue.class);
            outputQueue.clear();
        }
    }

    public Set<BotIRCInstance> getBotIRCInstances() {
        return Collections.unmodifiableSet(botIRCInstances);
    }

    public void exit() {
        disconnectBots();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore) {
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        new Main().start();
    }

    private static class MainModule extends AbstractModule {
        private final Main main;

        private MainModule(final Main main) {
            this.main = main;
        }

        @Override
        protected void configure() {
            bind(Main.class).toInstance(main);
            bind(EventBus.class).toInstance(new EventBus());
            requestStaticInjection(ServiceLocator.class);

            //Install plugin modules
            PluginServiceLoader<AbstractModule> pluginModulesLoader = PluginServiceLoader
                    .newLoaderFor(AbstractModule.class, "plugins");
            for (AbstractModule module : pluginModulesLoader.getInstances()) {
                if (!module.getClass().isAnnotationPresent(BasicSetup.class)) {
                    try {
                        install(module);
                    } catch (Exception e) {
                        Main.log.error("Failed to load plugin module", e);
                    }
                }
            }
        }
    }

    private static class BotTrayControl {
        private final Main main;
        private TrayIcon trayIcon;
        private PopupMenu popup;
        private MenuItem exitItem;

        private BotTrayControl(final Main main) {
            this.main = main;
            loadTray();
        }

        private void loadTray() {
            if (SystemTray.isSupported()) {
                final SystemTray tray = SystemTray.getSystemTray();
                Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/java.png"));

                popup = new PopupMenu();
                exitItem = new MenuItem("Exit");
                exitItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        main.exit();
                    }
                });
                popup.add(exitItem);

                trayIcon = new TrayIcon(image, "LucidBot", popup);
                trayIcon.setImageAutoSize(true);
                try {
                    tray.add(trayIcon);
                } catch (AWTException e) {
                    log.error("Could not add system tray icon", e);
                }
            }
        }

        private void installMenuItem(final MenuItem item) {
            if (popup != null) popup.insert(item, 0);
        }

        private void setToolTip(final String message) {
            if (trayIcon != null) {
                trayIcon.setToolTip(message);
            }
        }
    }
}