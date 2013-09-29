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
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.data.validator.DoubleValidator;
import com.vaadin.data.validator.IntegerValidator;
import com.vaadin.ui.*;
import database.daos.BonusDAO;
import database.daos.RaceDAO;
import database.daos.SpellDAO;
import database.models.Bonus;
import database.models.IntelAccuracySpecification;
import database.models.Race;
import database.models.SpellType;
import setup.tools.Races;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.*;

import static api.database.transactions.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class RacesSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "620px";
    private static final String POPUP_WIDTH = "450px";

    private final Provider<RaceDAO> raceDAOProvider;

    private final Label description;
    private final Button addButton;
    private final Table raceTable;
    private final Button loadDefaultsButton;

    private final Map<Object, Race> raceMap = new HashMap<>();
    private final Map<String, Bonus> bonusMap = new TreeMap<>();
    private final Map<String, SpellType> spellMap = new TreeMap<>();

    @Inject
    public RacesSettingsPanel(final Provider<RaceDAO> raceDAOProvider, final Provider<BonusDAO> bonusDAOProvider,
                              final Provider<SpellDAO> spellDAOProvider, final Races racesSetup) {
        this.raceDAOProvider = raceDAOProvider;

        setSpacing(true);
        setMargin(true);

        loadBonuses(bonusDAOProvider);

        loadSpells(spellDAOProvider);

        description = new Label("Add and save races");

        addButton = new Button("Add Race", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        loadDefaultsButton = new Button("Load defaults", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                racesSetup.loadIntoDatabase();
                loadBonuses(bonusDAOProvider);
                loadSpells(spellDAOProvider);
                raceTable.setContainerDataSource(getRaceContainer());
            }
        });

        raceTable = new Table();
        raceTable.setContainerDataSource(getRaceContainer());
        raceTable.setHeight("100%");
        raceTable.setWidth("650px");
        raceTable.setColumnHeaders(new String[]{"Name", "Short", "Bonuses", "Spellbook", "Edit", "Delete"});

        addComponent(description);
        addComponent(loadDefaultsButton);
        addComponent(addButton);
        addComponent(raceTable);
    }

    private void loadBonuses(final Provider<BonusDAO> bonusDAOProvider) {
        bonusMap.clear();
        for (Bonus bonus : bonusDAOProvider.get().getAllBonuses()) {
            bonusMap.put(bonus.getName(), bonus);
        }
    }

    private void loadSpells(final Provider<SpellDAO> spellDAOProvider) {
        spellMap.clear();
        for (SpellType spellType : spellDAOProvider.get().getAllSpellTypes()) {
            spellMap.put(spellType.getName(), spellType);
        }
    }

    private IndexedContainer getRaceContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("shortName", String.class, null);
        container.addContainerProperty("bonuses", VerticalLayout.class, null);
        container.addContainerProperty("spellBook", VerticalLayout.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (Race race : raceDAOProvider.get().getAllRaces()) {
                    addRace(race, container);
                }
            }
        });

        return container;
    }

    private void addRace(final Race race, final Container container) {
        final Object itemId = container.addItem();
        raceMap.put(itemId, race);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(race.getName());
        item.getItemProperty("shortName").setValue(race.getShortName());
        item.getItemProperty("bonuses").setValue(createBonusList(race));
        item.getItemProperty("spellBook").setValue(createNewSpellList(race));
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
                    Race remove = raceMap.get(itemId);
                    if (remove.getId() != null) {
                        raceDAOProvider.get().delete(remove);
                        raceMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static VerticalLayout createBonusList(final Race race) {
        VerticalLayout bonusList = new VerticalLayout();
        for (Bonus bonus : race.getBonuses()) {
            bonusList.addComponent(new Label(bonus.getName()));
        }
        return bonusList;
    }

    private static VerticalLayout createNewSpellList(final Race race) {
        VerticalLayout spellList = new VerticalLayout();
        for (SpellType spellType : race.getSpellbook()) {
            spellList.addComponent(new Label(spellType.getName()));
        }
        return spellList;
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final TextField shortNameField;
        private final TextField soldierStrengthField;
        private final TextField soldierNetworthField;
        private final TextField offSpecNameField;
        private final TextField offSpecStrengthField;
        private final TextField defSpecNameField;
        private final TextField defSpecStrengthField;
        private final TextField eliteNameField;
        private final TextField eliteOffStrengthField;
        private final TextField eliteDefStrengthField;
        private final TextField eliteNetworthField;
        private final TextField eliteSendoutPercentageField;
        private final Select intelAccuracySelect;
        private final CheckBox isDragonImmuneCheckBox;
        private final TextArea prosArea;
        private final TextArea consArea;
        private final TwinColSelect bonusesSelect;
        private final TwinColSelect spellsSelect;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            Race race = null;
            if (itemId != null) {
                race = raceMap.get(itemId);
            }

            nameField = new TextField("Name");
            nameField.setRequired(true);
            if (race != null) nameField.setValue(race.getName());

            shortNameField = new TextField("Short Name");
            shortNameField.setRequired(true);
            if (race != null) shortNameField.setValue(race.getShortName());

            soldierStrengthField = new TextField("Soldier Strength");
            soldierStrengthField.setRequired(true);
            soldierStrengthField.addValidator(new IntegerValidator("Soldier Strength should be an integer"));
            if (race != null) soldierStrengthField.setValue(race.getSoldierStrength());

            soldierNetworthField = new TextField("Soldier Networth");
            soldierNetworthField.setRequired(true);
            soldierNetworthField.addValidator(new DoubleValidator("Soldier networth should be a decimal number"));
            if (race != null) soldierNetworthField.setValue(race.getSoldierNetworth());

            offSpecNameField = new TextField("Off Spec Name");
            offSpecNameField.setRequired(true);
            if (race != null) offSpecNameField.setValue(race.getOffSpecName());

            offSpecStrengthField = new TextField("Off Spec Strength");
            offSpecStrengthField.setRequired(true);
            offSpecStrengthField.addValidator(new IntegerValidator("Off Spec Strength should be an integer"));
            if (race != null) offSpecStrengthField.setValue(race.getOffSpecStrength());

            defSpecNameField = new TextField("Def Spec Name");
            defSpecNameField.setRequired(true);
            if (race != null) defSpecNameField.setValue(race.getDefSpecName());

            defSpecStrengthField = new TextField("Def Spec Strength");
            defSpecStrengthField.setRequired(true);
            defSpecStrengthField.addValidator(new IntegerValidator("Def Spec Strength should be an integer"));
            if (race != null) defSpecStrengthField.setValue(race.getDefSpecStrength());

            eliteNameField = new TextField("Elite Name");
            eliteNameField.setRequired(true);
            if (race != null) eliteNameField.setValue(race.getEliteName());

            eliteOffStrengthField = new TextField("Elite Off Strength");
            eliteOffStrengthField.setRequired(true);
            eliteOffStrengthField.addValidator(new IntegerValidator("Elite Off Strength should be an integer"));
            if (race != null) eliteOffStrengthField.setValue(race.getEliteOffStrength());

            eliteDefStrengthField = new TextField("Elite Def Strength");
            eliteDefStrengthField.setRequired(true);
            eliteDefStrengthField.addValidator(new IntegerValidator("Elite Def Strength should be an integer"));
            if (race != null) eliteDefStrengthField.setValue(race.getEliteDefStrength());

            eliteNetworthField = new TextField("Elite Networth");
            eliteNetworthField.setRequired(true);
            eliteNetworthField.addValidator(new DoubleValidator("Elite networth should be a decimal number"));
            if (race != null) eliteNetworthField.setValue(race.getEliteNetworth());

            eliteSendoutPercentageField = new TextField("Elite Sendout Percentage");
            eliteSendoutPercentageField.setRequired(true);
            eliteSendoutPercentageField.addValidator(new IntegerValidator("Elite sendout percentage should be an integer"));
            eliteSendoutPercentageField.addValidator(new AbstractValidator("Elite sendout percentage should be [0, 100]") {
                @Override
                public boolean isValid(final Object value) {
                    int val = Integer.parseInt(value.toString());
                    return val <= 100 && val >= 0;
                }
            });
            if (race != null) eliteSendoutPercentageField.setValue(race.getEliteSendoutPercentage());

            intelAccuracySelect = new Select("Intel Accuracy", ListUtil.getNames(Lists.newArrayList(IntelAccuracySpecification.values())));
            intelAccuracySelect.setRequired(true);
            intelAccuracySelect.setNullSelectionAllowed(false);
            if (race != null) intelAccuracySelect.setValue(race.getIntelAccuracySpecification().getName());

            isDragonImmuneCheckBox = new CheckBox("Dragon immune");
            if (race != null) isDragonImmuneCheckBox.setValue(race.isDragonImmune());

            prosArea = new TextArea("Pros");
            prosArea.setRows(10);
            prosArea.setColumns(20);
            prosArea.setNullRepresentation("");
            if (race != null) prosArea.setValue(race.getPros());

            consArea = new TextArea("Cons");
            consArea.setRows(10);
            consArea.setColumns(20);
            consArea.setNullRepresentation("");
            if (race != null) consArea.setValue(race.getCons());

            bonusesSelect = new TwinColSelect("Bonuses", bonusMap.keySet());
            bonusesSelect.setRows(10);
            bonusesSelect.setNullSelectionAllowed(true);
            bonusesSelect.setMultiSelect(true);
            bonusesSelect.setWidth("100%");
            if (race != null) bonusesSelect.setValue(ListUtil.getNames(race.getBonuses()));

            spellsSelect = new TwinColSelect("Spellbook", spellMap.keySet());
            spellsSelect.setRows(10);
            spellsSelect.setNullSelectionAllowed(true);
            spellsSelect.setMultiSelect(true);
            spellsSelect.setWidth("100%");
            if (race != null) spellsSelect.setValue(ListUtil.getNames(race.getSpellbook()));

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        String shortName = validate(shortNameField, String.class);
                        String offSpecName = validate(offSpecNameField, String.class);
                        int soldierStrength = soldierStrengthField.getValue() instanceof String ? Integer
                                .parseInt(validate(soldierStrengthField, String.class)) : validate(soldierStrengthField, Integer.class);
                        double soldierNetworth = soldierNetworthField.getValue() instanceof String ? Double
                                .parseDouble(validate(soldierNetworthField, String.class)) : validate(soldierNetworthField, Double.class);
                        int offSpecStrength = offSpecStrengthField.getValue() instanceof String ? Integer
                                .parseInt(validate(offSpecStrengthField, String.class)) : validate(offSpecStrengthField, Integer.class);
                        String defSpecName = validate(defSpecNameField, String.class);
                        int defSpecStrength = defSpecStrengthField.getValue() instanceof String ? Integer
                                .parseInt(validate(defSpecStrengthField, String.class)) : validate(defSpecStrengthField, Integer.class);
                        String eliteName = validate(eliteNameField, String.class);
                        int eliteOffStrength = eliteOffStrengthField.getValue() instanceof String ? Integer
                                .parseInt(validate(eliteOffStrengthField, String.class)) : validate(eliteOffStrengthField, Integer.class);
                        int eliteDefStrength = eliteDefStrengthField.getValue() instanceof String ? Integer
                                .parseInt(validate(eliteDefStrengthField, String.class)) : validate(eliteDefStrengthField, Integer.class);
                        double eliteNetworth = eliteNetworthField.getValue() instanceof String ? Double
                                .parseDouble(validate(eliteNetworthField, String.class)) : validate(eliteNetworthField, Double.class);
                        int eliteSendoutPercentage = eliteSendoutPercentageField.getValue() instanceof String ? Integer
                                .parseInt(validate(eliteSendoutPercentageField, String.class)) : validate(eliteSendoutPercentageField,
                                Integer.class);
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
                        Set<?> spellIds = (Set<?>) spellsSelect.getValue();
                        List<SpellType> spellBook = new ArrayList<>(spellIds.size());
                        for (Object spellId : spellIds) {
                            spellBook.add(spellMap.get(spellId));
                        }

                        Race race;
                        if (itemId == null) {
                            race = new Race(name, shortName, soldierStrength, soldierNetworth, offSpecName, offSpecStrength, defSpecName,
                                    defSpecStrength, eliteName, eliteOffStrength, eliteDefStrength, eliteNetworth,
                                    eliteSendoutPercentage, intelSpec, isDragonImmune, pros, cons, bonuses, spellBook);
                            raceDAOProvider.get().save(race);
                            addRace(race, raceTable.getContainerDataSource());
                        } else {
                            race = raceMap.get(itemId);
                            race.setName(name);
                            race.setShortName(shortName);
                            race.setSoldierStrength(soldierStrength);
                            race.setSoldierNetworth(soldierNetworth);
                            race.setOffSpecName(offSpecName);
                            race.setOffSpecStrength(offSpecStrength);
                            race.setDefSpecName(defSpecName);
                            race.setDefSpecStrength(defSpecStrength);
                            race.setEliteName(eliteName);
                            race.setEliteOffStrength(eliteOffStrength);
                            race.setEliteDefStrength(eliteDefStrength);
                            race.setEliteNetworth(eliteNetworth);
                            race.setEliteSendoutPercentage(eliteSendoutPercentage);
                            race.setIntelAccuracySpecification(intelSpec);
                            race.setDragonImmune(isDragonImmune);
                            race.setPros(pros);
                            race.setCons(cons);
                            race.setBonuses(bonuses);
                            race.setSpellbook(spellBook);

                            Container container = raceTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("shortName").setValue(shortName);
                            item.getItemProperty("bonuses").setValue(createBonusList(race));
                            item.getItemProperty("spellBook").setValue(createNewSpellList(race));
                            raceDAOProvider.get().save(race);
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
            addComponent(soldierStrengthField);
            addComponent(soldierNetworthField);
            addComponent(offSpecNameField);
            addComponent(offSpecStrengthField);
            addComponent(defSpecNameField);
            addComponent(defSpecStrengthField);
            addComponent(eliteNameField);
            addComponent(eliteOffStrengthField);
            addComponent(eliteDefStrengthField);
            addComponent(eliteNetworthField);
            addComponent(eliteSendoutPercentageField);
            addComponent(intelAccuracySelect);
            addComponent(isDragonImmuneCheckBox);
            addComponent(prosArea);
            addComponent(consArea);
            addComponent(bonusesSelect);
            addComponent(spellsSelect);
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
