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

import api.commands.Command;
import api.commands.CommandCache;
import api.database.SimpleTransactionTask;
import api.database.daos.CommandDefinitionDAO;
import api.database.models.AccessLevel;
import api.database.models.CommandDefinition;
import api.events.DelayedEventPoster;
import api.tools.collections.ListUtil;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.*;
import setup.tools.VaadinUtil;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static api.database.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class CommandsSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "600px";
    private static final String POPUP_WIDTH = "400px";

    private final Provider<CommandDefinitionDAO> commandDefinitionDAOProvider;
    private final CommandCache commandCache;

    private final Label description;
    private final Button addButton;
    private final Table commandTable;

    private final Map<Object, Command> commandMap = new HashMap<>();
    private final Map<Command, CommandDefinition> commandDefinitionMap = new HashMap<>();

    @Inject
    public CommandsSettingsPanel(final Provider<CommandDefinitionDAO> commandDefinitionDAOProvider, final CommandCache commandCache) {
        this.commandDefinitionDAOProvider = commandDefinitionDAOProvider;
        this.commandCache = commandCache;

        setSpacing(true);
        setMargin(true);

        description = new Label("Add and edit command info");

        addButton = new Button("Add Command Definition", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent clickEvent) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        commandTable = new Table();
        commandTable.setContainerDataSource(getCommandDefinitionContainer());
        commandTable.setColumnHeaders(new String[]{"Name", "Command Type", "Access", "Template", "Edit"});
        commandTable.setHeight("100%");
        commandTable.setWidth("650px");

        addComponent(description);
        addComponent(addButton);
        addComponent(commandTable);
    }

    private IndexedContainer getCommandDefinitionContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("type", String.class, null);
        container.addContainerProperty("access", String.class, null);
        container.addContainerProperty("template", String.class, null);
        container.addContainerProperty("edit", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (Command command : commandCache.getAllCommands()) {
                    addCommand(command, container);
                }
                for (CommandDefinition definition : commandDefinitionDAOProvider.get().getAllCommandDefinitions()) {
                    Command command = commandCache.getCommandFromName(definition.getName());
                    if (command != null) {
                        commandDefinitionMap.put(command, definition);
                    }
                }
                commandCache.refreshDefinitions();
            }
        });

        return container;
    }

    private void addCommand(final Command command, final Container container) {
        final Object itemId = container.addItem();
        commandMap.put(itemId, command);
        final Item item = container.getItem(itemId);
        item.getItemProperty("name").setValue(command.getName());
        item.getItemProperty("type").setValue(command.getCommandType());
        item.getItemProperty("access").setValue(command.getRequiredAccessLevel().getName());
        item.getItemProperty("template").setValue(command.getTemplateFile());
        item.getItemProperty("edit").setValue(new Button("Edit", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent clickEvent) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(itemId), POPUP_HEIGHT, POPUP_WIDTH));
            }
        }));
    }

    private class EditPopupContent extends VerticalLayout {
        private final Select nameSelect;
        private final TextField typeField;
        private final Select accessSelect;
        private final TextField templateField;
        private final TextArea syntaxArea;
        private final TextArea helpArea;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            Command command = commandMap.get(itemId);
            boolean isNew = itemId == null || !commandDefinitionMap.containsKey(command);

            nameSelect = new Select("Command name", isNew ? ListUtil.getNames(getCommandsWithoutDefinition())
                    : ListUtil.getNames(commandCache.getAllCommands()));
            nameSelect.setRequired(true);
            nameSelect.setWidth("100%");
            nameSelect.setNullSelectionAllowed(false);
            if (command != null) {
                nameSelect.setValue(command.getName());
                nameSelect.setEnabled(false);
            }

            typeField = new TextField("Command type");
            typeField.setRequired(true);
            typeField.setWidth("100%");
            typeField.addValidator(new StringLengthValidator("Command type is required", 1, 100, false));
            if (command != null) typeField.setValue(command.getCommandType());

            accessSelect = new Select("Access level", ListUtil.getNames(Lists.newArrayList(AccessLevel.values())));
            accessSelect.setRequired(true);
            accessSelect.setNullSelectionAllowed(false);
            if (command != null) accessSelect.setValue(command.getRequiredAccessLevel().getName());

            templateField = new TextField("Template file");
            templateField.setWidth("100%");
            templateField.setNullRepresentation("");
            templateField.setNullSettingAllowed(true);
            templateField.addValidator(new StringLengthValidator("Template file name too long", 1, 100, false));
            if (command != null) templateField.setValue(command.getTemplateFile());

            syntaxArea = new TextArea("Syntax");
            syntaxArea.setWidth("100%");
            syntaxArea.setNullRepresentation("");
            syntaxArea.setNullSettingAllowed(true);
            syntaxArea.setRows(10);
            syntaxArea.setWordwrap(true);
            syntaxArea.addValidator(new StringLengthValidator("Syntax is too long", 0, 5000, true));
            if (command != null) syntaxArea.setValue(command.getSyntax());

            helpArea = new TextArea("Help text");
            helpArea.setWidth("100%");
            helpArea.setNullRepresentation("");
            helpArea.setNullSettingAllowed(true);
            helpArea.setRows(10);
            helpArea.setWordwrap(true);
            helpArea.addValidator(new StringLengthValidator("Help is too long", 0, 5000, true));
            if (command != null) helpArea.setValue(command.getHelpText());

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String name = validate(nameSelect, String.class);
                        String type = validate(typeField, String.class);
                        AccessLevel accessLevel = AccessLevel.fromName(validate(accessSelect, String.class));
                        String templateFile = validate(templateField, String.class);
                        String syntax = validate(syntaxArea, String.class);
                        String helpText = validate(helpArea, String.class);

                        Command command = itemId == null ? null : commandMap.get(itemId);
                        CommandDefinition commandDefinition = command == null ? null : commandDefinitionMap.get(command);
                        if (commandDefinition == null) {
                            commandDefinition = new CommandDefinition(name, syntax, helpText, type, templateFile, accessLevel);
                            command = commandCache.getCommandFromName(name);
                        } else {
                            commandDefinition = commandDefinitionMap.get(command);
                            commandDefinition.setAccessLevel(accessLevel);
                            commandDefinition.setCommandType(type);
                            commandDefinition.setHelpText(helpText);
                            commandDefinition.setSyntax(syntax);
                            commandDefinition.setTemplateFile(templateFile);
                        }
                        commandDefinitionDAOProvider.get().save(commandDefinition);
                        commandDefinitionMap.put(command, commandDefinition);
                        commandDefinitionDAOProvider.get().loadDefinitionFromDB(command);

                        Container container = commandTable.getContainerDataSource();
                        Item item = itemId == null ? getItemFromCommand(command, container) : container.getItem(itemId);
                        item.getItemProperty("type").setValue(type);
                        item.getItemProperty("access").setValue(accessLevel.getName());
                        item.getItemProperty("template").setValue(templateFile);

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

            addComponent(nameSelect);
            addComponent(typeField);
            addComponent(accessSelect);
            addComponent(templateField);
            addComponent(syntaxArea);
            addComponent(helpArea);
            HorizontalLayout buttons = new HorizontalLayout();
            buttons.addComponent(saveButton);
            buttons.addComponent(cancelButton);
            addComponent(buttons);
        }

        private Item getItemFromCommand(final Command command, final Container container) {
            for (Map.Entry<Object, Command> entry : commandMap.entrySet()) {
                if (entry.getValue().equals(command)) return container.getItem(entry.getKey());
            }
            throw new IllegalStateException("Added command definition for unknown command somehow...");
        }

        private void removePopup(final Button.ClickEvent event) {
            Window parent = event.getButton().getWindow();
            parent.getParent().removeWindow(parent);
        }

        private Collection<Command> getCommandsWithoutDefinition() {
            Collection<Command> allCommands = commandCache.getAllCommands();
            Collection<Command> out = new ArrayList<>(allCommands.size());
            for (Command command : allCommands) {
                if (!commandDefinitionMap.containsKey(command)) out.add(command);
            }
            return out;
        }
    }
}
