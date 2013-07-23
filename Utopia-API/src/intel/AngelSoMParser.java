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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@Log4j
class AngelSoMParser implements IntelParser<SoM> {
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<ArmyDAO> armyDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    private Pattern provinceNamePattern;
    private Pattern persAndTitlePattern;
    private Pattern persAndTitlePatternAlt;
    private Pattern peasantTitlePattern;
    private Pattern raceAndPersPattern;
    private Pattern netDefensePattern;
    private Pattern netOffensePattern;
    private Pattern armyHomePattern;
    private Pattern armyOutPattern;
    private Pattern armyTrainingPattern;
    private Pattern generalsPattern;
    private Pattern soldiersPattern;
    private Pattern offSpecsPattern;
    private Pattern defSpecsPattern;
    private Pattern elitesPattern;
    private Pattern warHorsesPattern;
    private Pattern thievesPattern;
    private Pattern landPattern;

    private Pattern identifierPattern;
    private static final String ARMY_SECTION_ENDINGS = "(?:\\*\\* Army|\\*\\* Troops in Training \\*\\*|\\*\\* Finished \\*\\*|$)";

    @Inject
    AngelSoMParser(final CommonEntitiesAccess commonEntitiesAccess,
                   final Provider<ProvinceDAO> provinceDAOProvider,
                   final Provider<ArmyDAO> armyDAOProvider,
                   final EventBus eventBus,
                   final Provider<BotUserDAO> botUserDAOProvider) {
        this.provinceDAOProvider = provinceDAOProvider;
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.armyDAOProvider = armyDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        provinceNamePattern = Pattern
                .compile("Military (?:Intel|Intelligence) on ([^(]+)(" + UtopiaValidationType.KDLOC.getPatternString() + ')');
        Pattern selfSomPattern = Pattern.compile("Military (?:Intel|Intelligence) Formatted Report");
        persAndTitlePattern = Pattern.compile(
                "Ruler Name: The (?:" + commonEntitiesAccess.getPersonalityGroup() + ") (" + commonEntitiesAccess.getHonorTitleGroup() +
                        ')');
        persAndTitlePatternAlt = Pattern.compile(
                "Ruler Name: (" + commonEntitiesAccess.getHonorTitleGroup() + ") .*? the (?:" + commonEntitiesAccess.getPersonalityGroup() +
                        ')');
        peasantTitlePattern = Pattern.compile("Ruler Name: ");
        raceAndPersPattern = Pattern.compile("Personality & Race: The (" +
                commonEntitiesAccess.getPersonalityGroup() + "), (" + commonEntitiesAccess.getRaceGroup() +
                ')');
        netDefensePattern = Pattern.compile("Net Defense at Home \\(from Utopia\\): (" +
                ValidationType.INT.getPattern() + ')');
        netOffensePattern = Pattern.compile("Net Offense at Home \\(from Utopia\\): (" +
                ValidationType.INT.getPattern() + ')');
        armyHomePattern = Pattern.compile("\\*\\* Standing Army \\(At Home\\) \\*\\*(.*?)(?=" + ARMY_SECTION_ENDINGS + ')');
        armyOutPattern = Pattern
                .compile("\\*\\* Army #([2-7]) \\(Back in (\\d{1,2}:\\d{1,2}) hours\\) \\*\\*(.*?)(?=" + ARMY_SECTION_ENDINGS + ')');
        armyTrainingPattern = Pattern.compile("\\*\\* Troops in Training \\*\\*(.*)");
        generalsPattern = Pattern.compile("Generals: (\\d)");
        soldiersPattern = Pattern.compile("Soldiers: (" + ValidationType.INT.getPattern() + ')');
        offSpecsPattern = Pattern.compile("(?:" + commonEntitiesAccess.getOffSpecGroup() + "): (" +
                ValidationType.INT.getPattern() + ')');
        defSpecsPattern = Pattern.compile("(?:" + commonEntitiesAccess.getDefSpecGroup() + "): (" +
                ValidationType.INT.getPattern() + ')');
        elitesPattern = Pattern.compile("(?:" + commonEntitiesAccess.getEliteGroup() + "): (" +
                ValidationType.INT.getPattern() + ')');
        warHorsesPattern = Pattern.compile("War Horses: (" + ValidationType.INT.getPattern() + ')');
        thievesPattern = Pattern.compile("Thieves: (" + ValidationType.INT.getPattern() + ')');
        landPattern = Pattern.compile("Captured Land: (" + ValidationType.INT.getPattern() + ") Acres");

        identifierPattern = Pattern
                .compile('(' + provinceNamePattern.pattern() + '|' + selfSomPattern.pattern() + ")\\s*(?:\\[http://www.utopiatemple.com Angel|\\[http://www.thedragonportal.eu Ultima)");
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

        Matcher matcher = provinceNamePattern.matcher(text);
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

        matcher = persAndTitlePattern.matcher(text);
        if (matcher.find()) {
            som.getProvince().setHonorTitle(commonEntitiesAccess.getHonorTitle(matcher.group(1)));
        } else {
            matcher = persAndTitlePatternAlt.matcher(text);
            if (matcher.find()) {
                som.getProvince().setHonorTitle(commonEntitiesAccess.getHonorTitle(matcher.group(1)));
            } else if (peasantTitlePattern.matcher(text).find()) {
                som.getProvince().setHonorTitle(commonEntitiesAccess.getLowestRankingHonorTitle());
            }
        }

        matcher = raceAndPersPattern.matcher(text);
        if (matcher.find()) {
            som.getProvince().setPersonality(commonEntitiesAccess.getPersonality(matcher.group(1)));
            som.getProvince().setRace(commonEntitiesAccess.getRace(matcher.group(2)));
        }

        matcher = netDefensePattern.matcher(text);
        if (matcher.find()) {
            int netDef = NumberUtil.parseInt(matcher.group(1));
            som.setNetDefense(netDef);
        } else som.setNetDefense(null);

        matcher = netOffensePattern.matcher(text);
        if (matcher.find()) {
            int netOff = NumberUtil.parseInt(matcher.group(1));
            som.setNetOffense(netOff);
        } else som.setNetOffense(null);

        Set<Army> armies = new HashSet<>();

        matcher = armyHomePattern.matcher(text);
        if (matcher.find()) {
            String armyText = matcher.group(1);
            Army army = parseArmy(armyText, SoMArmyUtil.getOrCreateHomeArmy(som, som.getProvince()));
            armies.add(army);
        } else throw new ParseException("SoM to be parsed does not contain an army home entry", 0);

        matcher = armyOutPattern.matcher(text);
        while (matcher.find()) {
            int armyno = NumberUtil.parseInt(matcher.group(1));
            String time = matcher.group(2);
            String[] split = RegexUtil.COLON_PATTERN.split(time);
            long returnTime = System.currentTimeMillis() + DateUtil.hoursToMillis(NumberUtil.parseInt(split[0])) +
                    DateUtil.minutesToMillis(NumberUtil.parseInt(split[1]) - 5);
            String armyText = matcher.group(3);
            Army army = parseArmy(armyText, SoMArmyUtil.getOrCreateOutArmy(som, armyno, som.getProvince()));
            army.setReturningDate(new Date(returnTime));
            armies.add(army);
        }

        matcher = armyTrainingPattern.matcher(text);
        if (matcher.find()) {
            String armyText = matcher.group(1);
            Army army = parseArmy(armyText, SoMArmyUtil.getOrCreateTrainingArmy(som, som.getProvince()));
            armies.add(army);
        }

        for (Iterator<Army> iter = som.getArmies().iterator(); iter.hasNext(); ) {
            Army entry = iter.next();
            if (!armies.contains(entry)) iter.remove();
        }

        matcher = exportLinePattern.matcher(text);
        if (matcher.find()) {
            som.setExportLine(matcher.group(1));
        } else som.setExportLine(null);
        som.setSavedBy(savedBy);
        som.setLastUpdated(new Date());

        armyDAOProvider.get().save(armies);
        som.setArmiesOutWhenPosted(som.getArmiesOut().size());

        return som;
    }

