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

package internal.database;

import api.database.*;
import api.database.models.*;
import api.settings.PropertiesCollection;
import api.settings.PropertiesConfig;
import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import internal.database.updates.DatabaseUpdateModule;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

import static api.tools.text.StringUtil.isNullOrEmpty;

public class DatabaseModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DatabaseManager.class).annotatedWith(MySQL.class).to(MySQLDatabaseManager.class).in(Singleton.class);
        bind(DatabaseManager.class).annotatedWith(H2.class).to(H2DatabaseManager.class).in(Singleton.class);

        bind(DatabaseManager.class).toProvider(DatabaseManagerProvider.class);

        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Transactional.class),
                new TransactionInterceptor(getProvider(TransactionManager.class)));
        requestStaticInjection(Transactions.class);

        Multibinder<HibernateMapped> binder = Multibinder.newSetBinder(binder(), HibernateMapped.class);
        binder.addBinding().toInstance(new HibernateMapped(Alias.class));
        binder.addBinding().toInstance(new HibernateMapped(BotInstanceSettings.class));
        binder.addBinding().toInstance(new HibernateMapped(BotInstanceSettingsChannel.class));
        binder.addBinding().toInstance(new HibernateMapped(BotUser.class));
        binder.addBinding().toInstance(new HibernateMapped(Channel.class));
        binder.addBinding().toInstance(new HibernateMapped(CommandDefinition.class));
        binder.addBinding().toInstance(new HibernateMapped(ContactInformation.class));
        binder.addBinding().toInstance(new HibernateMapped(Nickname.class));
        binder.addBinding().toInstance(new HibernateMapped(UserStatistic.class));

        install(new DatabaseUpdateModule());
    }

    private static class DatabaseManagerProvider implements Provider<DatabaseManager> {
        private final Injector injector;
        private final PropertiesCollection properties;

        @Inject
        DatabaseManagerProvider(final Injector injector, final PropertiesCollection properties) {
            this.properties = properties;
            this.injector = injector;
        }

        @Override
        public DatabaseManager get() {
            if (isNullOrEmpty(properties.get(PropertiesConfig.DB_HOST))) return new NoDatabaseManager();
            return "embedded".equalsIgnoreCase(properties.get(PropertiesConfig.DB_HOST)) ? injector
                    .getInstance(Key.get(DatabaseManager.class, H2.class)) : injector
                    .getInstance(Key.get(DatabaseManager.class, MySQL.class));
        }
    }

    @Provides
    SessionFactory sessionFactoryProvider(final DatabaseManager databaseManager) {
        return databaseManager.getSessionFactory();
    }

    @Provides
    Session sessionProvider(final SessionFactory sessionFactory) {
        return sessionFactory.getCurrentSession();
    }

    @Provides
    Configuration configurationProvider(final Set<HibernateMapped> mappedClasses) {
        Configuration configuration = new Configuration();
        for (HibernateMapped mappedClassContainer : mappedClasses) {
            configuration.addAnnotatedClass(mappedClassContainer.getClazz());
        }
        return configuration;
    }
}
