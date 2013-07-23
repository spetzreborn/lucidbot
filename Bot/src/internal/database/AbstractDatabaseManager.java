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
import api.tools.common.CleanupUtil;
import internal.main.Main;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.*;
import java.util.Set;

@ParametersAreNonnullByDefault
@Log4j
abstract class AbstractDatabaseManager implements DatabaseManager {
    private final Configuration configuration;
    private final Set<DatabaseUpdater> updaters;

    protected AbstractDatabaseManager(final Configuration configuration, final Set<DatabaseUpdater> updaters) {
        this.configuration = configuration;
        this.updaters = updaters;
    }

    @Override
    public Session getSession() {
        return getSessionFactory().getCurrentSession();
    }

    @Override
    public DatabaseState getDatabaseState() {
        Session session = getSession();
        session.beginTransaction();
        try {
            DatabaseState databaseState = session.doReturningWork(new ReturningWork<DatabaseState>() {
                @Override
                public DatabaseState execute(final Connection connection) throws SQLException {
                    try {
                        int dbVersion = getDBVersion(connection);
                        if (dbVersion == -1) {
                            return DatabaseState.CONNECTED_UNINSTALLED_DB;
                        } else if (dbVersion < Main.DB_VERSION) {
                            return DatabaseState.CONNECTED_OUTDATED_DB;
                        }

                        SchemaValidator validator = new SchemaValidator(configuration);
                        validator.validate();
                        return DatabaseState.CONNECTED_FULLY;
                    } catch (final Exception e) {
                        log.error("", e);
                    }
                    return DatabaseState.CONNECTED_INCOMPLETE_DB;
                }
            });
            session.getTransaction().commit();
            return databaseState;
        } catch (HibernateException e) {
            session.getTransaction().rollback();
            throw new DBException(e);
        }
    }

    private int getDBVersion(final Connection connection) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT db_version FROM version");
             ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt("db_version");
            }
        } catch (final SQLException e) {
            log.error("", e);
        }
        return -1;
    }

    @Override
    public void createSchema() {
        Session session = getSession();
        session.beginTransaction();
        try {
            getSession().doWork(new Work() {
                @Override
                public void execute(final Connection connection) throws SQLException {
                    Statement statement = null;
                    PreparedStatement ps = null;
                    try {
                        statement = connection.createStatement();
                        statement.execute("DROP TABLE IF EXISTS version");
                        statement.execute("CREATE TABLE version(db_version BIGINT NOT NULL DEFAULT 1, CONSTRAINT pk_version PRIMARY KEY (db_version))");
                        ps = connection.prepareStatement("INSERT INTO version VALUES(?)");
                        ps.setLong(1, Main.DB_VERSION);
                        ps.executeUpdate();
                    } catch (final SQLException e) {
                        AbstractDatabaseManager.log.error("", e);
                    } finally {
                        CleanupUtil.closeSilently(statement);
                        CleanupUtil.closeSilently(ps);
                    }
                }
            });
            session.getTransaction().commit();
        } catch (HibernateException e) {
            session.getTransaction().rollback();
            throw new DBException(e);
        }

        SchemaExport schemaExport = new SchemaExport(configuration);
        schemaExport.create(true, true);
    }

    @Override
    public void updateSchema() {
        //Do scripted updates
        Session session = getSession();
        session.beginTransaction();
        try {
            getSession().doWork(new Work() {
                @Override
                public void execute(final Connection connection) throws SQLException {
                    int dbVersion = getDBVersion(connection);

                    try {
                        for (DatabaseUpdater updater : updaters) {
                            if (getDatabaseType().equals(updater.getDatabaseType()) && updater.updatesToVersion() > dbVersion) {
                                for (DatabaseUpdateAction updateAction : updater.getUpdateActions()) {
                                    updateAction.runDatabaseAction(connection);
                                }
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException("Failed to run database updates", e);
                    }
                }
            });
            session.getTransaction().commit();
        } catch (HibernateException e) {
            session.getTransaction().rollback();
            throw new DBException(e);
        }

        //Do whatever updates Hibernate can handle by itself
        SchemaUpdate schemaUpdate = new SchemaUpdate(configuration);
        schemaUpdate.execute(true, true);

        //Set the new version
        try {
            executeSQL("UPDATE version SET db_version = " + Main.DB_VERSION);
        } catch (SQLException e) {
            AbstractDatabaseManager.log.error("Could not set the new db_version to " + Main.DB_VERSION + ", please do it manually", e);
        }
    }

    private void executeSQL(final String... queries) throws SQLException {
        Session session = getSession();
        session.beginTransaction();
        try {
            session.doWork(new Work() {
                @Override
                public void execute(final Connection connection) throws SQLException {
                    Statement statement = null;
                    try {
                        statement = connection.createStatement();
                        for (String query : queries) {
                            statement.addBatch(query);
                        }
                        statement.executeBatch();
                        connection.commit();
                    } finally {
                        CleanupUtil.closeSilently(statement);
                    }
                }
            });
            session.getTransaction().commit();
        } catch (HibernateException e) {
            session.getTransaction().rollback();
            throw new DBException(e);
        }
    }

    @Override
    public void repairSchema() {
        SchemaUpdate schemaUpdate = new SchemaUpdate(configuration);
        schemaUpdate.execute(true, true);
    }

    protected abstract Class<?> getDatabaseType();
}
