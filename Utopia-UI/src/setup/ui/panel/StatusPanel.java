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

package setup.ui.panel;

import api.irc.BotIRCInstance;
import api.runtime.ThreadingManager;
import api.timers.TimerManager;
import api.tools.time.TimeUtil;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.*;
import database.CommonEntitiesAccess;
import internal.main.Main;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Log4j
public class StatusPanel extends VerticalLayout {
    private final Button refresh;
    private final Panel ircPanel;
    private final Panel botPanel;
    private final Panel systemPanel;

    @Inject
    public StatusPanel(final TimerManager timerManager, final ThreadingManager threadingManager, final CommonEntitiesAccess cache,
                       final Main main) {
        refresh = new Button("Refresh");
        refresh.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                ((Refreshable) ircPanel).refresh();
                ((Refreshable) botPanel).refresh();
                ((Refreshable) systemPanel).refresh();
            }
        });
        ircPanel = new IRCPanel(main);
        botPanel = new BotPanel(timerManager, cache);
        systemPanel = new SystemPanel(threadingManager);

        addComponent(refresh);
        addComponent(ircPanel);
        addComponent(botPanel);
        addComponent(systemPanel);
        setSizeUndefined();
        setSpacing(true);
        setMargin(true);
    }

    private interface Refreshable {
        void refresh();
    }

    private static class IRCPanel extends Panel implements Refreshable {
        private final Main main;

        private final VerticalLayout root = new VerticalLayout();
        private final Button connectAllButton;
        private final Button disconnectAllButton;
        private final Table botInstancesTable;

        private IRCPanel(final Main main) {
            this.main = main;
            setCaption("IRC Status");
            setWidth("650px");
            addComponent(root);

            connectAllButton = new Button("Connect All");
            connectAllButton.addListener(new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    main.connectBots();
                }
            });

            disconnectAllButton = new Button("Disconnect All");
            disconnectAllButton.addListener(new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    main.disconnectBots();
                }
            });

            botInstancesTable = new Table();
            botInstancesTable.setContainerDataSource(getBotInstanceContainer());
            botInstancesTable.setHeight("300px");
            botInstancesTable.setWidth("600px");
            botInstancesTable.setColumnHeaders(new String[]{"Nick", "Connected", "Identified", "Connect", "Disconnect"});
            botInstancesTable.setCaption("Bot Instances");
            botInstancesTable.setColumnWidth("nick", 175);
            botInstancesTable.setColumnWidth("connected", 75);
            botInstancesTable.setColumnWidth("identified", 75);
            botInstancesTable.setColumnWidth("connect", 90);

            HorizontalLayout buttonsLayout = new HorizontalLayout();
            buttonsLayout.setSpacing(true);
            buttonsLayout.addComponent(connectAllButton);
            buttonsLayout.addComponent(disconnectAllButton);
            root.addComponent(buttonsLayout);
            root.addComponent(botInstancesTable);
            root.setSpacing(true);
        }

        private IndexedContainer getBotInstanceContainer() {
            final IndexedContainer container = new IndexedContainer();

            container.addContainerProperty("nick", String.class, null);
            container.addContainerProperty("connected", Boolean.class, null);
            container.addContainerProperty("identified", Boolean.class, null);
            container.addContainerProperty("connect", Button.class, null);
            container.addContainerProperty("disconnect", Button.class, null);

            main.loadBotInstances();
            for (BotIRCInstance instance : main.getBotIRCInstances()) {
                addBot(instance, container);
            }

            return container;
        }

        private void addBot(final BotIRCInstance bot, final Container container) {
            final Object itemId = container.addItem();
            final Item item = container.getItem(itemId);
            item.getItemProperty("nick").setValue(bot.getNick());
            item.getItemProperty("connected").setValue(bot.isConnected());
            item.getItemProperty("identified").setValue(bot.isIdentified());
            item.getItemProperty("connect").setValue(new Button("Connect", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        bot.connect();
                        main.botConnected();
                    } catch (final IOException e) {
                        StatusPanel.log.warn("Could not connect to IRC", e);
                        getWindow()
                                .showNotification("Could not connect. See error log for details", Window.Notification.TYPE_ERROR_MESSAGE);
                    }
                }
            }));
            item.getItemProperty("disconnect").setValue(new Button("Disconnect", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    bot.setDoNotAttemptReconnect(true);
                    bot.getShutdownRunner().run();
                }
            }));
        }

        @Override
        public void refresh() {
            botInstancesTable.setContainerDataSource(getBotInstanceContainer());
        }
    }

    private static class BotPanel extends Panel implements Refreshable {
        private final TimerManager timerManager;

        private final VerticalLayout root = new VerticalLayout();
        private final Button refreshCacheButton;
        private final Table timerTable;

        private BotPanel(final TimerManager timerManager, final CommonEntitiesAccess cache) {
            this.timerManager = timerManager;

            setCaption("Bot Status");
            setWidth("650px");
            addComponent(root);

            refreshCacheButton = new Button("Reload Cache");
            refreshCacheButton.addListener(new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    cache.reloadAll();
                }
            });
            cache.reloadAll();

            timerTable = new Table();
            timerTable.setContainerDataSource(getBotInstanceContainer());
            timerTable.setHeight("300px");
            timerTable.setWidth("550px");
            timerTable.setColumnHeaders(new String[]{"Timer ID", "Due", "Cancel"});
            timerTable.setColumnWidth("id", 200);
            timerTable.setColumnWidth("due", 200);
            timerTable.setCaption("Current Timers");

            root.addComponent(refreshCacheButton);
            root.addComponent(timerTable);
            root.setSpacing(true);
        }

        private IndexedContainer getBotInstanceContainer() {
            final IndexedContainer container = new IndexedContainer();

            container.addContainerProperty("id", String.class, null);
            container.addContainerProperty("due", String.class, null);
            container.addContainerProperty("cancel", Button.class, null);

            for (String timerId : timerManager.getCurrentTimers()) {
                addTimer(timerId, container);
            }

            return container;
        }

        private void addTimer(final String timerId, final Container container) {
            final Object itemId = container.addItem();
            final Item item = container.getItem(itemId);
            item.getItemProperty("id").setValue(timerId);
            item.getItemProperty("due").setValue(getDue(timerId));
            item.getItemProperty("cancel").setValue(new Button("Cancel", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    timerManager.cancelTimer(timerId);
                    timerTable.removeItem(itemId);
                }
            }));
        }

        private String getDue(final String timerId) {
            long timeLeft = timerManager.getTimeLeft(timerId, TimeUnit.MILLISECONDS);
            return timeLeft <= 0 ? "Expired" : TimeUtil.formatTimeDifference(timeLeft);
        }

        @Override
        public void refresh() {
            timerTable.setContainerDataSource(getBotInstanceContainer());
        }
    }

    private static class SystemPanel extends Panel implements Refreshable {
        private static final int MILLION = 1_000_000;

        private final ThreadingManager threadingManager;

        private final GridLayout root = new GridLayout(2, 6);
        private final Label os;
        private final Label memoryUsage;
        private final Label threadUsage;
        private final Label uptime;
        private final Label javaVersion;
        private final Label botVersion;

        private SystemPanel(final ThreadingManager threadingManager) {
            this.threadingManager = threadingManager;

            setCaption("System Status");
            setWidth("650px");
            addComponent(root);
            root.setSpacing(true);

            os = new Label(System.getProperty("os.name") + ' ' + System.getProperty("os.version"));
            memoryUsage = new Label();
            threadUsage = new Label();
            uptime = new Label();
            javaVersion = new Label(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ')');
            botVersion = new Label(Main.VERSION);

            addWithDescription(os, "Running On", 0);
            addWithDescription(memoryUsage, "Memory Usage", 1);
            addWithDescription(threadUsage, "Thread Usage", 2);
            addWithDescription(uptime, "Uptime", 3);
            addWithDescription(javaVersion, "Java Version", 4);
            addWithDescription(botVersion, "LucidBot Version", 5);
            refresh();
        }

        private void addWithDescription(final Label label, final String description, final int row) {
            root.addComponent(new Label(description), 0, row);
            root.addComponent(label, 1, row);
        }

        @Override
        public final void refresh() {
            Runtime runtime = Runtime.getRuntime();
            long reservedMemoryMB = runtime.totalMemory() / MILLION;
            long freeMemoryMB = runtime.freeMemory() / MILLION;
            long maxMemoryMB = runtime.maxMemory() / MILLION;
            long usedMemoryMB = reservedMemoryMB - freeMemoryMB;
            memoryUsage.setValue("Using: " + usedMemoryMB + "MB, Reserved: " + reservedMemoryMB + "MB, Max Limit: " + maxMemoryMB + "MB");

            threadUsage.setValue("Using: " + threadingManager.threadsUsed() + " out of total: " + threadingManager.totalThreads());

            uptime.setValue(TimeUtil.formatTimeDifference(System.currentTimeMillis() - Main.STARTUP_TIME));
        }
    }
}
