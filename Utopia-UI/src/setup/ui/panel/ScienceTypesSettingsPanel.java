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
import com.vaadin.data.validator.DoubleValidator;
import com.vaadin.ui.*;
import database.daos.BonusDAO;
import database.daos.ScienceTypeDAO;
import database.models.Bonus;
import database.models.ScienceType;
import setup.tools.ScienceTypes;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static api.database.transactions.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class ScienceTypesSettingsPanel extends VerticalLayout {
    private final Provider<ScienceTypeDAO> scienceTypeDAOProvider;

    private final Label description;
    private final Button addButton;
    private final Table scienceTypeTable;
    private final Button loadDefaultsButton;

    private final Map<Object, ScienceType> scienceTypeMap = new HashMap<>();
    private final Map<String, Bonus> bonusMap = new HashMap<>();

    @Inject
    public ScienceTypesSettingsPanel(final Provider<ScienceTypeDAO> scienceTypeDAOProvider, final ScienceTypes scienceTypesSetup,
                                     final Provider<BonusDAO> bonusDAOProvider) {
        this.scienceTypeDAOProvider = scienceTypeDAOProvider;

        setSpacing(true);
        setMargin(true);

        loadBonuses(bonusDAOProvider);

        description = new Label("Add and save science types");

        addButton = new Button("Add Science Type", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), "600px", "600px"));
            }
        });

        loadDefaultsButton = new Button("Load defaults", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                scienceTypesSetup.loadIntoDatabase();
                loadBonuses(bonusDAOProvider);
                scienceTypeTable.setContainerDataSource(getOpTypeContainer());
            }
        });

        scienceTypeTable = new Table();
        scienceTypeTable.setContainerDataSource(getOpTypeContainer());
        scienceTypeTable.setHeight("100%");
        scienceTypeTable.setWidth("750px");
        scienceTypeTable.setColumnHeaders(new String[]{"Name", "Angel Name", "Result Factor", "Bonuses", "Edit", "Delete"});
        scienceTypeTable.setColumnExpandRatio("name", 0.3f);
        scienceTypeTable.setColumnExpandRatio("bonuses", 0.3f);

        addComponent(description);
        addComponent(loadDefaultsButton);
        addComponent(addButton);
        addComponent(scienceTypeTable);
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
        container.addContainerProperty("angelName", String.class, null);
        container.addContainerProperty("resultFactor", Double.class, null);
        container.addContainerProperty("bonuses", VerticalLayout.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (ScienceType scienceType : scienceTypeDAOProvider.get().getAllScienceTypes()) {
                    addScienceType(scienceType, container);
                }
            }
        });

        return container;
    }

    private void addScienceType(final ScienceType scienceType, final Container container) {
        final Object itemId = container.addItem();
        scienceTypeMap.put(itemId, scienceType);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(scienceType.getName());
        item.getItemProperty("angelName").setValue(scienceType.getAngelName());
        item.getItemProperty("resultFactor").setValue(scienceType.getResultFactor());
        item.getItemProperty("bonuses").setValue(createBonusList(scienceType));
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
                    ScienceType remove = scienceTypeMap.get(itemId);
                    if (remove.getId() != null) {
                        scienceTypeDAOProvider.get().delete(remove);
                        scienceTypeMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static VerticalLayout createBonusList(final ScienceType scienceType) {
        VerticalLayout bonusList = new VerticalLayout();
        for (Bonus bonus : scienceType.getBonuses()) {
            bonusList.addComponent(new Label(bonus.getName()));
        }
        return bonusList;
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final TextField angelNameField;
        private final TextArea descriptionArea;
        private final TextField resultFactorField;
        private final TwinColSelect bonusesSelect;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            ScienceType scienceType = null;
            if (itemId != null) {
                scienceType = scienceTypeMap.get(itemId);
            }

            nameField = new TextField("Name");
            nameField.setRequired(true);
            if (scienceType != null) nameField.setValue(scienceType.getName());

            angelNameField = new TextField("Angel Name");
            angelNameField.setRequired(true);
            angelNameField.setWidth("100%");
            if (scienceType != null) angelNameField.setValue(scienceType.getAngelName());

            descriptionArea = new TextArea("Description");
            descriptionArea.setRows(10);
            descriptionArea.setColumns(20);
            descriptionArea.setWordwrap(true);
            descriptionArea.setNullRepresentation("");
            descriptionArea.setNullSettingAllowed(true);
            if (scienceType != null) descriptionArea.setValue(scienceType.getDescription());

            resultFactorField = new TextField("Result Factor");
            resultFactorField.addValidator(new DoubleValidator("Must be a decimal number"));
            if (scienceType != null) resultFactorField.setValue(scienceType.getResultFactor());

            bonusesSelect = new TwinColSelect("Bonuses", bonusMap.keySet());
            bonusesSelect.setRows(10);
            bonusesSelect.setNullSelectionAllowed(true);
            bonusesSelect.setMultiSelect(true);
            bonusesSelect.setWidth("100%");
            if (scienceType != null) bonusesSelect.setValue(ListUtil.getNames(scienceType.getBonuses()));

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        String angelName = validate(angelNameField, String.class);
                        String description = validate(descriptionArea, String.class);
                        double resultFactor = resultFactorField.getValue() instanceof String ? Double
                                .parseDouble(validate(resultFactorField, String.class)) : validate(resultFactorField, Double.class);
                        Set<?> bonusIds = (Set<?>) bonusesSelect.getValue();
                        Set<Bonus> bonuses = new HashSet<>(bonusIds.size());
                        for (Object bonusId : bonusIds) {
                            bonuses.add(bonusMap.get(bonusId));
                        }

                        ScienceType scienceType;
                        if (itemId == null) {
                            scienceType = new ScienceType(name, angelName, description, resultFactor, bonuses);
                            scienceTypeDAOProvider.get().save(scienceType);
                            addScienceType(scienceType, scienceTypeTable.getContainerDataSource());
                        } else {
                            scienceType = scienceTypeMap.get(itemId);
                            scienceType.setName(name);
                            scienceType.setAngelName(angelName);
                            scienceType.setDescription(description);
                            scienceType.setResultFactor(resultFactor);
                            scienceType.setBonuses(bonuses);

                            Container container = scienceTypeTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("angelName").setValue(angelName);
                            item.getItemProperty("resultFactor").setValue(resultFactor);
                            item.getItemProperty("bonuses").setValue(createBonusList(scienceType));
                            scienceTypeDAOProvider.get().save(scienceType);
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
            addComponent(angelNameField);
            addComponent(descriptionArea);
            addComponent(resultFactorField);
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
