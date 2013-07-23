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

import api.settings.PropertiesCollection;
import api.tools.time.DateFactory;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.ui.*;
import setup.tools.DatabaseCleaner;
import setup.tools.DefaultsLoader;
import tools.UtopiaPropertiesConfig;

import javax.inject.Inject;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicSettingsPanel extends VerticalLayout {
    private static final Pattern KD_LOC_PATTERN = Pattern.compile("\\((\\d{1,2}):(\\d{1,2})\\)");
    private static final DateFormat AGE_START_FORMAT = DateFactory.getISOWithoutSecondsDateTimeFormat();
    private static final String ISLAND = "island";
    private static final String KINGDOM = "kingdom";
    private static final String AGE_START = "ageStart";

    static {
        AGE_START_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final Basics basics = new Basics();

    @Inject
    public BasicSettingsPanel(final PropertiesCollection properties, final DefaultsLoader defaultsLoader, final DatabaseCleaner cleaner) {
        Label description = new Label("Here you may set some basic settings for the bot. These settings are what's referred to as " +
                "properties and may also be edited in the .properties files in the bot folder. Note that the " +
                "bot will not reload the file contents automatically (at this time), so if you do edit one of " +
                "the files manually, make sure to restart the bot for them to take effect.");

        Date ageStart = properties.getDate(UtopiaPropertiesConfig.AGE_START);
        basics.setAgeStart(ageStart);
        Matcher matcher = KD_LOC_PATTERN.matcher(properties.get(UtopiaPropertiesConfig.INTRA_KD_LOC));
        if (!matcher.matches()) throw new IllegalStateException("The kingdom location in the properties file is in the wrong format");
        basics.setIsland(matcher.group(1));
        basics.setKingdom(matcher.group(2));
        BeanItem<Basics> basicsItem = new BeanItem<>(basics);

        final Form form = new ComplexForm();
        form.setVisibleItemProperties(new Object[]{ISLAND, KINGDOM, AGE_START});
        form.setItemDataSource(basicsItem);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);
        Button apply = new Button("Apply");
        apply.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent clickEvent) {
                try {
                    form.commit();
                    properties.change(basics.toMap());
                } catch (final Exception ignore) {
                }
            }
        });
        Button reset = new Button("Reset");
        reset.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent clickEvent) {
                form.discard();
            }
        });
        buttons.addComponent(apply);
        buttons.addComponent(reset);

        Label loadDefaultsDescription = new Label(
                "Click the button below if you want to load ALL the defaults for races, spells, ops etc. " +
                        "The other option is to do it manually on their specific settings pages (click the tabs above)");
        Button loadDefaults = new Button("Load defaults");
        loadDefaults.setSizeUndefined();
        loadDefaults.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent clickEvent) {
                defaultsLoader.loadAllDefaults();
                getApplication().getMainWindow().showNotification("Defaults loaded!", Window.Notification.TYPE_HUMANIZED_MESSAGE);
            }
        });

        Label resetAgeDescription = new Label("Click the button below if you want to clear all the age specific information from the bot." +
                " This includes provinces, kingdoms, intel, user statistics, spells, ops etc. Basically " +
                "everything that changes each age. The next step after this would be to go through " +
                "the races and personalities and such and update any bonuses that were changed for the " +
                "new age.");
        Button resetAge = new Button("Reset for a new age");
        resetAge.setSizeUndefined();
        resetAge.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(final Button.ClickEvent clickEvent) {
                cleaner.clean();
                getApplication().getMainWindow()
                        .showNotification("The database was reset successfully!", Window.Notification.TYPE_HUMANIZED_MESSAGE);
            }
        });

        addComponent(description);
        addComponent(form);
        addComponent(buttons);
        addComponent(loadDefaultsDescription);
        addComponent(loadDefaults);
        addComponent(resetAgeDescription);
        addComponent(resetAge);
        setWidth("600px");
        setHeight("100%");
        setSpacing(true);
        setMargin(true);
    }

    public static class ComplexForm extends Form {
        private final HorizontalLayout kdAndIslandLayout = new HorizontalLayout();
        private final HorizontalLayout ageStartLayout = new HorizontalLayout();

        public ComplexForm() {
            setCaption("Basic settings");
            setWriteThrough(false);
            setInvalidCommitted(false);

            HorizontalLayout kdLayout = new HorizontalLayout();
            kdLayout.setSpacing(true);
            kdLayout.addComponent(new Label("Our Kingdom"));
            kdLayout.addComponent(kdAndIslandLayout);
            kdAndIslandLayout.setSpacing(false);

            ageStartLayout.setSpacing(true);
            ageStartLayout.addComponent(new Label("Age Start (in GMT)"));

            VerticalLayout base = new VerticalLayout();
            base.setSpacing(true);
            base.setMargin(true, false, false, false);
            base.addComponent(kdLayout);
            base.addComponent(ageStartLayout);
            setLayout(base);
            setFormFieldFactory(new BasicsFieldFactory());
        }

        @Override
        protected void attachField(final Object propertyId, final Field field) {
            switch (propertyId.toString()) {
                case AGE_START:
                    ageStartLayout.addComponent(field);
                    break;
                case ISLAND:
                    kdAndIslandLayout.addComponent(new Label("("));
                    kdAndIslandLayout.addComponent(field);
                    kdAndIslandLayout.addComponent(new Label(":"));
                    break;
                case KINGDOM:
                    kdAndIslandLayout.addComponent(field);
                    kdAndIslandLayout.addComponent(new Label(")"));
                    break;
                default:
                    throw new IllegalStateException("Unknown property: " + propertyId);
            }
        }
    }

    public static class Basics implements Serializable {
        private String island = "";
        private String kingdom = "";
        private Date ageStart;

        public String getIsland() {
            return island;
        }

        public void setIsland(final String island) {
            this.island = island;
        }

        public String getKingdom() {
            return kingdom;
        }

        public void setKingdom(final String kingdom) {
            this.kingdom = kingdom;
        }

        public Date getAgeStart() {
            return ageStart;
        }

        public void setAgeStart(final Date ageStart) {
            this.ageStart = ageStart;
        }

        private Map<String, String> toMap() {
            Map<String, String> out = new HashMap<>();
            out.put(UtopiaPropertiesConfig.INTRA_KD_LOC, '(' + island + ':' + kingdom + ')');
            out.put(UtopiaPropertiesConfig.AGE_START, AGE_START_FORMAT.format(ageStart));
            return out;
        }
    }

    @SuppressWarnings("deprecation")
    private static class BasicsFieldFactory extends DefaultFieldFactory {
        @Override
        public Field createField(final Item item, final Object propertyId, final Component uiContext) {
            Field field;

            switch (propertyId.toString()) {
                case AGE_START:
                    field = new PopupDateField();
                    ((DateField) field).setDateFormat(DateFactory.getISOWithoutSecondsDateTimeFormatAsString());
                    field.setSizeUndefined();
                    ((DateField) field).setTimeZone(TimeZone.getTimeZone("GMT"));
                    ((DateField) field).setResolution(DateField.RESOLUTION_MIN);
                    break;
                case ISLAND:
                case KINGDOM:
                    field = new TextField();
                    ((TextField) field).setNullRepresentation("");
                    field.addValidator(new RegexpValidator("\\d{1,2}", "Must be a positive integer"));
                    field.setInvalidAllowed(false);
                    field.setWidth("25px");
                    field.setHeight("20px");
                    ((TextField) field).setRows(0);
                    break;
                default:
                    throw new IllegalStateException("Unknown property: " + propertyId);
            }

            return field;
        }
    }
}
