package database.updates.h2;

import api.database.updates.DatabaseUpdateAction;
import api.database.updates.SimpleUpdateAction;
import com.google.common.collect.Lists;

public class H2UpdateV9ToV10 extends ApiH2DatabaseUpdater {
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

                new SimpleUpdateAction("DELETE FROM race_bonus WHERE EXISTS(SELECT * FROM race INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Dwarf' AND bonus.type = 'GAIN' AND race_bonus.race_id = race.id)"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Dwarf Gains'"),
                new SimpleUpdateAction("INSERT INTO bonus (name, type, applicability, is_increasing, bonus_value) VALUES(" +
                        "'Dwarf Gains', 'GAIN', 'OFFENSIVELY', false, 0.1)"),
                new SimpleUpdateAction("INSERT INTO race_bonus (race_id, bonus_id) SELECT race.id,bonus.id FROM race INNER JOIN bonus WHERE race.name = 'Dwarf' AND bonus.name = 'Dwarf Gain'"),
                new SimpleUpdateAction("UPDATE bonus SET bonus_value = 0.3 WHERE name = 'Elf WPA'"),
                new SimpleUpdateAction("DELETE FROM race_bonus WHERE EXISTS(SELECT * FROM race INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Faery' AND (bonus.type = 'WPA' OR bonus.type = 'TPA' OR bonus.type = 'HONOR') AND race_bonus.race_id = race.id)"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Faery WPA' OR name = 'Faery TPA' OR name = 'Faery Honor Bonus'"),
                new SimpleUpdateAction("DELETE FROM race_bonus WHERE EXISTS(SELECT * FROM race INNER JOIN bonus ON race_bonus.bonus_id = bonus.id WHERE " +
                        "race.name = 'Halfling' AND bonus.type = 'GAIN' AND race_bonus.race_id = race.id)"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'Halfling Gains'"),

                new SimpleUpdateAction("DELETE FROM personality_bonus WHERE EXISTS(SELECT * FROM personality INNER JOIN bonus ON personality_bonus.bonus_id = bonus.id WHERE " +
                        "personality.name = 'War Hero' AND bonus.type = 'HONOR' AND personality_bonus.personality_id = personality.id)"),
                new SimpleUpdateAction("DELETE FROM bonus WHERE name = 'War Hero Honor Bonus'"),
                new SimpleUpdateAction("UPDATE personality SET intel_accuracy = 'ALWAYS' WHERE name = 'Tactician'")
        );
    }
}
