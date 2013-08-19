package database.updates.mysql;

import api.database.DatabaseUpdateAction;
import api.database.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class MySQLUpdateV8ToV9 extends ApiMySQLDatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 9;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(
                new SimpleUpdateAction("ALTER TABLE som ADD armies_out_when_posted INT NOT NULL DEFAULT 0"),
                new SimpleUpdateAction("ALTER TABLE build ADD land INT NOT NULL DEFAULT 0")
        );
    }
}
