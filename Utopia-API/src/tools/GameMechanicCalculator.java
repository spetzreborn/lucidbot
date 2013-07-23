/*
 * Copyright (c) 2012, Fredrik Yttergren
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name LucidBot nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Fredrik Yttergren BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package tools;

import api.tools.numbers.CalculatorUtil;
import api.tools.numbers.NumberUtil;
import api.tools.text.RegexUtil;
import api.tools.text.StringUtil;
import com.google.common.collect.Lists;
import database.CommonEntitiesAccess;
import database.models.AttackType;
import database.models.Building;
import database.models.BuildingFormula;
import database.models.OpType;
import database.models.ScienceType;
import database.models.SpellType;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static api.tools.text.StringUtil.isNotNullOrEmpty;

/**
 * This class is capable of calculating various game mechanic effects
 */
@Log4j
public class GameMechanicCalculator {
    private static final Pattern percentPlaceHolderPattern = Pattern.compile("#percent#");
    private static final Pattern amountPlaceHolderPattern = Pattern.compile("#amount#");
    private static final Pattern bePlaceHolderPattern = Pattern.compile("#be#");
    private static final DecimalFormat decimalFormat = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US));

    static {
        decimalFormat.setRoundingMode(RoundingMode.FLOOR);
    }

    private final CommonEntitiesAccess commonEntitiesAccess;

    @Inject
    public GameMechanicCalculator(final CommonEntitiesAccess commonEntitiesAccess) {
        this.commonEntitiesAccess = commonEntitiesAccess;
    }

    /**
     * Calculates the effects of the specified Building given the various parameters and returns
     * the effects inserted into the result messages specified for the Building.
     * <p/>
     * Percent and amount may be null individually, but not at the same time
     *
     * @param buildingType the name of the Building type
     * @param percent      the amount/land factor for the building, may be null
     * @param amount       the amount factor for the building, may be null
     * @param be           the building efficiency to calculate with. A null value makes this default to 100%
     * @return a List of Strings with the result messages
     * @throws IllegalArgumentException if both percent and amount are null at the same time
     */
    public List<String> getBuildingEffects(String buildingType, String percent, String amount, String be) {
        if (StringUtil.isNullOrEmpty(percent) && StringUtil.isNullOrEmpty(amount))
            throw new IllegalArgumentException("Percent and amount are both null");
        Building building = commonEntitiesAccess.getBuilding(buildingType);
        if (building == null) throw new IllegalArgumentException("The building type does not exist: " + buildingType);

        Map<String, Double> params = new HashMap<>();
        if (percent != null) params.put("percent", NumberUtil.parseDouble(percent));
        if (amount != null) params.put("amount", NumberUtil.parseDouble(amount));
        params.put("be", be == null ? 100 : NumberUtil.parseDouble(be));

        List<String> out = new ArrayList<>();
        for (BuildingFormula buildingFormula : building.getFormulas()) {
            Double result = performBuildingEffectCalculation(buildingFormula, params.get("percent"), params.get("amount"),
                                                             params.get("be"));
            if (result != null) {
                String formatted = formatBuildingEffectsResult(result, buildingFormula);
                out.add(formatted);
            } else return Lists.newArrayList(
                    "Could not calculate because one or more parameters are missing (some require percentage, some amount, some both)");
        }
        return out;
    }

    private static String formatBuildingEffectsResult(Double result, BuildingFormula buildingFormula) {
        String resultText = buildingFormula.getResultText();
        resultText = resultText.replace("?", decimalFormat.format(result));
        return resultText;
    }

    public static Double performBuildingEffectCalculation(BuildingFormula buildingFormula, Double percent, Double amount, double be) {
        String formula = buildingFormula.getFormula();
        if (percent != null) formula = percentPlaceHolderPattern.matcher(formula).replaceAll(String.valueOf(percent));
        if (amount != null) formula = amountPlaceHolderPattern.matcher(formula).replaceAll(String.valueOf(amount));
        formula = bePlaceHolderPattern.matcher(formula).replaceAll(String.valueOf(be));

        Double result = CalculatorUtil.calc(formula, true);
        if (result == null) {
            log.error("Could not calculate building formula: " + formula + " for " + buildingFormula.getBuilding().getName());
            return null;
        }
        if (buildingFormula.getCap() != null) result = Math.min(result, buildingFormula.getCap());
        return result;
    }

    /**
     * Calculates the books per acre based on the science effects
     *
     * @param type    the science type
     * @param percent the effect in %
     * @param bonuses all the bonuses from race/pers/buildings etc. separated by spaces
     * @return the calculated BPA
     * @throws IllegalArgumentException if the science type could not be found
     */
    public int getBpaFromPercent(String type, double percent, String bonuses) {
        ScienceType scienceType = commonEntitiesAccess.getScienceType(type);
        if (scienceType == null) throw new IllegalArgumentException("Science type could not be found: " + type);

        double raw = percent / scienceType.getResultFactor();
        if (isNotNullOrEmpty(bonuses)) {
            for (String bonus : RegexUtil.WHITESPACES_PATTERN.split(bonuses)) {
                raw /= (1 + NumberUtil.parseDouble(bonus.replace("%", "")) / 100.0);
            }
        }
        return (int) Math.ceil(Math.pow(raw, 2));
    }

    /**
     * The percent science effect for the specified science type, based on the bpa and bonuses
     *
     * @param type    the science type
     * @param bpa     the books per acre
     * @param bonuses all the bonuses from race/pers/buildings etc. separated by spaces
     * @return the calculated percent effect
     * @throws IllegalArgumentException if the science type could not be found
     */
    public String getPercentFromBpa(String type, int bpa, String bonuses) {
        ScienceType scienceType = commonEntitiesAccess.getScienceType(type);
        if (scienceType == null) throw new IllegalArgumentException("Science type could not be found: " + type);

        double result = scienceType.getResultFactor() * Math.sqrt(bpa);
        if (isNotNullOrEmpty(bonuses)) {
            for (String bonus : RegexUtil.WHITESPACES_PATTERN.split(bonuses)) {
                result *= 1 + NumberUtil.parseDouble(bonus.replace("%", "")) / 100;
            }
        }
        return decimalFormat.format(result) + '%';
    }

    /**
     * @param type the spell type
     * @return the description of the effects for the specified type of spell
     */
    public String getSpellEffectsDescriptions(String type) {
        SpellType spellType = commonEntitiesAccess.getSpellType(type);
        if (spellType == null) throw new IllegalArgumentException("Spell type could not be found: " + type);
        return spellType.getEffects() == null ? "A description has not been added for this spell" : spellType.getEffects();
    }

    /**
     * @param type the op type
     * @return the description of the effects for the specified type of op
     */
    public String getOpEffectsDescriptions(String type) {
        OpType opType = commonEntitiesAccess.getOpType(type);
        if (opType == null) throw new IllegalArgumentException("Op type could not be found: " + type);
        return opType.getEffects() == null ? "A description has not been added for this op" : opType.getEffects();
    }

    /**
     * Calculates attack gains
     *
     * @param target the size of the target
     * @param self   the size of the attacker
     * @param total  the total of the resource targeted by the attack, typically land. 0 if unspecified
     * @param type   type of attack
     * @return the gain, either in percent (if total was 0) or amount
     */
    public static double calcGains(final double target, final double self, final double total, final AttackType type) {
        double constant = type.getBaseGainFactor() / 0.12;
        double resources = total;
        double gain = 0;
        if (total == 0) {
            resources = 1.0;
        }
        if (target / self < 0.9 && target / self > 0) {
            gain = Math.max(0.0, (0.12 - (0.9 - target / self) * 0.36) * constant * resources);
        } else if (target / self >= 0.9 && target / self <= 1.1) {
            gain = (0.12 * constant * resources);
        } else if (target / self > 1.1) {
            gain = Math.max(0.0, (0.12 - (target / self - 1.1) * 0.206) * constant * resources);
        }
        return gain;
    }
}
