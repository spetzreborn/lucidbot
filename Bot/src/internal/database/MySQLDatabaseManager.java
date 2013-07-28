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

import api.database.DatabaseUpdater;
import api.database.MySQL;
import api.settings.PropertiesCollection;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import javax.inject.Inject;
import java.util.Set;

import static api.settings.PropertiesConfig.*;

/**
 * A Manager for MySQL
 */
final class MySQLDatabaseManager extends AbstractDatabaseManager {
    private final SessionFactory sessionFactory;

    @Inject
    MySQLDatabaseManager(final Configuration configuration, final PropertiesCollection properties, final Set<DatabaseUpdater> updaters) {
        super(configuration, updaters);

        String dbHost = properties.get(DB_HOST);
        String dbName = properties.get(DB_NAME);
        String dbUsername = properties.get(DB_USERNAME);
        String dbPassword = properties.get(DB_PASSWORD);

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Couldn't find database drivers");
        }

        configuration.setProperty("hibernate.current_session_context_class", "thread");
        configuration.setProperty("hibernate.flushMode", "COMMIT");
        configuration.setProperty("hibernate.jdbc_batch_size", String.valueOf(15));
        configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.service.jdbc.connections.internal.C3P0ConnectionProvider");
        configuration.setProperty("hibernate.c3p0.timeout", String.valueOf(60));
        configuration.setProperty("hibernate.connection.username", dbUsername);
        configuration.setProperty("hibernate.connection.password", dbPassword);
        configuration.setProperty("hibernate.connection.useUnicode", "true");
        configuration.setProperty("hibernate.connection.characterEncoding", "UTF-8");
        configuration.setProperty("hibernate.connection.charSet", "UTF-8");
        configuration.setProperty("hibernate.dialect", "internal.database.Mysql5BitBooleanDialect");
        configuration.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        configuration.setProperty("hibernate.connection.url", "jdbc:mysql://" + dbHost + '/' + dbName + "?createDatabaseIfNotExist=true&transformedBitIsBoolean=true");

        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    }

    @Override
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    protected Class<?> getDatabaseType() {
        return MySQL.class;
    }
}
