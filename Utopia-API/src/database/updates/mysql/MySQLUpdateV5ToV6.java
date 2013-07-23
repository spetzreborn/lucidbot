package database.updates.mysql;

import api.database.AbstractMySQLDatabaseUpdater;
import api.database.DatabaseUpdateAction;
import api.database.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class MySQLUpdateV5ToV6 extends AbstractMySQLDatabaseUpdater {
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
