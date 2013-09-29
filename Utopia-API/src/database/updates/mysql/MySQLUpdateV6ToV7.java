package database.updates.mysql;

import api.database.updates.DatabaseUpdateAction;
import api.database.updates.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class MySQLUpdateV6ToV7 extends ApiMySQLDatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 7;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(
                new SimpleUpdateAction("DELETE race_bonus FROM race_bonus INNER JOIN race ON race_bonus.race_id = race.id INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Avian' AND bonus.name = 'Avian Gains'"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Avian Gains'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.3 WHERE name = 'Elf WPA'"),
                new SimpleUpdateAction("DELETE race_bonus FROM race_bonus INNER JOIN race ON race_bonus.race_id = race.id INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Faery' AND (bonus.name = 'Faery WPA' OR bonus.name = 'Faery TPA')"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Faery WPA'"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Faery TPA'"),
                new SimpleUpdateAction("UPDATE race SET elite_networth = 5.5, elite_off_strength = 5 WHERE name = 'Halfling'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.4 WHERE name = 'Halfling TPA'"),
                new SimpleUpdateAction("REPLACE INTO bonus (name, type, applicability, is_increasing, bonus_value) VALUES('Halfling Gains', 'GAIN', 'OFFENSIVELY', false, 0.15)"),
                new SimpleUpdateAction("REPLACE INTO race_bonus (race_id, bonus_id) SELECT race.id,bonus.id FROM race,bonus WHERE race.name = 'Halfling' AND bonus.name = 'Halfling Gains'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.25 WHERE name = 'Orc Gains'")
        );
    }
}
