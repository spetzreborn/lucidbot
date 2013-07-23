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
import api.database.daos.AliasDAO;
import api.database.models.Alias;
import api.events.DelayedEventPoster;
import api.events.bot.AliasRemovedEvent;
import api.events.bot.AliasUpdateEvent;
import com.google.common.eventbus.EventBus;
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.*;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static api.database.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class AliasesSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "800px";
    private static final String POPUP_WIDTH = "400px";

    private final Provider<AliasDAO> aliasDAOProvider;
    private final EventBus eventBus;

    private final Label description;
    private final Button addButton;

    private final Table aliasesTable;

    private final Map<Object, Alias> aliasMap = new HashMap<>();

    @Inject
    public AliasesSettingsPanel(final Provider<AliasDAO> aliasDAOProvider, final EventBus eventBus) {
        this.aliasDAOProvider = aliasDAOProvider;
        this.eventBus = eventBus;

        setSpacing(true);
        setMargin(true);

        description = new Label("Add and save aliases");

        addButton = new Button("Add Alias", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        aliasesTable = new Table();
        aliasesTable.setContainerDataSource(getBonusContainer());
        aliasesTable.setColumnHeaders(new String[]{"Alias", "Transform", "Description", "Edit", "Delete"});
        aliasesTable.setHeight("100%");
        aliasesTable.setWidth("700px");

        addComponent(description);
        addComponent(addButton);
        addComponent(aliasesTable);
    }

    private IndexedContainer getBonusContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("alias", String.class, null);
        container.addContainerProperty("transform", String.class, null);
        container.addContainerProperty("description", String.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (Alias alias : aliasDAOProvider.get().getAllAliases()) {
                    addAlias(alias, container);
                }
            }
        });

        return container;
    }

    private void addAlias(final Alias alias, final Container container) {
        final Object itemId = container.addItem();
        aliasMap.put(itemId, alias);
        final Item item = container.getItem(itemId);
        item.getItemProperty("alias").setValue(alias.getAlias());
        item.getItemProperty("transform").setValue(alias.getTransform());
        item.getItemProperty("description").setValue(alias.getDescription());
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
                    Alias remove = aliasMap.get(itemId);
                    if (remove.getId() != null) {
                        aliasDAOProvider.get().delete(remove);
                        aliasMap.remove(itemId);
                        container.removeItem(itemId);
                        eventBus.post(new AliasRemovedEvent(remove.getId()));
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextArea aliasField;
        private final TextArea transformField;
        private final TextArea descriptionField;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            Alias alias = null;
            if (itemId != null) {
                alias = aliasMap.get(itemId);
            }

            aliasField = new TextArea("Alias");
            aliasField.setRequired(true);
            aliasField.setWidth("100%");
            aliasField.setNullRepresentation("");
            aliasField.setNullSettingAllowed(false);
            aliasField.setRows(10);
            aliasField.setWordwrap(true);
            aliasField.addValidator(new StringLengthValidator("Alias is too long", 0, 255, false));
            if (alias != null) aliasField.setValue(alias.getAlias());

            transformField = new TextArea("Transform");
            transformField.setRequired(true);
            transformField.setWidth("100%");
            transformField.setNullRepresentation("");
            transformField.setNullSettingAllowed(false);
            transformField.setRows(10);
            transformField.setWordwrap(true);
            transformField.addValidator(new StringLengthValidator("Transform is too long", 0, 255, false));
            if (alias != null) transformField.setValue(alias.getTransform());

            descriptionField = new TextArea("Description");
            descriptionField.setWidth("100%");
            descriptionField.setNullRepresentation("");
            descriptionField.setNullSettingAllowed(true);
            descriptionField.setRows(10);
            descriptionField.setWordwrap(true);
            descriptionField.addValidator(new StringLengthValidator("Description is too long", 0, 5000, false));
            if (alias != null) descriptionField.setValue(alias.getDescription());

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String alias = validate(aliasField, String.class);
                        String transform = validate(transformField, String.class);
                        String description = validate(descriptionField, String.class);

                        Alias aliasObj;
                        if (itemId == null) {
                            aliasObj = new Alias(alias, transform, description);
                            aliasDAOProvider.get().save(aliasObj);
                            addAlias(aliasObj, aliasesTable.getContainerDataSource());
                        } else {
                            aliasObj = aliasMap.get(itemId);
                            aliasObj.setAlias(alias);
                            aliasObj.setTransform(transform);
                            aliasObj.setDescription(description);

                            Container container = aliasesTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("alias").setValue(alias);
                            item.getItemProperty("transform").setValue(transform);
                            item.getItemProperty("description").setValue(description);
                            aliasDAOProvider.get().save(aliasObj);
                        }
                        removePopup(event);
                        eventBus.post(new AliasUpdateEvent(aliasObj.getId()));
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

            addComponent(aliasField);
            addComponent(transformField);
            addComponent(descriptionField);
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
