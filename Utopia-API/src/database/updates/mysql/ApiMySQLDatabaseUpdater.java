package database.updates.mysql;

import api.database.updates.AbstractMySQLDatabaseUpdater;

public abstract class ApiMySQLDatabaseUpdater extends AbstractMySQLDatabaseUpdater {

    @Override
    public String forArtifact() {
        return "Utopia-API";
    }

}
