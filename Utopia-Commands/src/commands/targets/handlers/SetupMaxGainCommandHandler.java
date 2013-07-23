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

package commands.targets.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import api.tools.collections.Params;
import api.tools.time.DateUtil;
import database.daos.KingdomDAO;
import database.daos.TargetDAO;
import database.models.*;
import filtering.filters.AgeFilter;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.GameMechanicCalculator;
import tools.UtopiaPropertiesConfig;

import javax.inject.Inject;
import java.util.*;

public class SetupMaxGainCommandHandler implements CommandHandler {
    private final KingdomDAO kingdomDAO;
    private final TargetDAO targetDAO;
    private final PropertiesCollection properties;

    @Inject
    public SetupMaxGainCommandHandler(final PropertiesCollection properties,
                                      final TargetDAO targetDAO,
                                      final KingdomDAO kingdomDAO) {
        this.properties = properties;
        this.targetDAO = targetDAO;
        this.kingdomDAO = kingdomDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            targetDAO.delete(targetDAO.getTargetsOfType(Target.TargetType.GENERATED_TARGET));

            Kingdom selfKD = kingdomDAO.getSelfKD();

            Kingdom targetKD = kingdomDAO.getKingdom(params.getParameter("kd"));
            if (targetKD == null) return CommandResponse.errorResponse("Target kd has not yet been added to the bot");

            Date maxAge = new Date(System.currentTimeMillis() - DateUtil.hoursToMillis(properties.getDouble(UtopiaPropertiesConfig.MAX_INTEL_AGE)));
            AgeFilter filter = AgeFilter.createMoreRecentThanFilter(maxAge);

            List<Hitter> hitters = createHittersList(selfKD, params.getParameter("start"), filter);

            List<Defender> defenders = createDefendersList(targetKD, params.containsKey("war"), filter);

            defenders = calcOptimumHitsOnDefenders(hitters, defenders, AttackType.TM);

            List<Target> targets = createAndSaveTargets(defenders);

            return CommandResponse.resultResponse("targets", targets, "added", targets.size());
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static List<Hitter> createHittersList(Kingdom selfKD, String sorting, AgeFilter filter) {
        List<Province> provinces = sort(selfKD.getProvinces(), sorting == null ? "low" : sorting);
        List<Hitter> hitters = new ArrayList<>(provinces.size());
        for (Province province : provinces) {
            if (province.getSot() != null && filter.passesFilter(province.getSot().getLastUpdated()) && province.getProvinceOwner() != null)
                hitters.add(new Hitter(province));
        }
        return hitters;
    }

    private List<Defender> createDefendersList(Kingdom targetKD, boolean isWar, AgeFilter filter) {
        Collection<Province> provinces = targetKD.getProvinces();
        List<Defender> defenders = new ArrayList<>(provinces.size());
        for (Province province : provinces) {
            if (province.getSot() != null && filter.passesFilter(province.getSot().getLastUpdated()))
                defenders.add(new Defender(province, isWar, properties));
        }
        return defenders;
    }

    private List<Defender> calcOptimumHitsOnDefenders(List<Hitter> hitters,
                                                      List<Defender> defenders,
                                                      AttackType attackType) {
        int minGain;
        while (!hitters.isEmpty()) {
            Hitter hitter = hitters.get(0);
            if (hitter.generals < 1) {
                hitters.remove(hitter);
                continue;
            }
            minGain = (int) Math.floor(properties.getDouble(UtopiaPropertiesConfig.MIN_GAIN) * hitter.province.getLand());
            Defender bestDefender = null;
            int bestGain = 0;
            int offRequired = 0;
            for (Defender defender : defenders) {
                //TODO FUTURE use the bonuses from generals to calc offense
                if (hitter.offense > defender.defense * 1.041) {
                    int gain = (int) GameMechanicCalculator
                            .calcGains(hitter.province.getNetworth(), defender.currentNw, defender.currentLand, attackType);
                    gain *= (1 - defender.gbProt) * calcGainMods(hitter.province, defender.province);
                    if (gain >= minGain && gain > bestGain) {
                        bestDefender = defender;
                        bestGain = gain;
                        offRequired = (int) (defender.defense * 1.041);
                    }
                }
            }
            if (bestDefender != null) {
                bestDefender.update(bestGain, hitter);
                hitter.offense -= offRequired;
                hitter.generals -= 1;
            } else {
                hitters.remove(0);
            }
        }
        return defenders;
    }

    private static double calcGainMods(final Province hitter, final Province defender) {
        double mod = 1.0;
        Dragon dragon = hitter.getKingdom().getDragon();
        if (dragon != null) dragon.getBonus(BonusType.GAIN, BonusApplicability.OFFENSIVELY).applyTo(mod);
        mod = applyGainMods(mod, hitter, BonusApplicability.OFFENSIVELY);
        mod = applyGainMods(mod, defender, BonusApplicability.DEFENSIVELY);
        return mod;
    }

    private static double applyGainMods(final double value,
                                        final Province province,
                                        final BonusApplicability applicability) {
        double out = value;
        if (province.getRace() != null) {
            Bonus bonus = province.getRace().getBonus(BonusType.GAIN, applicability);
            out = bonus.applyTo(out);
        }
        if (province.getPersonality() != null) {
            Bonus bonus = province.getPersonality().getBonus(BonusType.GAIN, applicability);
            out = bonus.applyTo(out);
        }
        Survey survey = province.getSurvey();
        if (survey != null) {
            out = survey.applyAnyBonuses(BonusType.GAIN, applicability, out);
        }
        SoS sos = province.getSos();
        if (sos != null) {
            out = sos.applyAnyBonuses(BonusType.GAIN, applicability, out);
        }
        return out;
    }

    private List<Target> createAndSaveTargets(List<Defender> defenders) {
        List<Target> targets = new ArrayList<>(defenders.size());
        for (Defender defender : defenders) {
            if (!defender.hitters.isEmpty()) {
                Target target = new Target(defender.province, Target.TargetType.GENERATED_TARGET, "Max gains target", null);
                for (BotUser hitter : defender.hitters) {
                    target.insertHitter(hitter, target.getAmountOfHitters() + 1);
                }
                targets.add(target);
            }
        }
        targetDAO.save(targets);
        return targets;
    }

    private static List<Province> sort(Collection<Province> toSort, String sort) {
        List<Province> out = new ArrayList<>(toSort);
        if ("mid".equals(sort)) midSort(out);
        else {
            Collections.sort(out, new NetworthComparator());
            if ("high".equals(sort)) Collections.reverse(out);
        }
        return out;
    }

    private static <E> void midSort(List<E> toSort) {
        List<E> unsortedList = new ArrayList<>(toSort);
        toSort.clear();
        while (!unsortedList.isEmpty()) {
            toSort.add(unsortedList.remove(unsortedList.size() / 2));
        }
    }

    private static class NetworthComparator implements Comparator<Province> {
        @Override
        public int compare(final Province o1, final Province o2) {
            return Integer.compare(o1.getNetworth(), o2.getNetworth());
        }
    }

    private static class Hitter {
        public Province province;
        public int offense;
        public int generals;

        private Hitter(Province province) {
            this.province = province;
            this.offense = province.getEstimatedCurrentOffense();
            this.generals = province.getGeneralsHome();
        }
    }

    private static class Defender {
        private final PropertiesCollection properties;
        public Province province;
        public double gbProt;
        public int currentLand;
        public int currentNw;
        public int defense;
        public List<BotUser> hitters = new ArrayList<>();

        private Defender(Province province, boolean isWar, final PropertiesCollection properties) {
            this.province = province;
            this.properties = properties;
            this.gbProt = province.getSot().getApproximateGBProt(isWar);
            this.currentLand = province.getLand();
            this.currentNw = province.getNetworth();
            this.defense = province.getEstimatedCurrentDefense();
        }

        public void update(int landGain, Hitter hitter) {
            gbProt = Math.max(0.9, gbProt + landGain * 1.0 / currentLand * 1.5);
            currentLand -= landGain;
            currentNw -= landGain * properties.getInteger(UtopiaPropertiesConfig.BUILT_ACRE_NW);
            hitters.add(hitter.province.getProvinceOwner());
        }
    }
}
