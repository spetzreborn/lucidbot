package database.updates.h2;

import api.database.updates.DatabaseUpdateAction;
import api.database.updates.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class H2UpdateV8ToV9 extends ApiH2DatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 9;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(
                new SimpleUpdateAction("ALTER TABLE som ADD armies_out_when_posted INT DEFAULT 0 NOT NULL"),
                new SimpleUpdateAction("ALTER TABLE build ADD land INT DEFAULT 0 NOT NULL")
        );
    }
}
