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
import api.events.DelayedEventPoster;
import api.tools.collections.ListUtil;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.*;
import database.daos.BonusDAO;
import database.daos.OpDAO;
import database.models.Bonus;
import database.models.OpType;
import database.models.SpellOpCharacter;
import setup.tools.OpTypes;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static api.database.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class OpTypesSettingsPanel extends VerticalLayout {
    private final Provider<OpDAO> opDAOProvider;

    private final Label description;
    private final Button addButton;
    private final Table opTypeTable;
    private final Button loadDefaultsButton;

    private final Map<Object, OpType> opTypeMap = new HashMap<>();
    private final Map<String, Bonus> bonusMap = new HashMap<>();

    @Inject
    public OpTypesSettingsPanel(final Provider<OpDAO> opDAOProvider, final OpTypes opTypesSetup,
                                final Provider<BonusDAO> bonusDAOProvider) {
        this.opDAOProvider = opDAOProvider;

        setSpacing(true);
        setMargin(true);

        loadBonuses(bonusDAOProvider);

        description = new Label("Add and save op types");

        addButton = new Button("Add Op Type", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), "600px", "600px"));
            }
        });

        loadDefaultsButton = new Button("Load defaults", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                opTypesSetup.loadIntoDatabase();
                loadBonuses(bonusDAOProvider);
                opTypeTable.setContainerDataSource(getOpTypeContainer());
            }
        });

        opTypeTable = new Table();
        opTypeTable.setContainerDataSource(getOpTypeContainer());
        opTypeTable.setHeight("100%");
        opTypeTable.setWidth("750px");
        opTypeTable.setColumnHeaders(new String[]{"Name", "Short Name", "Character", "Bonuses", "Edit", "Delete"});
        opTypeTable.setColumnExpandRatio("name", 0.3f);
        opTypeTable.setColumnExpandRatio("character", 0.4f);
        opTypeTable.setColumnExpandRatio("bonuses", 0.3f);

        addComponent(description);
        addComponent(loadDefaultsButton);
        addComponent(addButton);
        addComponent(opTypeTable);
    }

    private void loadBonuses(final Provider<BonusDAO> bonusDAOProvider) {
        bonusMap.clear();
        for (Bonus bonus : bonusDAOProvider.get().getAllBonuses()) {
            bonusMap.put(bonus.getName(), bonus);
        }
    }

    private IndexedContainer getOpTypeContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("shortName", String.class, null);
        container.addContainerProperty("character", String.class, null);
        container.addContainerProperty("bonuses", VerticalLayout.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (OpType opType : opDAOProvider.get().getAllOpTypes()) {
                    addOpType(opType, container);
                }
            }
        });

        return container;
    }

    private void addOpType(final OpType opType, final Container container) {
        final Object itemId = container.addItem();
        opTypeMap.put(itemId, opType);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(opType.getName());
        item.getItemProperty("shortName").setValue(opType.getShortName());
        item.getItemProperty("character").setValue(opType.getOpCharacter().getName());
        item.getItemProperty("bonuses").setValue(createBonusList(opType));
        item.getItemProperty("edit").setValue(new Button("Edit", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(itemId), "600px", "600px"));
            }
        }));
        item.getItemProperty("delete").setValue(new Button("Delete", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                try {
                    OpType remove = opTypeMap.get(itemId);
                    if (remove.getId() != null) {
                        opDAOProvider.get().delete(remove);
                        opTypeMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static VerticalLayout createBonusList(final OpType opType) {
        VerticalLayout bonusList = new VerticalLayout();
        for (Bonus bonus : opType.getBonuses()) {
            bonusList.addComponent(new Label(bonus.getName()));
        }
        return bonusList;
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final TextField shortNameField;
        private final TextArea effectsArea;
        private final TextField opRegexField;
        private final TextField newsRegexField;
        private final Select characterSelect;
        private final TwinColSelect bonusesSelect;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            OpType opType = null;
            if (itemId != null) {
                opType = opTypeMap.get(itemId);
            }

            nameField = new TextField("Name");
            nameField.setRequired(true);
            if (opType != null) nameField.setValue(opType.getName());

            shortNameField = new TextField("Short Name");
            shortNameField.setRequired(false);
            shortNameField.setNullRepresentation("");
            shortNameField.setNullSettingAllowed(true);
            if (opType != null) shortNameField.setValue(opType.getShortName());

            effectsArea = new TextArea("Effects");
            effectsArea.setRows(10);
            effectsArea.setColumns(20);
            effectsArea.setWordwrap(true);
            effectsArea.setNullRepresentation("");
            effectsArea.setNullSettingAllowed(true);
            if (opType != null) effectsArea.setValue(opType.getEffects());

            opRegexField = new TextField("Op Regex");
            opRegexField.setNullRepresentation("");
            opRegexField.setNullSettingAllowed(true);
            opRegexField.setWidth("100%");
            if (opType != null) opRegexField.setValue(opType.getOpRegex());

            newsRegexField = new TextField("News Regex");
            newsRegexField.setNullRepresentation("");
            newsRegexField.setNullSettingAllowed(true);
            newsRegexField.setWidth("100%");
            if (opType != null) newsRegexField.setValue(opType.getNewsRegex());

            characterSelect = new Select("Op Character", ListUtil.getNames(Lists.newArrayList(SpellOpCharacter.values())));
            characterSelect.setNullSelectionAllowed(false);
            characterSelect.setRequired(true);
            if (opType != null) characterSelect.setValue(opType.getOpCharacter().getName());

            bonusesSelect = new TwinColSelect("Bonuses", bonusMap.keySet());
            bonusesSelect.setRows(10);
            bonusesSelect.setNullSelectionAllowed(true);
            bonusesSelect.setMultiSelect(true);
            bonusesSelect.setWidth("100%");
            if (opType != null) bonusesSelect.setValue(ListUtil.getNames(opType.getBonuses()));

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        String shortName = validate(shortNameField, String.class);
                        String effects = validate(effectsArea, String.class);
                        String opRegex = validate(opRegexField, String.class);
                        String newsRegex = validate(newsRegexField, String.class);
                        SpellOpCharacter character = SpellOpCharacter.fromName(validate(characterSelect, String.class));
                        Set<?> bonusIds = (Set<?>) bonusesSelect.getValue();
                        Set<Bonus> bonuses = new HashSet<>(bonusIds.size());
                        for (Object bonusId : bonusIds) {
                            bonuses.add(bonusMap.get(bonusId));
                        }

                        OpType opType;
                        if (itemId == null) {
                            opType = new OpType(name, shortName, effects, opRegex, newsRegex, character, bonuses);
                            opDAOProvider.get().save(opType);
                            addOpType(opType, opTypeTable.getContainerDataSource());
                        } else {
                            opType = opTypeMap.get(itemId);
                            opType.setName(name);
                            opType.setShortName(shortName);
                            opType.setEffects(effects);
                            opType.setOpRegex(opRegex);
                            opType.setNewsRegex(newsRegex);
                            opType.setOpCharacter(character);
                            opType.setBonuses(bonuses);

                            Container container = opTypeTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("shortName").setValue(shortName);
                            item.getItemProperty("character").setValue(character.getName());
                            item.getItemProperty("bonuses").setValue(createBonusList(opType));
                            opDAOProvider.get().save(opType);
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
            addComponent(shortNameField);
            addComponent(effectsArea);
            addComponent(opRegexField);
            addComponent(newsRegexField);
            addComponent(characterSelect);
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
