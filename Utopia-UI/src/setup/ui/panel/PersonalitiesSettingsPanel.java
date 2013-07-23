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
import database.daos.PersonalityDAO;
import database.models.Bonus;
import database.models.IntelAccuracySpecification;
import database.models.Personality;
import setup.tools.Personalities;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static api.database.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class PersonalitiesSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "620px";
    private static final String POPUP_WIDTH = "450px";

    private final Provider<PersonalityDAO> personalityDAOProvider;

    private final Label description;
    private final Button addButton;
    private final Table personalityTable;
    private final Button loadDefaultsButton;

    private final Map<Object, Personality> personalityMap = new HashMap<>();
    private final Map<String, Bonus> bonusMap = new HashMap<>();

    @Inject
    public PersonalitiesSettingsPanel(final Provider<PersonalityDAO> personalityDAOProvider, final Provider<BonusDAO> bonusDAOProvider,
                                      final Personalities personalitiesSetup) {
        this.personalityDAOProvider = personalityDAOProvider;

        setSpacing(true);
        setMargin(true);

        loadBonuses(bonusDAOProvider);

        description = new Label("Add and save personalities");

        addButton = new Button("Add Personality", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        loadDefaultsButton = new Button("Load defaults", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                personalitiesSetup.loadIntoDatabase();
                loadBonuses(bonusDAOProvider);
                personalityTable.setContainerDataSource(getPersonalityContainer());
            }
        });

        personalityTable = new Table();
        personalityTable.setContainerDataSource(getPersonalityContainer());
        personalityTable.setHeight("100%");
        personalityTable.setWidth("650px");
        personalityTable.setColumnHeaders(new String[]{"Name", "Alias", "Bonuses", "Edit", "Delete"});
        personalityTable.setColumnExpandRatio("name", 0.2f);
        personalityTable.setColumnExpandRatio("alias", 0.3f);
        personalityTable.setColumnExpandRatio("bonuses", 0.5f);

        addComponent(description);
        addComponent(loadDefaultsButton);
        addComponent(addButton);
        addComponent(personalityTable);
    }

    private void loadBonuses(final Provider<BonusDAO> bonusDAOProvider) {
        bonusMap.clear();
        for (Bonus bonus : bonusDAOProvider.get().getAllBonuses()) {
            bonusMap.put(bonus.getName(), bonus);
        }
    }

    private IndexedContainer getPersonalityContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("alias", String.class, null);
        container.addContainerProperty("bonuses", VerticalLayout.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (Personality personality : personalityDAOProvider.get().getAllPersonalities()) {
                    addPersonality(personality, container);
                }
            }
        });

        return container;
    }

    private void addPersonality(final Personality personality, final Container container) {
        final Object itemId = container.addItem();
        personalityMap.put(itemId, personality);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(personality.getName());
        item.getItemProperty("alias").setValue(personality.getAlias());
        item.getItemProperty("bonuses").setValue(createBonusList(personality));
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
                    Personality remove = personalityMap.get(itemId);
                    if (remove.getId() != null) {
                        personalityDAOProvider.get().delete(remove);
                        personalityMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static VerticalLayout createBonusList(final Personality personality) {
        VerticalLayout bonusList = new VerticalLayout();
        for (Bonus bonus : personality.getBonuses()) {
            bonusList.addComponent(new Label(bonus.getName()));
        }
        return bonusList;
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final TextField aliasField;
        private final Select intelAccuracySelect;
        private final CheckBox isDragonImmuneCheckBox;
        private final TextArea prosArea;
        private final TextArea consArea;
        private final TwinColSelect bonusesSelect;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            Personality personality = null;
            if (itemId != null) {
                personality = personalityMap.get(itemId);
            }

            nameField = new TextField("Name");
            nameField.setRequired(true);
            if (personality != null) nameField.setValue(personality.getName());

            aliasField = new TextField("Alias");
            aliasField.setRequired(true);
            if (personality != null) aliasField.setValue(personality.getAlias());

            intelAccuracySelect = new Select("Intel Accuracy", ListUtil.getNames(Lists.newArrayList(IntelAccuracySpecification.values())));
            intelAccuracySelect.setRequired(true);
            intelAccuracySelect.setNullSelectionAllowed(false);
            if (personality != null)
                intelAccuracySelect.setValue(personality.getIntelAccuracySpecification().getName());

            isDragonImmuneCheckBox = new CheckBox("Dragon immune");
            if (personality != null) isDragonImmuneCheckBox.setValue(personality.isDragonImmune());

            prosArea = new TextArea("Pros");
            prosArea.setRows(10);
            prosArea.setColumns(20);
            prosArea.setNullRepresentation("");
            if (personality != null) prosArea.setValue(personality.getPros());

            consArea = new TextArea("Cons");
            consArea.setRows(10);
            consArea.setColumns(20);
            consArea.setNullRepresentation("");
            if (personality != null) consArea.setValue(personality.getCons());

            bonusesSelect = new TwinColSelect("Bonuses", bonusMap.keySet());
            bonusesSelect.setRows(10);
            bonusesSelect.setNullSelectionAllowed(true);
            bonusesSelect.setMultiSelect(true);
            bonusesSelect.setWidth("100%");
            if (personality != null) bonusesSelect.setValue(ListUtil.getNames(personality.getBonuses()));

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        String alias = validate(aliasField, String.class);
                        IntelAccuracySpecification intelSpec = IntelAccuracySpecification
                                .fromName(intelAccuracySelect.getValue().toString());
                        boolean isDragonImmune = isDragonImmuneCheckBox.booleanValue();
                        String pros = validate(prosArea, String.class);
                        String cons = validate(consArea, String.class);
                        Set<?> bonusIds = (Set<?>) bonusesSelect.getValue();
                        Set<Bonus> bonuses = new HashSet<>(bonusIds.size());
                        for (Object bonusId : bonusIds) {
                            bonuses.add(bonusMap.get(bonusId));
                        }

                        Personality personality;
                        if (itemId == null) {
                            personality = new Personality(name, alias, intelSpec, isDragonImmune, pros, cons, bonuses);
                            personalityDAOProvider.get().save(personality);
                            addPersonality(personality, personalityTable.getContainerDataSource());
                        } else {
                            personality = personalityMap.get(itemId);
                            personality.setName(name);
                            personality.setAlias(alias);
                            personality.setIntelAccuracySpecification(intelSpec);
                            personality.setDragonImmune(isDragonImmune);
                            personality.setPros(pros);
                            personality.setCons(cons);
                            personality.setBonuses(bonuses);

                            Container container = personalityTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("alias").setValue(alias);
                            item.getItemProperty("bonuses").setValue(createBonusList(personality));
                            personalityDAOProvider.get().save(personality);
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
            addComponent(aliasField);
            addComponent(intelAccuracySelect);
            addComponent(isDragonImmuneCheckBox);
            addComponent(prosArea);
            addComponent(consArea);
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
