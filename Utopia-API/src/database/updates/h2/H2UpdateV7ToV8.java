package database.updates.h2;

import api.database.updates.DatabaseUpdateAction;
import api.database.updates.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class H2UpdateV7ToV8 extends ApiH2DatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 8;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.5 WHERE name = 'Elf WPA'"),
                new SimpleUpdateAction("DELETE FROM race_bonus WHERE EXISTS(SELECT * FROM race INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Faery' AND (bonus.type = 'WPA' OR bonus.type = 'TPA') AND race_bonus.race_id = race.id)"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Faery WPA'"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Faery TPA'"),
                new SimpleUpdateAction("INSERT INTO bonus (name, type, applicability, is_increasing, bonus_value) VALUES(" +
                        "'Faery WPA', 'WPA', 'BOTH', true, 0.1)"),
                new SimpleUpdateAction("INSERT INTO bonus (name, type, applicability, is_increasing, bonus_value) VALUES(" +
                        "'Faery TPA', 'TPA', 'BOTH', true, 0.1)"),
                new SimpleUpdateAction("INSERT INTO race_bonus (race_id, bonus_id) SELECT race.id,bonus.id FROM race INNER JOIN bonus WHERE race.name = 'Faery' AND bonus.name = 'Faery WPA'"),
                new SimpleUpdateAction("INSERT INTO race_bonus (race_id, bonus_id) SELECT race.id,bonus.id FROM race INNER JOIN bonus WHERE race.name = 'Faery' AND bonus.name = 'Faery TPA'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.5 WHERE name = 'Halfling TPA'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.3 WHERE name = 'Orc Gains'")
        );
    }
}
