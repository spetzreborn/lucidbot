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

import api.database.transactions.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import api.tools.collections.ListUtil;
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.*;
import database.daos.BonusDAO;
import database.daos.DragonDAO;
import database.models.Bonus;
import database.models.Dragon;
import setup.tools.Dragons;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static api.database.transactions.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class DragonsSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "450px";
    private static final String POPUP_WIDTH = "400px";

    private final Provider<DragonDAO> dragonDAOProvider;

    private final Label description;
    private final Button addButton;
    private final Table dragonTable;
    private final Button loadDefaultsButton;

    private final Map<Object, Dragon> dragonMap = new HashMap<>();
    private final Map<String, Bonus> bonusMap = new HashMap<>();

    @Inject
    public DragonsSettingsPanel(final Provider<DragonDAO> dragonDAOProvider, final Provider<BonusDAO> bonusDAOProvider,
                                final Dragons dragonsSetup) {
        this.dragonDAOProvider = dragonDAOProvider;

        setSpacing(true);
        setMargin(true);

        loadBonuses(bonusDAOProvider);

        description = new Label("Add and save dragons");

        addButton = new Button("Add Dragon", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        loadDefaultsButton = new Button("Load defaults", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                dragonsSetup.loadIntoDatabase();
                loadBonuses(bonusDAOProvider);
                dragonTable.setContainerDataSource(getDragonContainer());
            }
        });

        dragonTable = new Table();
        dragonTable.setContainerDataSource(getDragonContainer());
        dragonTable.setColumnHeaders(new String[]{"Name", "Bonuses", "Edit", "Delete"});
        dragonTable.setHeight("100%");
        dragonTable.setWidth("650px");
        dragonTable.setColumnExpandRatio("name", 0.2f);
        dragonTable.setColumnExpandRatio("bonuses", 0.6f);

        addComponent(description);
        addComponent(loadDefaultsButton);
        addComponent(addButton);
        addComponent(dragonTable);
    }

    private void loadBonuses(final Provider<BonusDAO> bonusDAOProvider) {
        bonusMap.clear();
        for (Bonus bonus : bonusDAOProvider.get().getAllBonuses()) {
            bonusMap.put(bonus.getName(), bonus);
        }
    }

    private IndexedContainer getDragonContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("bonuses", VerticalLayout.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (Dragon dragon : dragonDAOProvider.get().getAllDragons()) {
                    addDragon(dragon, container);
                }
            }
        });

        return container;
    }

    private void addDragon(final Dragon dragon, final Container container) {
        final Object itemId = container.addItem();
        dragonMap.put(itemId, dragon);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(dragon.getName());
        item.getItemProperty("bonuses").setValue(createBonusList(dragon));
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
                    Dragon remove = dragonMap.get(itemId);
                    if (remove.getId() != null) {
                        dragonDAOProvider.get().delete(remove);
                        dragonMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static VerticalLayout createBonusList(final Dragon dragon) {
        VerticalLayout bonusList = new VerticalLayout();
        for (Bonus bonus : dragon.getBonuses()) {
            bonusList.addComponent(
                    new Label(bonus.getName() + " (Value: " + bonus.getBonusValue() + ", Type: " + bonus.getType().getName() + ')'));
        }
        return bonusList;
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final TwinColSelect bonusesSelect;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            Dragon dragon = null;
            if (itemId != null) {
                dragon = dragonMap.get(itemId);
            }

            nameField = new TextField("Name");
            nameField.setRequired(true);
            if (dragon != null) nameField.setValue(dragon.getName());

            bonusesSelect = new TwinColSelect("Bonuses", bonusMap.keySet());
            bonusesSelect.setRows(10);
            bonusesSelect.setNullSelectionAllowed(true);
            bonusesSelect.setMultiSelect(true);
            bonusesSelect.setWidth("100%");
            if (dragon != null) bonusesSelect.setValue(ListUtil.getNames(dragon.getBonuses()));

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        Set<?> bonusIds = (Set<?>) bonusesSelect.getValue();
                        Set<Bonus> bonuses = new HashSet<>(bonusIds.size());
                        for (Object bonusId : bonusIds) {
                            bonuses.add(bonusMap.get(bonusId));
                        }

                        Dragon dragon;
                        if (itemId == null) {
                            dragon = new Dragon(name, bonuses);
                            dragonDAOProvider.get().save(dragon);
                            addDragon(dragon, dragonTable.getContainerDataSource());
                        } else {
                            dragon = dragonMap.get(itemId);
                            dragon.setName(name);
                            dragon.setBonuses(bonuses);
                            Container container = dragonTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("bonuses").setValue(createBonusList(dragon));
                            dragonDAOProvider.get().save(dragon);
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
            addComponent(bonusesSelect);
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
