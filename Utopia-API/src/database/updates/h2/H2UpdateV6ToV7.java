package database.updates.h2;

import api.database.AbstractH2DatabaseUpdater;
import api.database.DatabaseUpdateAction;
import api.database.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class H2UpdateV6ToV7 extends AbstractH2DatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 7;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(
                new SimpleUpdateAction("DELETE FROM race_bonus WHERE EXISTS(SELECT * FROM race INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Avian' AND bonus.name = 'Avian Gains' AND race_bonus.race_id = race.id)"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Avian Gains'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.3 WHERE name = 'Elf WPA'"),
                new SimpleUpdateAction("DELETE FROM race_bonus WHERE EXISTS(SELECT * FROM race INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Faery' AND (bonus.name = 'Faery WPA' OR bonus.name = 'Faery TPA') AND race_bonus.race_id = race.id)"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Faery WPA'"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Faery TPA'"),
                new SimpleUpdateAction("UPDATE race SET elite_networth = 5.5, elite_off_strength = 5 WHERE name = 'Halfling'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.4 WHERE name = 'Halfling TPA'"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Halfling Gains'"),
                new SimpleUpdateAction("INSERT INTO bonus (name, type, applicability, is_increasing, bonus_value) VALUES(" +
                        "'Halfling Gains', 'GAIN', 'OFFENSIVELY', false, 0.15)"),
                new SimpleUpdateAction("DELETE FROM race_bonus WHERE EXISTS(SELECT * FROM race INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Halfling' AND bonus.name = 'Halfling Gains' AND race_bonus.race_id = race.id)"),
                new SimpleUpdateAction("INSERT INTO race_bonus (race_id, bonus_id) SELECT race.id,bonus.id FROM race INNER JOIN bonus WHERE race.name = 'Halfling' AND bonus.name = 'Halfling Gains'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.25 WHERE name = 'Orc Gains'")
        );
    }
}
