package database.updates.mysql;

import api.database.updates.DatabaseUpdateAction;
import api.database.updates.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class MySQLUpdateV5ToV6 extends ApiMySQLDatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 6;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(new SimpleUpdateAction("ALTER TABLE race ADD dragon_immune BOOLEAN NOT NULL DEFAULT false"),
                new SimpleUpdateAction("ALTER TABLE personality ADD dragon_immune BOOLEAN NOT NULL DEFAULT false"));
    }
}
