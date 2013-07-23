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

import api.irc.ValidationType;
import api.tools.numbers.NumberUtil;
import api.tools.time.DateUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.CommonEntitiesAccess;
import database.daos.DragonDAO;
import database.daos.ProvinceDAO;
import database.models.*;
import events.CacheReloadEvent;
import lombok.extern.log4j.Log4j;
import tools.parsing.UtopiaValidationType;
import tools.time.UtopiaTime;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.tools.text.StringUtil.isNullOrEmpty;
import static api.tools.time.DateUtil.isBefore;

@Singleton
@Log4j
class SoTParser implements IntelParser<SoT> {
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<DragonDAO> dragonDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;
    private final UtopiaTimeFactory utopiaTimeFactory;

    private Pattern identifierPattern;
    private Pattern provincePattern;
    private Pattern timePattern;
    private Pattern raceSoldPattern;
    private Pattern rulerOffSpecsPattern;
    private Pattern landDefSpecsPattern;
    private Pattern peasantsElitesPattern;
    private Pattern beThievesPattern;
    private Pattern moneyWizardsPattern;
    private Pattern foodHorsesPattern;
    private Pattern runesPrisonersPattern;
    private Pattern tbOffPattern;
    private Pattern nwDefPattern;
    private Pattern persAndTitlePattern;
    private Pattern persAndTitlePatternAlt;

    private Pattern plaguePattern;
    private Pattern overpopPattern;
    private Pattern hitPattern;
    private Pattern dragonPattern;

