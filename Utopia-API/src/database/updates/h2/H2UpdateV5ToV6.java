package database.updates.h2;

import api.database.AbstractH2DatabaseUpdater;
import api.database.DatabaseUpdateAction;
import api.database.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class H2UpdateV5ToV6 extends AbstractH2DatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 6;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(new SimpleUpdateAction("ALTER TABLE race ADD COLUMN dragon_immune BOOLEAN NOT NULL DEFAULT false"),
                new SimpleUpdateAction("ALTER TABLE personality ADD COLUMN dragon_immune BOOLEAN NOT NULL DEFAULT false"));
    }
}
