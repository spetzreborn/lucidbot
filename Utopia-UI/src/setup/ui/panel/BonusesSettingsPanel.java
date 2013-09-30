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
import com.vaadin.data.validator.DoubleValidator;
import com.vaadin.ui.*;
import database.daos.BonusDAO;
import database.models.Bonus;
import database.models.BonusApplicability;
import database.models.BonusType;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static api.database.transactions.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class BonusesSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "400px";
    private static final String POPUP_WIDTH = "250px";

    private final Provider<BonusDAO> bonusDAOProvider;

    private final Label description;
    private final Button addButton;

    private final Table bonusesTable;

    private final Map<Object, Bonus> bonusMap = new HashMap<>();

    @Inject
    public BonusesSettingsPanel(final Provider<BonusDAO> bonusDAOProvider) {
        this.bonusDAOProvider = bonusDAOProvider;

        setSpacing(true);
        setMargin(true);

        description = new Label("Add and save bonuses");

        addButton = new Button("Add Bonus", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        bonusesTable = new Table();
        bonusesTable.setContainerDataSource(getBonusContainer());
        bonusesTable.setColumnHeaders(new String[]{"Name", "Type", "Applicability", "Is Increase", "Value", "Edit", "Delete"});
        bonusesTable.setColumnExpandRatio("name", 0.3f);
        bonusesTable.setColumnExpandRatio("type", 0.3f);
        bonusesTable.setHeight("100%");
        bonusesTable.setWidth("700px");

        addComponent(description);
        addComponent(addButton);
        addComponent(bonusesTable);
    }

    private IndexedContainer getBonusContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("type", String.class, null);
        container.addContainerProperty("applicability", String.class, null);
        container.addContainerProperty("isIncrease", Boolean.class, null);
        container.addContainerProperty("value", Double.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (Bonus bonus : bonusDAOProvider.get().getAllBonuses()) {
                    addBonus(bonus, container);
                }
            }
        });

        return container;
    }

    private void addBonus(final Bonus bonus, final Container container) {
        final Object itemId = container.addItem();
        bonusMap.put(itemId, bonus);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(bonus.getName());
        item.getItemProperty("type").setValue(bonus.getType().getName());
        item.getItemProperty("applicability").setValue(bonus.getApplicability().getName());
        item.getItemProperty("isIncrease").setValue(bonus.isIncreasing());
        item.getItemProperty("value").setValue(bonus.getBonusValue());
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
                    Bonus remove = bonusMap.get(itemId);
                    if (remove.getId() != null) {
                        bonusDAOProvider.get().delete(remove);
                        bonusMap.remove(itemId);
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
        private final Select applicabilitySelect;
        private final CheckBox isIncreaseCheckBox;
        private final TextField valueField;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            Bonus bonus = null;
            if (itemId != null) {
                bonus = bonusMap.get(itemId);
            }

            nameField = new TextField("Name");
            nameField.setRequired(true);
            nameField.setWidth("100%");
            if (bonus != null) nameField.setValue(bonus.getName());

            typeSelect = new Select("Bonus Type", ListUtil.getNames(Lists.newArrayList(BonusType.values())));
            typeSelect.setRequired(true);
            typeSelect.setNullSelectionAllowed(false);
            if (bonus != null) typeSelect.setValue(bonus.getType().getName());

            applicabilitySelect = new Select("Applicability", ListUtil.getNames(Lists.newArrayList(BonusApplicability.values())));
            applicabilitySelect.setRequired(true);
            applicabilitySelect.setNullSelectionAllowed(false);
            if (bonus != null) applicabilitySelect.setValue(bonus.getApplicability().getName());

            isIncreaseCheckBox = new CheckBox("Is Increase");
            if (bonus != null) isIncreaseCheckBox.setValue(bonus.isIncreasing());

            valueField = new TextField("Value");
            valueField.setRequired(true);
            valueField.addValidator(new DoubleValidator("Value should be a decimal number"));
            if (bonus != null) valueField.setValue(bonus.getBonusValue());

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        BonusType bonusType = BonusType.fromName(validate(typeSelect, String.class));
                        BonusApplicability bonusApplicability = BonusApplicability.fromName(validate(applicabilitySelect, String.class));
                        boolean isIncrease = isIncreaseCheckBox.booleanValue();
                        double value = valueField.getValue() instanceof String ? Double.parseDouble(validate(valueField, String.class))
                                : validate(valueField, Double.class);

                        Bonus bonus;
                        if (itemId == null) {
                            bonus = new Bonus(name, bonusType, bonusApplicability, isIncrease, value);
                            bonusDAOProvider.get().save(bonus);
                            addBonus(bonus, bonusesTable.getContainerDataSource());
                        } else {
                            bonus = bonusMap.get(itemId);
                            bonus.setName(name);
                            bonus.setType(bonusType);
                            bonus.setApplicability(bonusApplicability);
                            bonus.setIncreasing(isIncrease);
                            bonus.setBonusValue(value);

                            Container container = bonusesTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("type").setValue(bonusType.getName());
                            item.getItemProperty("applicability").setValue(bonusApplicability.getName());
                            item.getItemProperty("isIncrease").setValue(isIncrease);
                            item.getItemProperty("value").setValue(value);
                            bonusDAOProvider.get().save(bonus);
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
            addComponent(applicabilitySelect);
            addComponent(isIncreaseCheckBox);
            addComponent(valueField);
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