    @Inject
    SoTParser(final UtopiaTimeFactory utopiaTimeFactory, final CommonEntitiesAccess commonEntitiesAccess,
              final Provider<ProvinceDAO> provinceDAOProvider, final Provider<DragonDAO> dragonDAOProvider,
              final EventBus eventBus) {
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.provinceDAOProvider = provinceDAOProvider;
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.dragonDAOProvider = dragonDAOProvider;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        provincePattern = Pattern.compile("The Province of ([^(]+)(" + UtopiaValidationType.KDLOC.getPatternString() +
                ')');
        timePattern = Pattern.compile('(' + UtopiaValidationType.UTODATE.getPatternString() +
                ") \\(next tick: (?:(\\d{1,2} (?:minute|minutes|second|seconds))|imminent)");
        raceSoldPattern = Pattern.compile("Race\\s*(" + commonEntitiesAccess.getRaceGroup() + ")\\s*Soldiers\\s*(" +
                ValidationType.INT.getPattern() + ')');
        rulerOffSpecsPattern = Pattern.compile("Ruler\\s*(.*?)\\s*(?:" + commonEntitiesAccess.getOffSpecGroup() +
                ")\\s*(" + ValidationType.INT.getPattern() + ')');
        landDefSpecsPattern = Pattern.compile("Land\\s*(" + ValidationType.INT.getPattern() +
                ")\\s*(?:" + commonEntitiesAccess.getDefSpecGroup() + ")\\s*(" +
                ValidationType.INT.getPattern() + ')');
        peasantsElitesPattern = Pattern.compile("Peasants\\s*(" + ValidationType.INT.getPattern() +
                ")\\s*(?:" + commonEntitiesAccess.getEliteGroup() + ")\\s*(" +
                ValidationType.INT.getPattern() + ')');
        beThievesPattern = Pattern.compile("Building Eff\\.\\s*(" + ValidationType.INT.getPattern() +
                ")%\\s*Thieves\\s*(?:(" + ValidationType.INT.getPattern() +
                " \\((\\d+)%\\))|Unknown)");
        moneyWizardsPattern = Pattern.compile("Money\\s*(" + ValidationType.INT.getPattern() +
                ")\\s*Wizards\\s*(?:(" + ValidationType.INT.getPattern() +
                " \\((\\d+)%\\))|Unknown)");
        foodHorsesPattern = Pattern.compile("Food\\s*(" + ValidationType.INT.getPattern() +
                ")\\s*War Horses\\s*(" + ValidationType.INT.getPattern() + ')');
        runesPrisonersPattern = Pattern.compile("Runes\\s*(" + ValidationType.INT.getPattern() +
                ")\\s*Prisoners\\s*(" + ValidationType.INT.getPattern() + ')');
        tbOffPattern = Pattern.compile("Trade Balance\\s*([\\-]?" + ValidationType.INT.getPattern() +
                ")\\s*Off\\. Points\\s*(" + ValidationType.INT.getPattern() + ')');
        nwDefPattern = Pattern.compile("Networth\\s*(" + ValidationType.INT.getPattern() +
                ") gold coins\\s*Def\\. Points\\s*(" + ValidationType.INT.getPattern() + ')');

        persAndTitlePattern = Pattern.compile("The (" + commonEntitiesAccess.getPersonalityGroup() + ")\\s?(" +
                commonEntitiesAccess.getHonorTitleGroup() + ")?");
        persAndTitlePatternAlt = Pattern.compile("(?:(" + commonEntitiesAccess.getHonorTitleGroup() + ") .{1,30}|\\s+)the (" +
                commonEntitiesAccess.getPersonalityGroup() + ')');

        plaguePattern = Pattern.compile("The Plague has spread throughout the people");
        overpopPattern = Pattern.compile("Riots due to housing shortages");
        hitPattern = Pattern.compile("Province was hit (pretty heavily|moderately|a little|extremely badly) recently!");
        dragonPattern = Pattern.compile("A (" + dragonDAOProvider.get().getDragonGroup() + ") Dragon");

        identifierPattern = Pattern.compile(provincePattern.pattern() +
                "(?!\\s*(?:\\[http://www.utopiatemple.com Angel|\\[http://www.thedragonportal.eu Ultima))");
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
    public SoT parse(final String savedBy, final String text) throws Exception {
        SoT sot = new SoT();

        Province province;
        Matcher matcher = provincePattern.matcher(text);
        if (matcher.find()) {
            ProvinceDAO provinceDao = provinceDAOProvider.get();
            String name = matcher.group(1).trim();
            province = provinceDao.getOrCreateProvince(name, matcher.group(2));
            province.setName(name);
            if (province.getSot() != null) sot = province.getSot();
        } else throw new ParseException("SoT to be parsed does not contain name", 0);

        matcher = timePattern.matcher(text);
        if (matcher.find()) {
            String utodate = matcher.group(1).trim();
            int intoHour;
            if (matcher.group(2) == null || matcher.group(2).contains("second")) {
                intoHour = matcher.group(2) == null ? 60 : 59;
            } else {
                intoHour = Math.max(0, 60 - NumberUtil.parseInt(matcher.group(2)));
            }
            UtopiaTime ut = utopiaTimeFactory.newUtopiaTime(utodate);
            Date date = new Date(ut.getTime() + DateUtil.minutesToMillis(intoHour));
            if (sot.getLastUpdated() == null || isBefore(sot.getLastUpdated(), date)) {
                sot.setLastUpdated(date);
                sot.setProvince(province);
                province.setLastUpdated(date);
            } else return null;
        } else throw new ParseException("SoT to be parsed does not contain current uto date", 0);

        matcher = raceSoldPattern.matcher(text);
        if (matcher.find()) {
            Race race = commonEntitiesAccess.getRace(matcher.group(1));
            province.setRace(race);
            sot.setSoldiers(NumberUtil.parseInt(matcher.group(2)));
        } else throw new ParseException("SoT to be parsed does not contain race/soldiers", 0);

        matcher = rulerOffSpecsPattern.matcher(text);
        if (matcher.find()) {
            Matcher tempMatcher = persAndTitlePattern.matcher(matcher.group(1).trim());
            if (tempMatcher.find()) {
                province.setPersonality(commonEntitiesAccess.getPersonality(tempMatcher.group(1)));
                String honorTitle = tempMatcher.group(2);
                HonorTitle title = isNullOrEmpty(honorTitle) ? commonEntitiesAccess.getLowestRankingHonorTitle() :
                        commonEntitiesAccess.getHonorTitle(honorTitle);
                province.setHonorTitle(title);
            } else {
                tempMatcher = persAndTitlePatternAlt.matcher(text);
                if (tempMatcher.find()) {
                    province.setPersonality(commonEntitiesAccess.getPersonality(tempMatcher.group(2)));
                    String honorTitle = tempMatcher.group(1);
                    HonorTitle title = isNullOrEmpty(honorTitle) ? commonEntitiesAccess.getLowestRankingHonorTitle() :
                            commonEntitiesAccess.getHonorTitle(honorTitle);
                    province.setHonorTitle(title);
                } else throw new ParseException("Could not parse the personality and honor title", 0);
            }
            sot.setOffSpecs(NumberUtil.parseInt(matcher.group(2)));
        } else throw new ParseException("SoT to be parsed does not contain ruler/off specs", 0);

        matcher = landDefSpecsPattern.matcher(text);
        if (matcher.find()) {
            province.setLand(NumberUtil.parseInt(matcher.group(1)));
            sot.setDefSpecs(NumberUtil.parseInt(matcher.group(2)));
        } else throw new ParseException("SoT to be parsed does not contain land/def specs", 0);

        matcher = peasantsElitesPattern.matcher(text);
        if (matcher.find()) {
            sot.setPeasants(NumberUtil.parseInt(matcher.group(1)));
            sot.setElites(NumberUtil.parseInt(matcher.group(2)));
        } else throw new ParseException("SoT to be parsed does not contain peasants/elites", 0);

        matcher = beThievesPattern.matcher(text);
        if (matcher.find()) {
            sot.setBuildingEfficiency(NumberUtil.parseInt(matcher.group(1)));
            if (matcher.group(2) != null) {
                province.setThieves(NumberUtil.parseInt(matcher.group(2)));
                province.setStealth(NumberUtil.parseInt(matcher.group(3)));
                province.setThievesLastUpdated(new Date());
            }
        } else throw new ParseException("SoT to be parsed does not contain be/thieves", 0);

        matcher = moneyWizardsPattern.matcher(text);
        if (matcher.find()) {
            sot.setMoney(NumberUtil.parseInt(matcher.group(1)));
            if (matcher.group(2) != null) {
                province.setWizards(NumberUtil.parseInt(matcher.group(2)));
                province.setMana(NumberUtil.parseInt(matcher.group(3)));
                province.setWizardsLastUpdated(new Date());
            }
        } else throw new ParseException("SoT to be parsed does not contain money/wizards", 0);

        matcher = foodHorsesPattern.matcher(text);
        if (matcher.find()) {
            sot.setFood(NumberUtil.parseInt(matcher.group(1)));
            sot.setWarHorses(NumberUtil.parseInt(matcher.group(2)));
        } else throw new ParseException("SoT to be parsed does not contain food/war horses", 0);

        matcher = runesPrisonersPattern.matcher(text);
        if (matcher.find()) {
            sot.setRunes(NumberUtil.parseInt(matcher.group(1)));
            sot.setPrisoners(NumberUtil.parseInt(matcher.group(2)));
        } else throw new ParseException("SoT to be parsed does not contain runes/prisoners", 0);

        matcher = tbOffPattern.matcher(text);
        if (matcher.find()) {
            sot.setTradeBalance(NumberUtil.parseInt(matcher.group(1)));
            sot.setModOffense(NumberUtil.parseInt(matcher.group(2)));
        } else throw new ParseException("SoT to be parsed does not contain tb/mod off", 0);

        matcher = nwDefPattern.matcher(text);
        if (matcher.find()) {
            province.setNetworth(NumberUtil.parseInt(matcher.group(1)));
            sot.setModDefense(NumberUtil.parseInt(matcher.group(2)));
        } else throw new ParseException("SoT to be parsed does not contain nw/mod def", 0);


        matcher = plaguePattern.matcher(text);
        sot.setPlagued(matcher.find());

        matcher = overpopPattern.matcher(text);
        sot.setOverpopulated(matcher.find());

        matcher = hitPattern.matcher(text);
        if (matcher.find()) {
            String hit = matcher.group(1);
            sot.setHitStatus(hit);
        }

        if (!sot.getRace().isDragonImmune() && !sot.getPersonality().isDragonImmune()) {
            matcher = dragonPattern.matcher(text);
            if (matcher.find()) {
                Dragon dragon = dragonDAOProvider.get().getDragon(matcher.group(1));
                province.getKingdom().setDragon(dragon);
            } else {
                province.getKingdom().setDragon(null);
            }
        }

        sot.setSavedBy(savedBy);
        sot.setExportLine(null);

        return sot;
    }

    @Override
    public String getIntelTypeHandled() {
        return SoT.class.getSimpleName();
    }
}
