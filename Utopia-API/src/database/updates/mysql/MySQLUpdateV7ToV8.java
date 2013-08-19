package database.updates.mysql;

import api.database.DatabaseUpdateAction;
import api.database.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class MySQLUpdateV7ToV8 extends ApiMySQLDatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 8;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.5 WHERE name = 'Elf WPA'"),
                new SimpleUpdateAction("DELETE race_bonus FROM race_bonus INNER JOIN race ON race_bonus.race_id = race.id INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Faery' AND (bonus.type = 'WPA' OR bonus.type = 'TPA')"),
                new SimpleUpdateAction("REPLACE INTO bonus (name, type, applicability, is_increasing, bonus_value) VALUES('Faery WPA', 'WPA', 'BOTH', true, 0.1)"),
                new SimpleUpdateAction("REPLACE INTO race_bonus (race_id, bonus_id) SELECT race.id,bonus.id FROM race,bonus WHERE race.name = 'Faery' AND bonus.name = 'Faery WPA'"),
                new SimpleUpdateAction("REPLACE INTO bonus (name, type, applicability, is_increasing, bonus_value) VALUES('Faery TPA', 'TPA', 'BOTH', true, 0.1)"),
                new SimpleUpdateAction("REPLACE INTO race_bonus (race_id, bonus_id) SELECT race.id,bonus.id FROM race,bonus WHERE race.name = 'Faery' AND bonus.name = 'Faery TPA'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.5 WHERE name = 'Halfling TPA'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.3 WHERE name = 'Orc Gains'")
        );
    }
}