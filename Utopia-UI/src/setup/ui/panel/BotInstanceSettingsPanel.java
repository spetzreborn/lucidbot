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

import api.database.SimpleTransactionTask;
import api.database.daos.BotInstanceSettingsDAO;
import api.database.daos.ChannelDAO;
import api.database.models.BotInstanceSettings;
import api.database.models.Channel;
import api.events.DelayedEventPoster;
import api.irc.ValidationType;
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.ui.*;
import internal.main.Main;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.*;

import static api.database.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class BotInstanceSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "400px";
    private static final String POPUP_WIDTH = "400px";

    private final Provider<BotInstanceSettingsDAO> botInstanceSettingsDAO;
    private final Main main;

    private final Label description;
    private final Button addButton;
    private final Table botTable;

    private final Map<Object, BotInstanceSettings> botMap = new HashMap<>();
    private final Map<String, Channel> channelMap = new HashMap<>();

    @Inject
    public BotInstanceSettingsPanel(final Provider<BotInstanceSettingsDAO> botInstanceSettingsDAO,
                                    final Provider<ChannelDAO> channelDAOProvider, final Main main) {
        this.botInstanceSettingsDAO = botInstanceSettingsDAO;
        this.main = main;

        setSpacing(true);
        setMargin(true);

        loadChannels(channelDAOProvider);

        description = new Label(
                "Add and save the bots you want to connect to IRC (usually just one, but may be more. Note that some servers limit the amount of connections per IP to the server)");

        addButton = new Button("Add Bot", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        botTable = new Table();
        botTable.setContainerDataSource(getBotContainer());
        botTable.setColumnHeaders(new String[]{"Name", "Password", "Channels", "Edit", "Delete"});
        botTable.setHeight("100%");
        botTable.setWidth("650px");
        botTable.setColumnExpandRatio("channels", 0.6f);

        addComponent(description);
        addComponent(addButton);
        addComponent(botTable);
    }

    private void loadChannels(final Provider<ChannelDAO> channelDAOProvider) {
        channelMap.clear();
        for (Channel channel : channelDAOProvider.get().getAllChannels()) {
            channelMap.put(channel.getName(), channel);
        }
    }

    private IndexedContainer getBotContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("password", String.class, null);
        container.addContainerProperty("channels", VerticalLayout.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (BotInstanceSettings botSettings : botInstanceSettingsDAO.get().getAll()) {
                    addBot(botSettings, container);
                }
            }
        });

        return container;
    }

    private void addBot(final BotInstanceSettings botSettings, final Container container) {
        final Object itemId = container.addItem();
        botMap.put(itemId, botSettings);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(botSettings.getNick());
        item.getItemProperty("password").setValue(botSettings.getPassword());
        item.getItemProperty("channels").setValue(createChannelList(botSettings));
        item.getItemProperty("edit").setValue(new Button("Edit", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(itemId), POPUP_HEIGHT, POPUP_WIDTH));
            }
        }));
        item.getItemProperty("delete").setValue(new Button("Delete", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                try {
                    BotInstanceSettings remove = botMap.get(itemId);
                    if (remove.getId() != null) {
                        botInstanceSettingsDAO.get().delete(remove);
                        botMap.remove(itemId);
                        container.removeItem(itemId);
                        main.removeBotInstance(remove.getNick());
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static VerticalLayout createChannelList(final BotInstanceSettings botSettings) {
        VerticalLayout channelsList = new VerticalLayout();
        for (Channel channel : botSettings.getChannels()) {
            channelsList.addComponent(new Label(channel.getName() + " (" + channel.getType().getName() + ')'));
        }
        return channelsList;
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final TextField passwordField;
        private final TwinColSelect channelsSelect;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            BotInstanceSettings settings = null;
            if (itemId != null) {
                settings = botMap.get(itemId);
            }

            nameField = new TextField("Bot name/nick");
            nameField.setRequired(true);
            nameField.addValidator(new RegexpValidator(ValidationType.NICKNAME.getPattern(), "Invalid name format"));
            if (settings != null) nameField.setValue(settings.getNick());

            passwordField = new TextField("Bot password");
            passwordField.setRequired(true);
            passwordField.setNullSettingAllowed(false);
            if (settings != null) passwordField.setValue(settings.getPassword());

            channelsSelect = new TwinColSelect("Channels", channelMap.keySet());
            channelsSelect.setRows(channelMap.size());
            channelsSelect.setWidth("100%");
            channelsSelect.setLeftColumnCaption("All available channels");
            channelsSelect.setRightColumnCaption("Channels this bot will join");
            if (settings != null) channelsSelect.setValue(settings.getChannelNames(false));

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        String password = validate(passwordField, String.class);
                        Collection<?> channelIDs = channelsSelect.getItemIds();
                        Set<Channel> selectedChannels = new HashSet<>();
                        for (Object channelID : channelIDs) {
                            if (channelsSelect.isSelected(channelID)) {
                                selectedChannels.add(channelMap.get(channelID));
                            }
                        }

                        BotInstanceSettings botInstanceSettings;
                        if (itemId == null) {
                            botInstanceSettings = new BotInstanceSettings(name, password, selectedChannels);
                            botInstanceSettingsDAO.get().save(botInstanceSettings);
                            addBot(botInstanceSettings, botTable.getContainerDataSource());
                        } else {
                            botInstanceSettings = botMap.get(itemId);
                            String oldNick = botInstanceSettings.getNick();
                            botInstanceSettings = botMap.get(itemId);
                            botInstanceSettings.setNick(name);
                            botInstanceSettings.setPassword(password);
                            botInstanceSettings.setChannels(selectedChannels);

                            Container container = botTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("password").setValue(password);
                            item.getItemProperty("channels").setValue(createChannelList(botInstanceSettings));

                            main.syncBotInstance(oldNick, botInstanceSettings);
                            botInstanceSettingsDAO.get().save(botInstanceSettings);
                        }
                        removePopup(event);
                    } catch (Validator.InvalidValueException e) {
                        getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                    }
                }
            });

            cancelButton = new Button("Cancel", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    removePopup(event);
                }
            });

            addComponent(nameField);
            addComponent(passwordField);
            addComponent(channelsSelect);
            HorizontalLayout buttons = new HorizontalLayout();
            buttons.addComponent(saveButton);
            buttons.addComponent(cancelButton);
            addComponent(buttons);
        }

        private void removePopup(final Button.ClickEvent event) {
            Window parent = getWindow();
            parent.getParent().removeWindow(parent);
        }
    }
}
