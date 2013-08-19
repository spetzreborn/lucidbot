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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Objects.firstNonNull;

@ParametersAreNonnullByDefault
@Log4j
abstract class AbstractDatabaseManager implements DatabaseManager {
    private final Configuration configuration;
    private final Set<DatabaseUpdater> updaters;
    private final Map<String, Integer> latestArtifactVersions = new HashMap<>();

    protected AbstractDatabaseManager(final Configuration configuration, final Set<DatabaseUpdater> updaters) {
        this.configuration = configuration;
        this.updaters = updaters;
        for (DatabaseUpdater updater : updaters) {//assumed to be ordered by version already
            latestArtifactVersions.put(updater.forArtifact(), updater.updatesToVersion());
        }
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
                        if (databaseIsEmpty(connection)) {
                            return DatabaseState.CONNECTED_UNINSTALLED_DB;
                        }

                        //Transition period thing to make sure the new versions table is created
                        ensureVersionsTableExists(connection, 9);

                        for (Map.Entry<String, Integer> artifactVersion : latestArtifactVersions.entrySet()) {
                            int dbVersion = getDBVersion(artifactVersion.getKey(), connection);
                            if (dbVersion < artifactVersion.getValue()) {
                                return DatabaseState.CONNECTED_OUTDATED_DB;
                            }
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

    private boolean databaseIsEmpty(final Connection connection) {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SHOW TABLES");
             ResultSet resultSet = preparedStatement.executeQuery()) {
            return !resultSet.next();
        } catch (final SQLException e) {
            log.error("", e);
        }
        return true;
    }

    private void ensureVersionsTableExists(final Connection connection, final Integer initialVersion) {
        PreparedStatement transition = null;
        try (PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
                "versions(artifact VARCHAR NOT NULL, db_version BIGINT NOT NULL DEFAULT 1, CONSTRAINT pk_versions PRIMARY KEY (artifact))")) {

            statement.execute();

            //The below is intended to be used only during a transition period between the old version and the new versions table
            transition = connection.prepareStatement("INSERT INTO versions SELECT ?, ? FROM dual " +
                    "WHERE NOT EXISTS (SELECT * FROM versions where artifact = ?)");
            for (Map.Entry<String, Integer> artifactVersion : latestArtifactVersions.entrySet()) {
                String artifact = artifactVersion.getKey();
                transition.setString(1, artifact);
                transition.setInt(2, firstNonNull(initialVersion, artifactVersion.getValue()));
                transition.setString(3, artifact);
                transition.addBatch();
            }
            transition.executeBatch();
        } catch (final SQLException e) {
            AbstractDatabaseManager.log.error("", e);
        } finally {
            CleanupUtil.closeSilently(transition);
        }
    }

    private int getDBVersion(final String artifact, final Connection connection) {
        ResultSet resultSet = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT db_version FROM versions WHERE artifact = ?")) {
            preparedStatement.setString(1, artifact);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("db_version");
            }
        } catch (final SQLException e) {
            log.error("", e);
        } finally {
            CleanupUtil.closeSilently(resultSet);
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
                    ensureVersionsTableExists(connection, null);
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
                    try {
                        Map<String, Integer> artifactVersionsBeforeUpdate = new HashMap<>();
                        for (String artifact : latestArtifactVersions.keySet()) {
                            int version = getDBVersion(artifact, connection);
                            artifactVersionsBeforeUpdate.put(artifact, version);
                        }

                        for (DatabaseUpdater updater : updaters) {
                            if (getDatabaseType().equals(updater.getDatabaseType()) &&
                                    updater.updatesToVersion() > artifactVersionsBeforeUpdate.get(updater.forArtifact())) {
                                for (DatabaseUpdateAction updateAction : updater.getUpdateActions()) {
                                    updateAction.runDatabaseAction(connection);
                                }
                            }
                        }

                        for (Map.Entry<String, Integer> entry : latestArtifactVersions.entrySet()) {
                            updateDBVersion(entry.getKey(), entry.getValue(), connection);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException("Failed to run database updates", e);
                    }
                }
            });
            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
            throw new DBException(e);
        }

        //Do whatever updates Hibernate can handle by itself
        SchemaUpdate schemaUpdate = new SchemaUpdate(configuration);
        schemaUpdate.execute(true, true);
    }

    private void updateDBVersion(final String artifact, final long version, final Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE versions SET db_version = ? WHERE artifact = ?")) {
            statement.setLong(1, version);
            statement.setString(2, artifact);
        } catch (SQLException e) {
            AbstractDatabaseManager.log.error("Could not set the new db_version to " + version + " for '" + artifact + "', please do it manually", e);
        }
    }

    @Override
    public void repairSchema() {
        SchemaUpdate schemaUpdate = new SchemaUpdate(configuration);
        schemaUpdate.execute(true, true);
    }

    protected abstract Class<?> getDatabaseType();
}