    @Override
    public String getIntelTypeHandled() {
        return SoM.class.getSimpleName();
    }

    private Army parseArmy(final CharSequence text, final Army army) {
        Matcher matcher = generalsPattern.matcher(text);
        if (matcher.find()) {
            int gens = NumberUtil.parseInt(matcher.group(1));
            army.setGenerals(gens);
        } else army.setGenerals(0);

        matcher = soldiersPattern.matcher(text);
        if (matcher.find()) {
            int soldiers = NumberUtil.parseInt(matcher.group(1));
            army.setSoldiers(soldiers);
        } else army.setSoldiers(0);

        matcher = offSpecsPattern.matcher(text);
        if (matcher.find()) {
            int os = NumberUtil.parseInt(matcher.group(1));
            army.setOffSpecs(os);
        } else army.setOffSpecs(0);

        matcher = defSpecsPattern.matcher(text);
        if (matcher.find()) {
            int ds = NumberUtil.parseInt(matcher.group(1));
            army.setDefSpecs(ds);
        } else army.setDefSpecs(0);

        matcher = elitesPattern.matcher(text);
        if (matcher.find()) {
            int elites = NumberUtil.parseInt(matcher.group(1));
            army.setElites(elites);
        } else army.setElites(0);

        matcher = warHorsesPattern.matcher(text);
        if (matcher.find()) {
            int horses = NumberUtil.parseInt(matcher.group(1));
            army.setWarHorses(horses);
        } else army.setWarHorses(0);

        matcher = thievesPattern.matcher(text);
        if (matcher.find()) {
            int thieves = NumberUtil.parseInt(matcher.group(1));
            army.setThieves(thieves);
        } else army.setThieves(0);

        matcher = landPattern.matcher(text);
        if (matcher.find()) {
            int land = NumberUtil.parseInt(matcher.group(1));
            army.setLandGained(land);
        } else army.setLandGained(0);

        return army;
    }
}
