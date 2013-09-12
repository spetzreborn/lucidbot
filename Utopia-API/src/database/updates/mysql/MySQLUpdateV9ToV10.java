package database.updates.mysql;

import api.database.DatabaseUpdateAction;
import api.database.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class MySQLUpdateV9ToV10 extends ApiMySQLDatabaseUpdater {
    @Override
    public int updatesToVersion() {
        return 10;
    }

    @Override
    public Iterable<? extends DatabaseUpdateAction> getUpdateActions() {
        return Lists.newArrayList(
                new SimpleUpdateAction("UPDATE race SET elite_def_strength = 2, elite_networth = 5 WHERE name = 'Avian'"),
                new SimpleUpdateAction("UPDATE race SET elite_def_strength = 3, elite_networth = 5.25 WHERE name = 'Dwarf'"),
                new SimpleUpdateAction("UPDATE race SET def_spec_strength = 4, elite_networth = 5 WHERE name = 'Elf'"),
                new SimpleUpdateAction("UPDATE race SET elite_def_strength = 5, elite_networth = 5.25 WHERE name = 'Faery'"),
                new SimpleUpdateAction("UPDATE race SET elite_off_strength = 4, elite_def_strength = 4, elite_networth = 4 WHERE name = 'Halfling'"),
                new SimpleUpdateAction("UPDATE race SET off_spec_strength = 4, elite_off_strength = 6, elite_networth = 5.5, elite_sendout_percentage = 100 WHERE name = 'Human'"),

                new SimpleUpdateAction("REPLACE INTO bonus (name, type, applicability, is_increasing, bonus_value) VALUES('Dwarf Gains', 'GAIN', 'OFFENSIVELY', false, 0.1)"),
                new SimpleUpdateAction("REPLACE INTO race_bonus (race_id, bonus_id) SELECT race.id,bonus.id FROM race,bonus WHERE race.name = 'Dwarf' AND bonus.name = 'Dwarf Gains'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.3 WHERE name = 'Elf WPA'"),
                new SimpleUpdateAction("DELETE race_bonus FROM race_bonus INNER JOIN race ON race_bonus.race_id = race.id INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Faery' AND (bonus.type = 'WPA' OR bonus.type = 'TPA' OR bonus.type = 'HONOR')"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Faery TPA' OR name = 'Faery WPA' OR name = 'Faery Honor Bonus'"),
                new SimpleUpdateAction("DELETE race_bonus FROM race_bonus INNER JOIN race ON race_bonus.race_id = race.id INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Halfling' AND bonus.type = 'GAIN'"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Halfling Gains'"),

                new SimpleUpdateAction("DELETE personality_bonus FROM personality_bonus INNER JOIN personality ON personality_bonus.personality_id = personality.id " +
                        "INNER JOIN bonus ON personality_bonus.bonus_id = bonus.id WHERE personality.name = 'War Hero' AND bonus.type = 'HONOR'"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'War Hero Honor Bonus'"),
                new SimpleUpdateAction("UPDATE personality SET intel_accuracy = 'ALWAYS' WHERE name = 'Tactician'")
        );
    }
}
