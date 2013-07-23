package intel;

import database.models.Army;
import database.models.Province;
import database.models.SoM;

public class SoMArmyUtil {

    private SoMArmyUtil() {
    }

    public static Army getOrCreateHomeArmy(final SoM som, final Province province) {
        Army armyHome;
        if (som.getArmyHome() == null) {
            armyHome = new Army();
            armyHome.setProvince(province);
            armyHome.setType(Army.ArmyType.ARMY_HOME);
            armyHome.setSom(som);
        } else armyHome = som.getArmyHome();
        return armyHome;
    }

    public static Army getOrCreateTrainingArmy(final SoM som, final Province province) {
        Army armyTraining;
        if (som.getArmyInTraining() == null) {
            armyTraining = new Army();
            armyTraining.setProvince(province);
            armyTraining.setType(Army.ArmyType.ARMY_TRAINING);
            armyTraining.setGenerals(0);
            armyTraining.setSom(som);
        } else armyTraining = som.getArmyInTraining();
        return armyTraining;
    }

    public static Army getOrCreateOutArmy(final SoM som, final int armyNo, final Province province) {
        Army army;
        if (som.getArmyOut(armyNo) == null) {
            army = new Army();
            army.setArmyNumber(armyNo);
            army.setType(Army.ArmyType.ARMY_OUT);
            army.setProvince(province);
            army.setSom(som);
        } else army = som.getArmyOut(armyNo);
        return army;
    }
}
