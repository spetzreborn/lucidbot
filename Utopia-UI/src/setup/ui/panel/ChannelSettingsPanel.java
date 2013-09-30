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

import api.database.daos.ChannelDAO;
import api.database.models.Channel;
import api.database.models.ChannelType;
import api.database.transactions.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import api.irc.ValidationType;
import api.tools.collections.ListUtil;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.ui.*;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static api.database.transactions.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class ChannelSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "300px";
    private static final String POPUP_WIDTH = "225px";

    private final Provider<ChannelDAO> channelDAOProvider;

    private final Label description;
    private final Button addButton;
    private final Table channelTable;

    private final Map<Object, Channel> channelMap = new HashMap<>();

    @Inject
    public ChannelSettingsPanel(final Provider<ChannelDAO> channelDAOProvider) {
        this.channelDAOProvider = channelDAOProvider;

        setSpacing(true);
        setMargin(true);

        description = new Label("Add and save the channels you want the bot to be in. If a channel has a password or is invite only, " +
                "the bot needs @ or above or it will fail to join (it invites itself even into password protected channels).");

        addButton = new Button("Add Channel", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent clickEvent) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        channelTable = new Table();
        channelTable.setContainerDataSource(getChannelContainer());
        channelTable.setColumnHeaders(new String[]{"Name", "Channel Type", "Edit", "Delete"});
        channelTable.setHeight("100%");
        channelTable.setWidth("650px");
        channelTable.setColumnExpandRatio("name", 0.5f);

        addComponent(description);
        addComponent(addButton);
        addComponent(channelTable);
    }

    private IndexedContainer getChannelContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("type", String.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (Channel channel : channelDAOProvider.get().getAllChannels()) {
                    addChannel(channel, container);
                }
            }
        });

        return container;
    }

    private void addChannel(final Channel channel, final Container container) {
        final Object itemId = container.addItem();
        channelMap.put(itemId, channel);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(channel.getName());
        item.getItemProperty("type").setValue(channel.getType().getName());
        item.getItemProperty("edit").setValue(new Button("Edit", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent clickEvent) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(itemId), POPUP_HEIGHT, POPUP_WIDTH));
            }
        }));
        item.getItemProperty("delete").setValue(new Button("Delete", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                try {
                    Channel remove = channelMap.get(itemId);
                    if (remove.getId() != null) {
                        channelDAOProvider.get().delete(remove);
                        channelMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final Select typeSelect;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            String name = null;
            String channelTypeName = null;
            if (itemId != null) {
                Container container = channelTable.getContainerDataSource();
                Item item = container.getItem(itemId);
                name = (String) item.getItemProperty("name").getValue();
                channelTypeName = (String) item.getItemProperty("type").getValue();
            }

            nameField = new TextField("Channel name");
            nameField.setRequired(true);
            nameField.setWidth("100%");
            nameField.addValidator(new RegexpValidator(ValidationType.CHANNEL.getPattern(), "Invalid name format"));
            if (name != null) {
                nameField.setValue(name);
                nameField.setEnabled(false);
            }

            typeSelect = new Select("Channel Type", ListUtil.getNames(Lists.newArrayList(ChannelType.values())));
            typeSelect.setRequired(true);
            typeSelect.setNullSelectionAllowed(false);
            if (channelTypeName != null) typeSelect.setValue(channelTypeName);

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class).trim();
                        String type = validate(typeSelect, String.class);

                        Channel channel;
                        if (itemId == null) {
                            channel = new Channel(name, ChannelType.fromName(type));
                            channelDAOProvider.get().save(channel);
                            addChannel(channel, channelTable.getContainerDataSource());
                        } else {
                            channel = channelMap.get(itemId);
                            channel.setName(name);
                            channel.setType(ChannelType.fromName(type));

                            Container container = channelTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("type").setValue(type);
                            channelDAOProvider.get().save(channel);
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
            addComponent(typeSelect);
            HorizontalLayout buttons = new HorizontalLayout();
            buttons.addComponent(saveButton);
            buttons.addComponent(cancelButton);
            addComponent(buttons);
        }

        private void removePopup(final Button.ClickEvent event) {
            Window parent = event.getButton().getWindow();
            parent.getParent().removeWindow(parent);
        }
    }
}
