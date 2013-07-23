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

package intel;

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.irc.ValidationType;
import api.tools.numbers.NumberUtil;
import api.tools.text.RegexUtil;
import api.tools.time.DateUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.CommonEntitiesAccess;
import database.daos.ArmyDAO;
import database.daos.ProvinceDAO;
import database.models.Army;
import database.models.Province;
import database.models.SoM;
import events.CacheReloadEvent;
import lombok.extern.log4j.Log4j;
import tools.parsing.UtopiaValidationType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@Log4j
class SoMParser implements IntelParser<SoM> {
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<ArmyDAO> armyDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    private Pattern provincePattern;
    private Pattern netOffPattern;
    private Pattern netDefPattern;
    private Pattern armyReturnPattern;
    private Pattern generalsPattern;
    private Pattern soldiersPattern;
    private Pattern offSpecsPattern;
    private Pattern defSpecsPattern;
    private Pattern elitesPattern;
    private Pattern horsesPattern;
    private Pattern thievesPattern;
    private Pattern landPattern;

    private Pattern identifierPattern;

    @Inject
    SoMParser(final CommonEntitiesAccess commonEntitiesAccess,
              final Provider<ProvinceDAO> provinceDAOProvider,
              final Provider<BotUserDAO> botUserDAOProvider,
              final Provider<ArmyDAO> armyDAOProvider,
              final EventBus eventBus) {
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;
        this.armyDAOProvider = armyDAOProvider;
        this.commonEntitiesAccess = commonEntitiesAccess;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        provincePattern = Pattern.compile("Our thieves listen in on a report from the Military Elders of ([^(]+)(" +
                UtopiaValidationType.KDLOC.getPatternString() + ')');
        netOffPattern = Pattern.compile("Net Offensive Points at Home\\s*(" +
                ValidationType.INT.getPattern() + ')');
        netDefPattern = Pattern.compile("Net Defensive Points at Home\\s*(" +
                ValidationType.INT.getPattern() + ')');
        armyReturnPattern = Pattern.compile('(' + ValidationType.DOUBLE.getPattern() + ") days left");
        generalsPattern = Pattern.compile("Generals\\s+((?:" + UtopiaValidationType.GENERAL.getPatternString() + "\\s+)+)");
        soldiersPattern = Pattern.compile("Soldiers\\s+((?:" + ValidationType.INT.getPattern() + "\\s+)+)");
        offSpecsPattern = Pattern.compile("(?:" + commonEntitiesAccess.getOffSpecGroup() +
                ")\\s+((?:" + ValidationType.INT.getPattern() + "\\s+)+)");
        defSpecsPattern = Pattern.compile("(?:" + commonEntitiesAccess.getDefSpecGroup() +
                ")\\s+((?:(?:-|" + ValidationType.INT.getPattern() + ")\\s+)+)");
        elitesPattern = Pattern.compile("(?:" + commonEntitiesAccess.getEliteGroup() +
                ")\\s+((?:" + ValidationType.INT.getPattern() + "\\s+)+)");
        horsesPattern = Pattern.compile("War Horses\\s+((?:" + ValidationType.INT.getPattern() + "\\s+)+)");
        thievesPattern = Pattern.compile("Thieves\\s+((?:" + ValidationType.INT.getPattern() + "\\s+)+)");
        landPattern = Pattern.compile("Captured Land" +
                "\\s+((?:(?:-|" + ValidationType.INT.getPattern() + ")\\s+)+)");

        identifierPattern = Pattern.compile("(?:(?:" + provincePattern.pattern() +
                ")?.*?we have \\d generals available to lead our armies)");
    }

    @Subscribe
    public void onCacheReload(final CacheReloadEvent event) {
        compilePatterns();
    }

    @Override
    public Pattern getIdentifierPattern() {
        return identifierPattern;
    }

