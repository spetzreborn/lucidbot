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
import database.daos.SpellDAO;
import database.models.Bonus;
import database.models.SpellOpCharacter;
import database.models.SpellType;
import setup.tools.SpellTypes;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static api.database.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class SpellTypesSettingsPanel extends VerticalLayout {
    private final Provider<SpellDAO> spellDAOProvider;

    private final Label description;
    private final Button addButton;
    private final Table spellTypeTable;
    private final Button loadDefaultsButton;

    private final Map<Object, SpellType> spellTypeMap = new HashMap<>();
    private final Map<String, Bonus> bonusMap = new HashMap<>();

    @Inject
    public SpellTypesSettingsPanel(final Provider<SpellDAO> spellDAOProvider, final SpellTypes spellTypesSetup,
                                   final Provider<BonusDAO> bonusDAOProvider) {
        this.spellDAOProvider = spellDAOProvider;

        setSpacing(true);
        setMargin(true);

        loadBonuses(bonusDAOProvider);

        description = new Label("Add and save spell types");

        addButton = new Button("Add Spell Type", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), "600px", "600px"));
            }
        });

        loadDefaultsButton = new Button("Load defaults", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                spellTypesSetup.loadIntoDatabase();
                loadBonuses(bonusDAOProvider);
                spellTypeTable.setContainerDataSource(getSpellTypeContainer());
            }
        });

        spellTypeTable = new Table();
        spellTypeTable.setContainerDataSource(getSpellTypeContainer());
        spellTypeTable.setHeight("100%");
        spellTypeTable.setWidth("750px");
        spellTypeTable.setColumnHeaders(new String[]{"Name", "Short Name", "Character", "Bonuses", "Edit", "Delete"});
        spellTypeTable.setColumnExpandRatio("name", 0.3f);
        spellTypeTable.setColumnExpandRatio("character", 0.4f);
        spellTypeTable.setColumnExpandRatio("bonuses", 0.3f);

        addComponent(description);
        addComponent(loadDefaultsButton);
        addComponent(addButton);
        addComponent(spellTypeTable);
    }

    private void loadBonuses(final Provider<BonusDAO> bonusDAOProvider) {
        bonusMap.clear();
        for (Bonus bonus : bonusDAOProvider.get().getAllBonuses()) {
            bonusMap.put(bonus.getName(), bonus);
        }
    }

    private IndexedContainer getSpellTypeContainer() {
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
                for (SpellType spellType : spellDAOProvider.get().getAllSpellTypes()) {
                    addSpellType(spellType, container);
                }
            }
        });

        return container;
    }

    private void addSpellType(final SpellType spellType, final Container container) {
        final Object itemId = container.addItem();
        spellTypeMap.put(itemId, spellType);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(spellType.getName());
        item.getItemProperty("shortName").setValue(spellType.getShortName());
        item.getItemProperty("character").setValue(spellType.getSpellCharacter().getName());
        item.getItemProperty("bonuses").setValue(createBonusList(spellType));
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
                    SpellType remove = spellTypeMap.get(itemId);
                    if (remove.getId() != null) {
                        spellDAOProvider.get().delete(remove);
                        spellTypeMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static VerticalLayout createBonusList(final SpellType spellType) {
        VerticalLayout bonusList = new VerticalLayout();
        for (Bonus bonus : spellType.getBonuses()) {
            bonusList.addComponent(new Label(bonus.getName()));
        }
        return bonusList;
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final TextField shortNameField;
        private final TextArea effectsArea;
        private final TextField castRegexField;
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

            SpellType spellType = null;
            if (itemId != null) {
                spellType = spellTypeMap.get(itemId);
            }

            nameField = new TextField("Name");
            nameField.setRequired(true);
            if (spellType != null) nameField.setValue(spellType.getName());

            shortNameField = new TextField("Short Name");
            shortNameField.setRequired(false);
            shortNameField.setNullRepresentation("");
            shortNameField.setNullSettingAllowed(true);
            if (spellType != null) shortNameField.setValue(spellType.getShortName());

            effectsArea = new TextArea("Effects");
            effectsArea.setRows(10);
            effectsArea.setColumns(20);
            effectsArea.setWordwrap(true);
            effectsArea.setNullRepresentation("");
            effectsArea.setNullSettingAllowed(true);
            if (spellType != null) effectsArea.setValue(spellType.getEffects());

            castRegexField = new TextField("Cast Regex");
            castRegexField.setNullRepresentation("");
            castRegexField.setNullSettingAllowed(true);
            castRegexField.setWidth("100%");
            if (spellType != null) castRegexField.setValue(spellType.getCastRegex());

            newsRegexField = new TextField("News Regex");
            newsRegexField.setNullRepresentation("");
            newsRegexField.setNullSettingAllowed(true);
            newsRegexField.setWidth("100%");
            if (spellType != null) newsRegexField.setValue(spellType.getNewsRegex());

            characterSelect = new Select("Op Character", ListUtil.getNames(Lists.newArrayList(SpellOpCharacter.values())));
            characterSelect.setNullSelectionAllowed(false);
            characterSelect.setRequired(true);
            if (spellType != null) characterSelect.setValue(spellType.getSpellCharacter().getName());

            bonusesSelect = new TwinColSelect("Bonuses", bonusMap.keySet());
            bonusesSelect.setRows(10);
            bonusesSelect.setNullSelectionAllowed(true);
            bonusesSelect.setMultiSelect(true);
            bonusesSelect.setWidth("100%");
            if (spellType != null) bonusesSelect.setValue(ListUtil.getNames(spellType.getBonuses()));

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        String shortName = validate(shortNameField, String.class);
                        String effects = validate(effectsArea, String.class);
                        String castRegex = validate(castRegexField, String.class);
                        String newsRegex = validate(newsRegexField, String.class);
                        SpellOpCharacter character = SpellOpCharacter.fromName(validate(characterSelect, String.class));
                        Set<?> bonusIds = (Set<?>) bonusesSelect.getValue();
                        Set<Bonus> bonuses = new HashSet<>(bonusIds.size());
                        for (Object bonusId : bonusIds) {
                            bonuses.add(bonusMap.get(bonusId));
                        }

                        SpellType spellType;
                        if (itemId == null) {
                            spellType = new SpellType(name, shortName, effects, castRegex, newsRegex, character, bonuses);
                            spellDAOProvider.get().save(spellType);
                            addSpellType(spellType, spellTypeTable.getContainerDataSource());
                        } else {
                            spellType = spellTypeMap.get(itemId);
                            spellType.setName(name);
                            spellType.setShortName(shortName);
                            spellType.setEffects(effects);
                            spellType.setCastRegex(castRegex);
                            spellType.setNewsRegex(newsRegex);
                            spellType.setSpellCharacter(character);
                            spellType.setBonuses(bonuses);

                            Container container = spellTypeTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("shortName").setValue(shortName);
                            item.getItemProperty("character").setValue(character.getName());
                            item.getItemProperty("bonuses").setValue(createBonusList(spellType));
                            spellDAOProvider.get().save(spellType);
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
            addComponent(castRegexField);
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
