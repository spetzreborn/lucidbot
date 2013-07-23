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
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.IntegerValidator;
import com.vaadin.ui.*;
import database.daos.BonusDAO;
import database.daos.HonorTitleDAO;
import database.models.Bonus;
import database.models.HonorTitle;
import setup.tools.HonorTitles;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static api.database.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class TitlesSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "600px";
    private static final String POPUP_WIDTH = "450px";

    private final Provider<HonorTitleDAO> honorTitleDAOProvider;

    private final Label description;
    private final Button addButton;
    private final Table titleTable;
    private final Button loadDefaultsButton;

    private final Map<Object, HonorTitle> titleMap = new HashMap<>();
    private final Map<String, Bonus> bonusMap = new HashMap<>();

    @Inject
    public TitlesSettingsPanel(final Provider<HonorTitleDAO> honorTitleDAOProvider, final Provider<BonusDAO> bonusDAOProvider,
                               final HonorTitles titlesSetup) {
        this.honorTitleDAOProvider = honorTitleDAOProvider;

        setSpacing(true);
        setMargin(true);

        loadBonuses(bonusDAOProvider);

        description = new Label("Add and save honor titles");

        addButton = new Button("Add Title", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        loadDefaultsButton = new Button("Load defaults", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                titlesSetup.loadIntoDatabase();
                loadBonuses(bonusDAOProvider);
                titleTable.setContainerDataSource(getTitleContainer());
            }
        });

        titleTable = new Table();
        titleTable.setContainerDataSource(getTitleContainer());
        titleTable.setHeight("100%");
        titleTable.setWidth("650px");
        titleTable.setColumnHeaders(new String[]{"Name", "Alias", "Lower Bound", "Upper Bound", "Bonuses", "Edit", "Delete"});
        titleTable.setColumnExpandRatio("name", 0.2f);
        titleTable.setColumnExpandRatio("alias", 0.3f);
        titleTable.setColumnExpandRatio("bonuses", 0.5f);

        addComponent(description);
        addComponent(loadDefaultsButton);
        addComponent(addButton);
        addComponent(titleTable);
    }

    private void loadBonuses(final Provider<BonusDAO> bonusDAOProvider) {
        bonusMap.clear();
        for (Bonus bonus : bonusDAOProvider.get().getAllBonuses()) {
            bonusMap.put(bonus.getName(), bonus);
        }
    }

    private IndexedContainer getTitleContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("alias", String.class, null);
        container.addContainerProperty("lowerBound", Integer.class, null);
        container.addContainerProperty("upperBound", Integer.class, null);
        container.addContainerProperty("bonuses", VerticalLayout.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (HonorTitle title : honorTitleDAOProvider.get().getAllHonorTitles()) {
                    addTitle(title, container);
                }
            }
        });

        return container;
    }

    private void addTitle(final HonorTitle title, final Container container) {
        final Object itemId = container.addItem();
        titleMap.put(itemId, title);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(title.getName());
        item.getItemProperty("alias").setValue(title.getAlias());
        item.getItemProperty("lowerBound").setValue(title.getLowerBound());
        item.getItemProperty("upperBound").setValue(title.getUpperBound());
        item.getItemProperty("bonuses").setValue(createBonusList(title));
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
                    HonorTitle remove = titleMap.get(itemId);
                    if (remove.getId() != null) {
                        honorTitleDAOProvider.get().delete(remove);
                        titleMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static VerticalLayout createBonusList(final HonorTitle title) {
        VerticalLayout bonusList = new VerticalLayout();
        for (Bonus bonus : title.getBonuses()) {
            bonusList.addComponent(new Label(bonus.getName()));
        }
        return bonusList;
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField nameField;
        private final TextField aliasField;
        private final TextField lowerBoundField;
        private final TextField upperBoundField;
        private final TwinColSelect bonusesSelect;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            HonorTitle title = null;
            if (itemId != null) {
                title = titleMap.get(itemId);
            }

            nameField = new TextField("Name");
            nameField.setRequired(true);
            if (title != null) nameField.setValue(title.getName());

            aliasField = new TextField("Alias");
            aliasField.setRequired(true);
            if (title != null) aliasField.setValue(title.getAlias());

            lowerBoundField = new TextField("Lower Bound");
            lowerBoundField.setRequired(true);
            lowerBoundField.addValidator(new IntegerValidator("Must be an integer"));
            if (title != null) lowerBoundField.setValue(title.getLowerBound());

            upperBoundField = new TextField("Upper Bound");
            upperBoundField.setRequired(true);
            upperBoundField.addValidator(new IntegerValidator("Must be an integer"));
            if (title != null) upperBoundField.setValue(title.getLowerBound());

            bonusesSelect = new TwinColSelect("Bonuses", bonusMap.keySet());
            bonusesSelect.setRows(10);
            bonusesSelect.setNullSelectionAllowed(true);
            bonusesSelect.setMultiSelect(true);
            bonusesSelect.setWidth("100%");
            if (title != null) bonusesSelect.setValue(ListUtil.getNames(title.getBonuses()));

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameField, String.class);
                        String alias = validate(aliasField, String.class);
                        int lowerBound =
                                lowerBoundField.getValue() instanceof String ? Integer.parseInt(validate(lowerBoundField, String.class))
                                        : validate(lowerBoundField, Integer.class);
                        int upperBound =
                                upperBoundField.getValue() instanceof String ? Integer.parseInt(validate(upperBoundField, String.class))
                                        : validate(upperBoundField, Integer.class);
                        Set<?> bonusIds = (Set<?>) bonusesSelect.getValue();
                        Set<Bonus> bonuses = new HashSet<>(bonusIds.size());
                        for (Object bonusId : bonusIds) {
                            bonuses.add(bonusMap.get(bonusId));
                        }

                        HonorTitle title;
                        if (itemId == null) {
                            title = new HonorTitle(name, alias, lowerBound, upperBound, bonuses);
                            honorTitleDAOProvider.get().save(title);
                            addTitle(title, titleTable.getContainerDataSource());
                        } else {
                            title = titleMap.get(itemId);
                            title.setName(name);
                            title.setAlias(alias);
                            title.setLowerBound(lowerBound);
                            title.setUpperBound(upperBound);
                            title.setBonuses(bonuses);

                            Container container = titleTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("name").setValue(name);
                            item.getItemProperty("alias").setValue(alias);
                            item.getItemProperty("lowerBound").setValue(lowerBound);
                            item.getItemProperty("upperBound").setValue(upperBound);
                            item.getItemProperty("bonuses").setValue(createBonusList(title));
                            honorTitleDAOProvider.get().save(title);
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
            addComponent(lowerBoundField);
            addComponent(upperBoundField);
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
