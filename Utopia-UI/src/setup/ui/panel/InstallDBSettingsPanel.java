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

import api.database.DatabaseManager;
import api.database.DatabaseState;
import api.settings.PropertiesCollection;
import api.settings.PropertiesConfig;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import setup.ui.custom.InstallTab;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Log4j
public class InstallDBSettingsPanel extends Panel implements InstallTab {
    private final Injector injector;
    private final Accordion accordion;

    @Inject
    public InstallDBSettingsPanel(final Injector injector) {
        super("Install");
        this.injector = injector;
        accordion = new Accordion();

        accordion.addTab(new EasyInstallPanel(), "Easy install");
        accordion.addTab(new AdvancedInstallPanel(), "Advanced install");

        addComponent(accordion);
    }

    @Override
    public ComponentContainer getContainer() {
        return accordion;
    }

    private void notifySuccess() {
        Window.Notification notification = new Window.Notification(
                "The database was created successfully! Please restart the bot and come back here to go through setup.",
                Window.Notification.TYPE_HUMANIZED_MESSAGE);
        notification.setDelayMsec(2000);
        accordion.getWindow().showNotification(notification);
    }

    private void notifyFailure() {
        accordion.getWindow()
                .showNotification("The database could not be created. Please check the logs for more information about the error.",
                        Window.Notification.TYPE_ERROR_MESSAGE);
    }

    private class EasyInstallPanel extends VerticalLayout {
        private final Label description;
        private final Button installButton;

        private EasyInstallPanel() {
            description = new Label(
                    "The easy installation simply uses default settings and is targeted at the users who don't care about " +
                            "how to use databases and such and just want the bot to work with minimum effort.");
            installButton = new Button("Install", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    PropertiesCollection properties = injector.getInstance(PropertiesCollection.class);
                    properties.change(new DBSettings().toMap());

                    DatabaseManager databaseManager = injector.getInstance(DatabaseManager.class);
                    databaseManager.createSchema();
                    if (databaseManager.getDatabaseState() == DatabaseState.CONNECTED_FULLY) {
                        notifySuccess();
                    } else notifyFailure();
                }
            });

            addComponent(description);
            addComponent(installButton);
            setMargin(true);
            setSpacing(true);
        }
    }

    private class AdvancedInstallPanel extends VerticalLayout {
        private final DBSettings dbSettings;
        private final Form form;
        private final Button installButton;
        private final Label status;

        private AdvancedInstallPanel() {
            dbSettings = new DBSettings();
            BeanItem<DBSettings> dbSettingsItem = new BeanItem<>(dbSettings);

            form = new Form();
            form.setWriteThrough(false);
            form.setFormFieldFactory(new DBSettingsFieldFactory());
            form.setItemDataSource(dbSettingsItem);
            form.setVisibleItemProperties(Lists.newArrayList("host", "name", "username", "password"));

            installButton = new Button("Install", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        form.commit();

                        PropertiesCollection properties = injector.getInstance(PropertiesCollection.class);
                        properties.change(dbSettings.toMap());

                        DatabaseManager databaseManager = injector.getInstance(DatabaseManager.class);
                        databaseManager.createSchema();
                        if (databaseManager.getDatabaseState() == DatabaseState.CONNECTED_FULLY) {
                            notifySuccess();
                        } else notifyFailure();
                    } catch (Exception e) {
                        InstallDBSettingsPanel.log.warn("Failed", e);
                    }
                }
            });
            status = new Label();

            addComponent(form);
            addComponent(installButton);
            addComponent(status);
            setMargin(true);
            setSpacing(true);
        }
    }

    private static class DBSettingsFieldFactory extends DefaultFieldFactory {
        @Override
        public Field createField(final Item item, final Object propertyId, final Component uiContext) {
            Field field = super.createField(item, propertyId, uiContext);

            switch (propertyId.toString()) {
                case "host":
                    TextField hostField = (TextField) field;
                    hostField.setRequired(true);
                    hostField.setRequiredError("Please enter a database host");
                    break;
                case "name":
                    TextField nameField = (TextField) field;
                    nameField.setRequired(true);
                    nameField.setRequiredError("Please enter a database name");
                    break;
                case "username":
                    TextField usernameField = (TextField) field;
                    usernameField.setRequired(true);
                    usernameField.setRequiredError("Please enter a username");
                    break;
                case "password":
                    TextField passwordField = (TextField) field;
                    passwordField.setRequired(false);
                    passwordField.setNullRepresentation("");
                    passwordField.setNullSettingAllowed(true);
                    break;
            }

            return field;
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class DBSettings {
        private String host = "embedded";
        private String name = "lucidbot";
        private String username = "root";
        private String password = "password";

        private Map<String, String> toMap() {
            Map<String, String> out = new HashMap<>();
            out.put(PropertiesConfig.DB_HOST, host);
            out.put(PropertiesConfig.DB_NAME, name);
            out.put(PropertiesConfig.DB_USERNAME, username);
            out.put(PropertiesConfig.DB_PASSWORD, password);
            return out;
        }
    }
}
