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
import api.tools.text.StringUtil;
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.DoubleValidator;
import com.vaadin.ui.*;
import database.daos.BonusDAO;
import database.daos.BuildingDAO;
import database.models.Bonus;
import database.models.Building;
import database.models.BuildingFormula;
import setup.tools.Buildings;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.*;

import static api.database.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class BuildingsSettingsPanel extends VerticalLayout {
    private final Provider<BuildingDAO> buildingDAOProvider;

    private final Label description;
    private final Button addBuildingButton;
    private final Table buildingTable;
    private final Button addFormulaButton;
    private final Table buildingFormulaTable;
    private final Button loadDefaultsButton;

    private final Map<Object, Building> buildingMap = new HashMap<>();
    private final Map<Object, BuildingFormula> buildingFormulaMap = new HashMap<>();
    private final Map<String, Bonus> bonusMap = new HashMap<>();

    @Inject
    public BuildingsSettingsPanel(final Provider<BuildingDAO> buildingDAOProvider, final Buildings buildingsSetup,
                                  final Provider<BonusDAO> bonusDAOProvider) {
        this.buildingDAOProvider = buildingDAOProvider;

        setSpacing(true);
        setMargin(true);

        loadBonuses(bonusDAOProvider);

        description = new Label("Add and save buildings");

        addBuildingButton = new Button("Add Building", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditBuildingPopupContent(null), "600px", "600px"));
            }
        });

        addFormulaButton = new Button("Add Building Formula", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditBuildingFormulaPopupContent(null), "600px", "600px"));
            }
        });

        loadDefaultsButton = new Button("Load defaults", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                buildingsSetup.loadIntoDatabase();
                loadBonuses(bonusDAOProvider);
                buildingTable.setContainerDataSource(getBuildingContainer());
            }
        });

        buildingFormulaTable = new Table();
        buildingFormulaTable.setContainerDataSource(getBuildingFormulaContainer());
        buildingFormulaTable.setHeight("100%");
        buildingFormulaTable.setWidth("1100px");
        buildingFormulaTable.setColumnHeaders(new String[]{"Building", "Formula", "Result Text", "Cap", "Bonus", "Edit", "Delete"});

        buildingTable = new Table();
        buildingTable.setContainerDataSource(getBuildingContainer());
        buildingTable.setHeight("100%");
        buildingTable.setWidth("750px");
        buildingTable.setColumnHeaders(new String[]{"Name", "Short Name", "Syntax", "Edit", "Delete"});

        addComponent(description);
        addComponent(loadDefaultsButton);
        addComponent(addBuildingButton);
        addComponent(buildingTable);
        addComponent(addFormulaButton);
        addComponent(buildingFormulaTable);
    }

    private void loadBonuses(final Provider<BonusDAO> bonusDAOProvider) {
        bonusMap.clear();
        for (Bonus bonus : bonusDAOProvider.get().getAllBonuses()) {
            bonusMap.put(bonus.getName(), bonus);
        }
    }

    private IndexedContainer getBuildingContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("shortName", String.class, null);
        container.addContainerProperty("syntax", String.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (Building building : buildingDAOProvider.get().getAllBuildings()) {
                    addBuilding(building, container);
                    for (BuildingFormula formula : building.getFormulas()) {
                        addBuildingFormula(formula, buildingFormulaTable.getContainerDataSource());
                    }
                }
            }
        });

        return container;
    }

    private void addBuilding(final Building building, final Container container) {
        final Object itemId = container.addItem();
        buildingMap.put(itemId, building);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(building.getName());
        item.getItemProperty("shortName").setValue(building.getShortName());
        item.getItemProperty("syntax").setValue(building.getSyntax());
        item.getItemProperty("edit").setValue(new Button("Edit", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditBuildingPopupContent(itemId), "600px", "600px"));
            }
        }));
        item.getItemProperty("delete").setValue(new Button("Delete", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                try {
                    Building remove = buildingMap.get(itemId);
                    if (remove.getId() != null) {
                        List<Object> formulasToRemoveFromMap = new ArrayList<>(remove.getFormulas().size());
                        for (BuildingFormula formula : remove.getFormulas()) {
                            for (Map.Entry<Object, BuildingFormula> entry : buildingFormulaMap.entrySet()) {
                                if (entry.getValue().equals(formula)) formulasToRemoveFromMap.add(entry.getKey());
                            }
                        }
                        for (Object itemId : formulasToRemoveFromMap) {
                            buildingFormulaMap.remove(itemId);
                            buildingFormulaTable.removeItem(itemId);
                        }
                        buildingDAOProvider.get().delete(remove);
                        buildingMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static IndexedContainer getBuildingFormulaContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("building", String.class, null);
        container.addContainerProperty("formula", String.class, null);
        container.addContainerProperty("resultText", String.class, null);
        container.addContainerProperty("cap", Double.class, null);
        container.addContainerProperty("bonus", String.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        return container;
    }

    private void addBuildingFormula(final BuildingFormula formula, final Container container) {
        final Object itemId = container.addItem();
        buildingFormulaMap.put(itemId, formula);
        final Item item = container.getItem(itemId);
        item.getItemProperty("building").setValue(formula.getBuilding().getName());
        item.getItemProperty("formula").setValue(formula.getFormula());
        item.getItemProperty("resultText").setValue(formula.getResultText());
        item.getItemProperty("cap").setValue(formula.getCap());
        item.getItemProperty("bonus").setValue(formula.getBonus() == null ? null : formula.getBonus().getName());
        item.getItemProperty("edit").setValue(new Button("Edit", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditBuildingFormulaPopupContent(itemId), "600px", "600px"));
            }
        }));
        item.getItemProperty("delete").setValue(new Button("Delete", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                try {
                    container.removeItem(itemId);
                    BuildingFormula remove = buildingFormulaMap.remove(itemId);
                    Building building = remove.getBuilding();
                    building.getFormulas().remove(remove);
                    if (remove.getId() != null) buildingDAOProvider.get().save(building);
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private class EditBuildingPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final TextField shortNameField;
        private final TextField syntaxField;
        private final Button saveButton;
        private final Button cancelButton;

        private EditBuildingPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            Building building = null;
            if (itemId != null) {
                building = buildingMap.get(itemId);
            }

            nameField = new TextField("Name");
            nameField.setRequired(true);
            if (building != null) nameField.setValue(building.getName());

            shortNameField = new TextField("Short Name");
            shortNameField.setRequired(true);
            if (building != null) shortNameField.setValue(building.getShortName());

            syntaxField = new TextField("Syntax");
            if (building != null) syntaxField.setValue(building.getSyntax());

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        String shortName = validate(shortNameField, String.class);
                        String syntax = validate(syntaxField, String.class);

                        Building building;
                        if (itemId == null) {
                            building = new Building(name, shortName, syntax, Collections.<BuildingFormula>emptyList());
                            buildingDAOProvider.get().save(building);
                            addBuilding(building, buildingTable.getContainerDataSource());
                        } else {
                            building = buildingMap.get(itemId);
                            building.setName(name);
                            building.setShortName(shortName);
                            building.setSyntax(syntax);

                            Container container = buildingTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("shortName").setValue(shortName);
                            item.getItemProperty("syntax").setValue(syntax);
                            buildingDAOProvider.get().save(building);
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
            addComponent(syntaxField);
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

    private class EditBuildingFormulaPopupContent extends VerticalLayout {
        private final Select buildingSelect;
        private final TextField formulaField;
        private final TextField resultTextField;
        private final TextField capField;
        private final Select bonusSelect;
        private final Button saveButton;
        private final Button cancelButton;

        private EditBuildingFormulaPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            BuildingFormula formula = null;
            if (itemId != null) {
                formula = buildingFormulaMap.get(itemId);
            }

            buildingSelect = new Select("Building", ListUtil.getNames(buildingMap.values()));
            buildingSelect.setNullSelectionAllowed(false);
            buildingSelect.setRequired(true);
            if (formula != null) buildingSelect.setValue(formula.getBuilding().getName());

            formulaField = new TextField("Formula");
            formulaField.setRequired(true);
            formulaField.setWidth("100%");
            if (formula != null) formulaField.setValue(formula.getFormula());

            resultTextField = new TextField("Result Text");
            resultTextField.setRequired(true);
            resultTextField.setWidth("100%");
            if (formula != null) resultTextField.setValue(formula.getResultText());

            capField = new TextField("Cap");
            capField.setRequired(false);
            capField.setNullSettingAllowed(true);
            capField.setNullRepresentation("");
            capField.addValidator(new DoubleValidator("Must be a decimal value"));
            if (formula != null) capField.setValue(formula.getCap());

            bonusSelect = new Select("Bonus", bonusMap.keySet());
            bonusSelect.setNullSelectionAllowed(true);
            if (formula != null && formula.getBonus() != null) bonusSelect.setValue(formula.getBonus().getName());

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String buildingName = validate(buildingSelect, String.class);
                        String formula = validate(formulaField, String.class);
                        String resultText = validate(resultTextField, String.class);
                        String capString = capField.getValue() == null ? null : capField.getValue().toString();
                        Double cap = StringUtil.isNullOrEmpty(capString) ? null : capField.getValue() instanceof String ? Double
                                .parseDouble(validate(capField, String.class)) : validate(capField, Double.class);
                        Bonus bonus = bonusSelect.getValue() == null ? null : bonusMap.get(bonusSelect.getValue());

                        BuildingFormula buildingFormula;
                        Building building = getBuildingFromName(buildingName);
                        if (itemId == null) {
                            buildingFormula = new BuildingFormula(building, formula, resultText, cap, bonus);
                            building.getFormulas().add(buildingFormula);
                            addBuildingFormula(buildingFormula, buildingFormulaTable.getContainerDataSource());
                        } else {
                            buildingFormula = buildingFormulaMap.get(itemId);
                            Building previousBuilding = buildingFormula.getBuilding();
                            if (!previousBuilding.equals(building)) {
                                previousBuilding.getFormulas().remove(buildingFormula);
                                buildingDAOProvider.get().save(previousBuilding);
                                buildingFormula = new BuildingFormula();
                                buildingFormulaMap.put(itemId, buildingFormula);
                                buildingFormula.setBuilding(building);
                                building.getFormulas().add(buildingFormula);
                            }
                            buildingFormula.setFormula(formula);
                            buildingFormula.setResultText(resultText);
                            buildingFormula.setCap(cap);
                            buildingFormula.setBonus(bonus);

                            Container container = buildingFormulaTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("building").setValue(building.getName());
                            item.getItemProperty("formula").setValue(formula);
                            item.getItemProperty("resultText").setValue(resultText);
                            item.getItemProperty("cap").setValue(cap);
                            item.getItemProperty("bonus").setValue(bonus == null ? null : bonus.getName());
                        }
                        buildingDAOProvider.get().save(building);
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

            addComponent(buildingSelect);
            addComponent(formulaField);
            addComponent(resultTextField);
            addComponent(capField);
            addComponent(bonusSelect);
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

    private Building getBuildingFromName(final String name) {
        for (Building building : buildingMap.values()) {
            if (building.getName().equals(name)) return building;
        }
        throw new IllegalArgumentException("No such building: " + name);
    }
}
