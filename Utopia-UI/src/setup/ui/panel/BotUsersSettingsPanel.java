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

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.database.models.Nickname;
import api.database.transactions.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import api.irc.ValidationType;
import com.google.inject.Provider;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.ui.*;
import database.daos.ProvinceDAO;
import database.daos.UserActivitiesDAO;
import database.models.UserActivities;
import setup.tools.VaadinUtil;
import tools.parsing.UtopiaValidationType;

import javax.inject.Inject;
import java.util.*;

import static api.database.transactions.Transactions.inTransaction;
import static setup.tools.VaadinUtil.validate;

public class BotUsersSettingsPanel extends VerticalLayout {
    private static final String POPUP_HEIGHT = "600px";
    private static final String POPUP_WIDTH = "300px";

    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<UserActivitiesDAO> userActivitiesDAOProvider;

    private final Label description;
    private final Button addButton;
    private final Table userTable;

    private final Map<Object, BotUser> userMap = new HashMap<>();

    @Inject
    public BotUsersSettingsPanel(final Provider<BotUserDAO> botUserDAOProvider, final Provider<ProvinceDAO> provinceDAOProvider,
                                 final Provider<UserActivitiesDAO> userActivitiesDAOProvider) {
        this.botUserDAOProvider = botUserDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
        this.userActivitiesDAOProvider = userActivitiesDAOProvider;

        setSpacing(true);
        setMargin(true);

        description = new Label("Add and save bot users");

        addButton = new Button("Add User", new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent event) {
                getWindow().addWindow(VaadinUtil.createPopupWindow(new EditPopupContent(null), POPUP_HEIGHT, POPUP_WIDTH));
            }
        });

        userTable = new Table();
        userTable.setContainerDataSource(getBotContainer());
        userTable.setColumnHeaders(new String[]{
                "Main Nick", "Nicks", "Admin", "Owner", "TZ", "DST", "Country", "Name", "Email", "SMS", "SMS Works", "Edit",
                "Delete"
        });
        userTable.setColumnWidth("mainNick", 100);
        userTable.setColumnWidth("nicks", 100);
        userTable.setColumnWidth("country", 100);
        userTable.setColumnWidth("realName", 80);
        userTable.setColumnWidth("email", 175);
        userTable.setColumnWidth("sms", 175);
        userTable.setHeight("100%");
        userTable.setWidth("100%");

        addComponent(description);
        addComponent(addButton);
        addComponent(userTable);
    }

    private IndexedContainer getBotContainer() {
        final IndexedContainer container = new IndexedContainer();

        container.addContainerProperty("mainNick", String.class, null);
        container.addContainerProperty("nicks", VerticalLayout.class, null);
        container.addContainerProperty("isAdmin", Boolean.class, null);
        container.addContainerProperty("isOwner", Boolean.class, null);
        container.addContainerProperty("timeZone", String.class, null);
        container.addContainerProperty("dst", Boolean.class, null);
        container.addContainerProperty("country", String.class, null);
        container.addContainerProperty("realName", String.class, null);
        container.addContainerProperty("email", String.class, null);
        container.addContainerProperty("sms", String.class, null);
        container.addContainerProperty("smsWorks", Boolean.class, null);
        container.addContainerProperty("edit", Button.class, null);
        container.addContainerProperty("delete", Button.class, null);

        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventPoster) {
                for (BotUser user : botUserDAOProvider.get().getAllUsers()) {
                    addUser(user, container);
                }
            }
        });

        return container;
    }

    private void addUser(final BotUser user, final Container container) {
        final Object itemId = container.addItem();
        userMap.put(itemId, user);
        final Item item = container.getItem(itemId);
        item.getItemProperty("mainNick").setValue(user.getMainNick());
        item.getItemProperty("nicks").setValue(createNickList(user));
        item.getItemProperty("isAdmin").setValue(user.isAdmin());
        item.getItemProperty("isOwner").setValue(user.isOwner());
        item.getItemProperty("timeZone").setValue(user.getTimeZone().replace("+", ""));
        item.getItemProperty("dst").setValue(user.getDst() == 1);
        item.getItemProperty("country").setValue(user.getCountry());
        item.getItemProperty("realName").setValue(user.getRealName());
        item.getItemProperty("email").setValue(user.getEmail());
        item.getItemProperty("sms").setValue(user.getSms());
        item.getItemProperty("smsWorks").setValue(user.isSmsConfirmed());
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
                    BotUser remove = userMap.get(itemId);
                    if (remove.getId() != null) {
                        provinceDAOProvider.get().removeProvinceForUser(remove);
                        botUserDAOProvider.get().delete(remove);
                        userMap.remove(itemId);
                        container.removeItem(itemId);
                    }
                } catch (final Exception e) {
                    getWindow().showNotification(e.getMessage(), Window.Notification.TYPE_ERROR_MESSAGE);
                }
            }
        }));
    }

    private static VerticalLayout createNickList(final BotUser user) {
        VerticalLayout nicks = new VerticalLayout();
        for (Nickname nickname : user.getNickList()) {
            nicks.addComponent(new Label(nickname.getName()));
        }
        return nicks;
    }

    private class EditPopupContent extends VerticalLayout {
        private final TextField mainNickField;
        private final CheckBox isAdminCheckBox;
        private final CheckBox isOwnerCheckBox;
        private final Select timeZoneSelect;
        private final CheckBox dstField;
        private final TextField countryField;
        private final TextField realNameField;
        private final TextField emailField;
        private final TextField smsField;
        private final CheckBox smsWorksCheckBox;
        private final Button saveButton;
        private final Button cancelButton;

        private EditPopupContent(final Object itemId) {
            setSizeUndefined();
            setSpacing(true);
            setMargin(true);
            setWidth("100%");

            BotUser user = null;
            if (itemId != null) {
                user = userMap.get(itemId);
            }

            mainNickField = new TextField("Main nick");
            mainNickField.setRequired(true);
            mainNickField.addValidator(new RegexpValidator(ValidationType.NICKNAME.getPattern(), "Invalid nick format"));
            if (user != null) {
                mainNickField.setValue(user.getMainNick());
                mainNickField.setEnabled(false);
            }

            isAdminCheckBox = new CheckBox("Is Admin");
            if (user != null) isAdminCheckBox.setValue(user.isAdmin());

            isOwnerCheckBox = new CheckBox("Is Owner");
            if (user != null) isOwnerCheckBox.setValue(user.isOwner());

            timeZoneSelect = new Select("Time zone in GMT", createTimeZones());
            timeZoneSelect.setRequired(true);
            timeZoneSelect.setNullSelectionAllowed(false);
            timeZoneSelect
                    .addValidator(new RegexpValidator(UtopiaValidationType.TIMEZONE.getPatternString(), "Valid formats: 1 or -10 etc."));
            if (user != null) timeZoneSelect.setValue(user.getTimeZone().replace("+", ""));

            dstField = new CheckBox("Daylight Savings");
            if (user != null) dstField.setValue(user.getDst() == 1);

            countryField = new TextField("Country");
            countryField.setNullRepresentation("");
            if (user != null) countryField.setValue(user.getCountry());

            realNameField = new TextField("Real name");
            realNameField.setNullRepresentation("");
            if (user != null) realNameField.setValue(user.getRealName());

            emailField = new TextField("Email");
            emailField.setWidth("100%");
            emailField.setNullRepresentation("");
            emailField.addValidator(new EmailValidator("Not a valid email address"));
            if (user != null) emailField.setValue(user.getEmail());

            smsField = new TextField("SMS email");
            smsField.setWidth("100%");
            smsField.setNullRepresentation("");
            smsField.addValidator(new EmailValidator("Not a valid email address"));
            if (user != null) smsField.setValue(user.getSms());

            smsWorksCheckBox = new CheckBox("SMS address works");
            if (user != null) smsWorksCheckBox.setValue(user.isSmsConfirmed());

            saveButton = new Button("Save", new Button.ClickListener() {
                @Override
                public void buttonClick(final Button.ClickEvent event) {
                    try {
                        String mainNick = validate(mainNickField, String.class);
                        boolean isAdmin = isAdminCheckBox.booleanValue();
                        boolean isOwner = isOwnerCheckBox.booleanValue();
                        String timeZone = validate(timeZoneSelect, String.class);
                        if (!timeZone.startsWith("-")) timeZone = '+' + timeZone;
                        int dst = dstField.booleanValue() ? 1 : 0;
                        String country = validate(countryField, String.class);
                        String realName = validate(realNameField, String.class);
                        String email = validate(emailField, String.class);
                        String sms = validate(smsField, String.class);
                        boolean smsWorks = smsWorksCheckBox.booleanValue();

                        BotUser botUser;
                        if (itemId == null) {
                            botUser = new BotUser(mainNick, isAdmin, isOwner, timeZone, dst, country, realName, email, sms,
                                    smsWorks);
                            botUser.setPassword("password");
                            botUserDAOProvider.get().save(botUser);
                            addUser(botUser, userTable.getContainerDataSource());
                            userActivitiesDAOProvider.get().save(new UserActivities(botUser));
                        } else {
                            botUser = userMap.get(itemId);
                            botUser.setAdmin(isAdmin);
                            botUser.setOwner(isOwner);
                            botUser.setTimeZone(timeZone);
                            botUser.setDst(dst);
                            botUser.setCountry(country);
                            botUser.setRealName(realName);
                            botUser.setEmail(email);
                            botUser.setSms(sms);
                            botUser.setSmsConfirmed(smsWorks);
                            botUserDAOProvider.get().save(botUser);

                            Container container = userTable.getContainerDataSource();
                            Item item = container.getItem(itemId);
                            item.getItemProperty("mainNick").setValue(mainNick);
                            item.getItemProperty("isAdmin").setValue(isAdmin);
                            item.getItemProperty("isOwner").setValue(isOwner);
                            item.getItemProperty("timeZone").setValue(timeZone);
                            item.getItemProperty("dst").setValue(dst == 1);
                            item.getItemProperty("country").setValue(country);
                            item.getItemProperty("realName").setValue(realName);
                            item.getItemProperty("email").setValue(email);
                            item.getItemProperty("sms").setValue(sms);
                            item.getItemProperty("smsWorks").setValue(smsWorks);
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

            addComponent(mainNickField);
            addComponent(isAdminCheckBox);
            addComponent(isOwnerCheckBox);
            addComponent(timeZoneSelect);
            addComponent(dstField);
            addComponent(countryField);
            addComponent(realNameField);
            addComponent(emailField);
            addComponent(smsField);
            addComponent(smsWorksCheckBox);
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

    private static Collection<String> createTimeZones() {
        List<String> out = new ArrayList<>(30);
        for (int i = -14; i < 13; i++) {
            out.add(String.valueOf(i));
        }
        return out;
    }
}