    @Override
    public SoM parse(final String savedBy, final String text) throws Exception {
        SoM som = new SoM();

        Matcher matcher = provincePattern.matcher(text);
        if (matcher.find()) {
            ProvinceDAO provinceDao = provinceDAOProvider.get();
            String name = matcher.group(1).trim();
            Province province = provinceDao.getOrCreateProvince(name, matcher.group(2));
            province.setName(name);
            if (province.getSom() != null) som = province.getSom();
            else som.setProvince(province);
        } else {
            BotUser user = botUserDAOProvider.get().getUser(savedBy);
            Province province = provinceDAOProvider.get().getProvinceForUser(user);
            if (province == null)
                throw new ParseException("Self som contains no province name, and user has no registered province", 0);
            if (province.getSom() != null) som = province.getSom();
            else som.setProvince(province);
        }

        matcher = netDefPattern.matcher(text);
        if (matcher.find()) {
            int netDef = NumberUtil.parseInt(matcher.group(1));
            som.setNetDefense(netDef);
        } else throw new ParseException("SoM to be parsed does not contain net def", 0);

        matcher = netOffPattern.matcher(text);
        if (matcher.find()) {
            int netOff = NumberUtil.parseInt(matcher.group(1));
            som.setNetOffense(netOff);
        } else throw new ParseException("SoM to be parsed does not contain net off", 0);

        Set<Army> allArmies = new HashSet<>();
        Army armyHome = SoMArmyUtil.getOrCreateHomeArmy(som, som.getProvince());
        allArmies.add(armyHome);

        List<Army> armiesOut = new ArrayList<>();

        Army armyTraining = SoMArmyUtil.getOrCreateTrainingArmy(som, som.getProvince());
        allArmies.add(armyTraining);

        matcher = armyReturnPattern.matcher(text);
        int armyNo = 2;
        while (matcher.find()) {
            double time = NumberUtil.parseDouble(matcher.group(1));
            long returnTime = System.currentTimeMillis() + DateUtil.hoursToMillis(time);
            Army army = SoMArmyUtil.getOrCreateOutArmy(som, armyNo, som.getProvince());
            army.setReturningDate(new Date(returnTime));
            armiesOut.add(army);
            ++armyNo;
        }

        matcher = generalsPattern.matcher(text);
        if (matcher.find()) {
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
            armyHome.setGenerals(NumberUtil.parseInt(split[0]));
            if (split.length > 1) {
                for (int i = 1; i < split.length && i <= armiesOut.size(); ++i) {
                    armiesOut.get(i - 1).setGenerals(NumberUtil.parseInt(split[i]));
                }
            }
        }

        matcher = soldiersPattern.matcher(text);
        if (matcher.find()) {
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
            armyHome.setSoldiers(NumberUtil.parseInt(split[0]));
            if (split.length > 1) {
                for (int i = 1; i < split.length && i <= armiesOut.size(); ++i) {
                    armiesOut.get(i - 1).setSoldiers(NumberUtil.parseInt(split[i]));
                }
            }
        }

        matcher = offSpecsPattern.matcher(text);
        if (matcher.find()) {
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
            armyHome.setOffSpecs(NumberUtil.parseInt(split[0]));
            if (split.length > 1) {
                for (int i = 1; i < split.length && i <= armiesOut.size(); ++i) {
                    armiesOut.get(i - 1).setOffSpecs(NumberUtil.parseInt(split[i]));
                }
            }

            if (matcher.find()) {
                split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
                int inTraining = 0;
                for (String aSplit : split) {
                    inTraining += NumberUtil.parseInt(aSplit);
                }
                armyTraining.setOffSpecs(inTraining);
            }
        }

        matcher = defSpecsPattern.matcher(text);
        if (matcher.find()) {
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
            armyHome.setDefSpecs(NumberUtil.parseInt(split[0]));

            if (matcher.find()) {
                split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
                int inTraining = 0;
                for (String aSplit : split) {
                    inTraining += NumberUtil.parseInt(aSplit);
                }
                armyTraining.setDefSpecs(inTraining);
            }
        }

        matcher = elitesPattern.matcher(text);
        if (matcher.find()) {
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
            armyHome.setElites(NumberUtil.parseInt(split[0]));
            if (split.length > 1) {
                for (int i = 1; i < split.length && i <= armiesOut.size(); ++i) {
                    armiesOut.get(i - 1).setElites(NumberUtil.parseInt(split[i]));
                }
            }

            if (matcher.find()) {
                split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
                int inTraining = 0;
                for (String aSplit : split) {
                    inTraining += NumberUtil.parseInt(aSplit);
                }
                armyTraining.setElites(inTraining);
            }
        }

        matcher = horsesPattern.matcher(text);
        if (matcher.find()) {
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
            armyHome.setWarHorses(NumberUtil.parseInt(split[0]));
            if (split.length > 1) {
                for (int i = 1; i < split.length && i <= armiesOut.size(); ++i) {
                    armiesOut.get(i - 1).setWarHorses(NumberUtil.parseInt(split[i]));
                }
            }
        }

        matcher = landPattern.matcher(text);
        if (matcher.find()) {
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
            if (split.length > 1) {
                for (int i = 1; i < split.length && i <= armiesOut.size(); ++i) {
                    armiesOut.get(i - 1).setLandGained(NumberUtil.parseInt(split[i]));
                }
            }
        }

        matcher = thievesPattern.matcher(text);
        if (matcher.find()) {
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(1));
            int inTraining = 0;
            for (String aSplit : split) {
                inTraining += NumberUtil.parseInt(aSplit);
            }
            armyTraining.setThieves(inTraining);
        }

        allArmies.addAll(armiesOut);
        som.setSavedBy(savedBy);
        som.setLastUpdated(new Date());

        for (Iterator<Army> iter = som.getArmies().iterator(); iter.hasNext(); ) {
            Army entry = iter.next();
            if (!allArmies.contains(entry)) iter.remove();
        }

        som.setExportLine(null);

        armyDAOProvider.get().save(allArmies);
        som.setArmiesOutWhenPosted(som.getArmiesOut().size());

        return som;
    }

    @Override
    public String getIntelTypeHandled() {
        return SoM.class.getSimpleName();
    }
}
