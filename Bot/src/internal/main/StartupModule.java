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

package internal.main;

import api.runtime.ThreadingManager;
import api.settings.PluginServiceLoader;
import api.settings.PropertiesCollection;
import api.settings.PropertiesConfig;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import internal.commands.CommandsModule;
import internal.events.EventsModule;
import internal.filters.FilterModule;
import internal.irc.IRCModule;
import internal.settings.Properties;
import internal.templates.TemplatingModule;
import internal.tools.validation.ValidationModule;
import lombok.extern.log4j.Log4j;
import org.apache.log4j.PropertyConfigurator;
import spi.runtime.RequiresShutdown;
import spi.settings.PropertiesSpecification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Log4j
public class StartupModule extends AbstractModule {

    @Override
    protected void configure() {
        bindListener(Matchers.any(), new ShutDownListener());

        ThreadingManager threadingManager = new ThreadingManager(10);
        bind(ThreadingManager.class).toInstance(threadingManager);

        //Read all .properties files from the current folder and load them into Properties objects
        PropertyConfigurator.configureAndWatch("logging.properties");
        List<Properties> propertiesList = new ArrayList<>();
        Map<String, String> defaultProperties = new HashMap<>();
        PropertiesConfig propertiesConfig = new PropertiesConfig();
        Properties botProperties = new Properties(propertiesConfig.getFilePath());
        botProperties.loadFromFile();
        propertiesList.add(botProperties);
        defaultProperties.putAll(propertiesConfig.getDefaults());

        PluginServiceLoader<PropertiesSpecification> propLoader = PluginServiceLoader
                .newLoaderFor(PropertiesSpecification.class, "plugins");
        for (PropertiesSpecification propertiesSpecification : propLoader.getInstances()) {
            Properties properties = new Properties(propertiesSpecification.getFilePath());
            properties.loadFromFile();
            propertiesList.add(properties);
            defaultProperties.putAll(propertiesSpecification.getDefaults());
        }

        //Create the PropertiesCollection and bind it
        PropertiesCollection propertiesCollection = new PropertiesCollection(propertiesList, defaultProperties, threadingManager);
        Names.bindProperties(binder(), propertiesCollection.getAllProperties());
        bind(PropertiesCollection.class).toInstance(propertiesCollection);

        //Install internal modules
        install(new CommandsModule());
        install(new EventsModule());
        install(new FilterModule());
        install(new IRCModule());
        install(new TemplatingModule());
        install(new ValidationModule());
    }

    private static class ShutDownListener implements TypeListener {
        private final List<RequiresShutdown> objects = new CopyOnWriteArrayList<>();

        private ShutDownListener() {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    for (RequiresShutdown object : objects) {
                        object.getShutdownRunner().run();
                    }
                }
            });
        }

        @Override
        public <I> void hear(final TypeLiteral<I> type, final TypeEncounter<I> encounter) {
            if (RequiresShutdown.class.isAssignableFrom(type.getRawType())) {
                encounter.register(new InjectionListener<I>() {
                    @Override
                    public void afterInjection(final I injectee) {
                        objects.add((RequiresShutdown) injectee);
                    }
                });
            }
        }
    }
}
