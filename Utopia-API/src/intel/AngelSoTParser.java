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
import api.settings.PropertiesCollection;
import api.tools.numbers.NumberUtil;
import api.tools.time.DateUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.CommonEntitiesAccess;
import database.daos.DragonDAO;
import database.daos.ProvinceDAO;
import database.models.Dragon;
import database.models.Province;
import database.models.SoT;
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

import static api.tools.time.DateUtil.isBefore;
import static tools.UtopiaPropertiesConfig.TICK_LENGTH;

@Singleton
@Log4j
class AngelSoTParser implements IntelParser<SoT> {
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<DragonDAO> dragonDAOProvider;
    private final PropertiesCollection properties;
    private final UtopiaTimeFactory utopiaTimeFactory;

    private final CommonEntitiesAccess commonEntitiesAccess;
    private Pattern provinceNamePattern;
    private Pattern utopiaTimePattern;
    private Pattern persAndTitlePattern;
    private Pattern persAndTitlePatternAlt;
    private Pattern peasantTitlePattern;
    private Pattern raceAndPersPattern;
    private Pattern landPattern;
    private Pattern moneyPattern;
    private Pattern foodPattern;
    private Pattern runesPattern;
    private Pattern peasantsAndBEPattern;
    private Pattern tradeBalancePattern;
    private Pattern nwPattern;
    private Pattern soldiersPattern;
    private Pattern offSpecsPattern;
    private Pattern defSpecsPattern;
    private Pattern elitesPattern;
    private Pattern warHorsesPattern;
    private Pattern prisonersPattern;
    private Pattern modOffensePattern;
    private Pattern modDefensePattern;
    private Pattern thievesPattern;
    private Pattern wizardsPattern;
    private Pattern plaguePattern;
    private Pattern overpopPattern;
    private Pattern hitPattern;
    private Pattern dragonPattern;

    private Pattern identifierPattern;

    @Inject
    AngelSoTParser(final PropertiesCollection properties, final UtopiaTimeFactory utopiaTimeFactory,
                   final CommonEntitiesAccess commonEntitiesAccess, final Provider<ProvinceDAO> provinceDAOProvider,
                   final Provider<DragonDAO> dragonDAOProvider, final EventBus eventBus) {
        this.properties = properties;
        this.utopiaTimeFactory = utopiaTimeFactory;
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.provinceDAOProvider = provinceDAOProvider;
        this.dragonDAOProvider = dragonDAOProvider;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        provinceNamePattern = Pattern.compile("The Province of ([^(]+)(" + UtopiaValidationType.KDLOC.getPatternString() + ')');
        utopiaTimePattern = Pattern.compile("Utopian Date: (" + UtopiaValidationType.UTODATE.getPatternString() +
                ") \\((\\d+)% in the day\\)");
        persAndTitlePattern = Pattern.compile("Ruler Name: The (?:" + this.commonEntitiesAccess.getPersonalityGroup() + ") (" +
                this.commonEntitiesAccess.getHonorTitleGroup() + ')');
        persAndTitlePatternAlt = Pattern.compile("Ruler Name: (" + this.commonEntitiesAccess.getHonorTitleGroup() + ") .*? the (?:" +
                this.commonEntitiesAccess.getPersonalityGroup() + ')');
        peasantTitlePattern = Pattern.compile("Ruler Name: ");
        raceAndPersPattern = Pattern.compile("Personality & Race: The (" +
                this.commonEntitiesAccess.getPersonalityGroup() + "), (" +
                this.commonEntitiesAccess.getRaceGroup() + ')');
        landPattern = Pattern.compile("Land: (" + ValidationType.INT.getPattern() + ") Acres");
        moneyPattern = Pattern.compile("Money: (" + ValidationType.INT.getPattern() + ")gc.*?daily income");
        foodPattern = Pattern.compile("Food: (" + ValidationType.INT.getPattern() + ") bushels");
        runesPattern = Pattern.compile("Runes: (" + ValidationType.INT.getPattern() + ") runes");
        peasantsAndBEPattern = Pattern.compile("Peasants: (" + ValidationType.INT.getPattern() + ") \\((\\d+)");
        tradeBalancePattern = Pattern.compile("Trade Balance: ([-]?" + ValidationType.INT.getPattern() + ")gc");
        nwPattern = Pattern.compile("Total Networth: (" + ValidationType.INT.getPattern() + ")gc");
        soldiersPattern = Pattern.compile("Soldiers: (" + ValidationType.INT.getPattern() + ")(?!gc)");
        offSpecsPattern = Pattern.compile("(?:" + this.commonEntitiesAccess.getOffSpecGroup() +
                "): (" + ValidationType.INT.getPattern() + ")(?!gc)");
        defSpecsPattern = Pattern.compile("(?:" + this.commonEntitiesAccess.getDefSpecGroup() +
                "): (" + ValidationType.INT.getPattern() + ")(?!gc)");
        elitesPattern = Pattern.compile("(?:" + this.commonEntitiesAccess.getEliteGroup() +
                "): (" + ValidationType.INT.getPattern() + ")(?!gc)");
        warHorsesPattern = Pattern.compile("War-Horses: (" + ValidationType.INT.getPattern() + ")(?!gc)");
        prisonersPattern = Pattern.compile("Prisoners: (" + ValidationType.INT.getPattern() + ')');
        modOffensePattern = Pattern.compile("Total Modified Offense: (" + ValidationType.INT.getPattern() + ')');
        modDefensePattern = Pattern.compile("Total Modified Defense: (" + ValidationType.INT.getPattern() + ')');
        thievesPattern = Pattern.compile("Thieves: (" + ValidationType.INT.getPattern() + ").*?(?:(\\d+)% Stealth)");
        wizardsPattern = Pattern.compile("Wizards: (" + ValidationType.INT.getPattern() + ").*?(?:(\\d+)% Mana)");
        plaguePattern = Pattern.compile("The Plague has spread throughout the people");
        overpopPattern = Pattern.compile("Riots due to housing shortages from overpopulation" + "are hampering tax collection efforts!");
        hitPattern = Pattern.compile("Province was hit (pretty heavily|moderately|a little|extremely badly) recently!");
        dragonPattern = Pattern.compile("An? (?<dragon>" + dragonDAOProvider.get().getDragonGroup() + ") Dragon ravages the lands!");

        identifierPattern = Pattern.compile(
                provinceNamePattern.pattern() + "\\s*(?:\\[http://www.utopiatemple.com Angel|\\[http://www.thedragonportal.eu Ultima)");
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
        sot.setAngelIntel(true);
        Province province;
        Matcher matcher = provinceNamePattern.matcher(text);
        if (matcher.find()) {
            ProvinceDAO provinceDao = provinceDAOProvider.get();
            String name = matcher.group(1).trim();
            province = provinceDao.getOrCreateProvince(name, matcher.group(2));
            province.setName(name);
            if (province.getSot() != null) sot = province.getSot();
        } else throw new ParseException("SoT to be parsed does not contain name", 0);

        matcher = utopiaTimePattern.matcher(text);
        if (matcher.find()) {
            String utodate = matcher.group(1).trim();
            int intoHour = NumberUtil.parseInt(matcher.group(2));
            int tickLength = properties.getInteger(TICK_LENGTH);
            double minutes = intoHour / 100.0 * tickLength;
            UtopiaTime ut = utopiaTimeFactory.newUtopiaTime(utodate);
            Date date = new Date(ut.getTime() + DateUtil.minutesToMillis(minutes));
            if (sot.getLastUpdated() == null || isBefore(sot.getLastUpdated(), date)) {
                sot.setLastUpdated(date);
                sot.setProvince(province);
                province.setLastUpdated(date);
            } else return null;
        } else {
            Date now = new Date();
            sot.setLastUpdated(now);
            sot.setProvince(province);
            province.setLastUpdated(now);
        }

        matcher = persAndTitlePattern.matcher(text);
        if (matcher.find()) {
            province.setHonorTitle(commonEntitiesAccess.getHonorTitle(matcher.group(1)));
        } else {
            matcher = persAndTitlePatternAlt.matcher(text);
            if (matcher.find()) {
                province.setHonorTitle(commonEntitiesAccess.getHonorTitle(matcher.group(1)));
            } else if (peasantTitlePattern.matcher(text).find()) {
                province.setHonorTitle(commonEntitiesAccess.getLowestRankingHonorTitle());
            }
        }

        matcher = raceAndPersPattern.matcher(text);
        if (matcher.find()) {
            province.setPersonality(commonEntitiesAccess.getPersonality(matcher.group(1)));
            province.setRace(commonEntitiesAccess.getRace(matcher.group(2)));
        } else throw new ParseException("SoT to be parsed does not contain a race and personality", 0);

        matcher = landPattern.matcher(text);
        if (matcher.find()) {
            String land = matcher.group(1).trim();
            province.setLand(NumberUtil.parseInt(land));
        } else throw new ParseException("SoT to be parsed does not contain land", 0);

        matcher = moneyPattern.matcher(text);
        if (matcher.find()) {
            String money = matcher.group(1).trim();
            sot.setMoney(NumberUtil.parseInt(money));
        } else throw new ParseException("SoT to be parsed does not contain gcs", 0);

        matcher = foodPattern.matcher(text);
        if (matcher.find()) {
            String food = matcher.group(1).trim();
            sot.setFood(NumberUtil.parseInt(food));
        } else throw new ParseException("SoT to be parsed does not contain bushels", 0);

        matcher = runesPattern.matcher(text);
        if (matcher.find()) {
            String runes = matcher.group(1).trim();
            sot.setRunes(NumberUtil.parseInt(runes));
        } else throw new ParseException("SoT to be parsed does not contain runes", 0);

        matcher = peasantsAndBEPattern.matcher(text);
        if (matcher.find()) {
            String peasants = matcher.group(1);
            String be = matcher.group(2);
            peasants = peasants.trim();
            sot.setPeasants(NumberUtil.parseInt(peasants));
            sot.setBuildingEfficiency(NumberUtil.parseInt(be));
        } else throw new ParseException("SoT to be parsed does not contain peasants/BE", 0);

        matcher = tradeBalancePattern.matcher(text);
        if (matcher.find()) {
            String tb = matcher.group(1).trim();
            sot.setTradeBalance(NumberUtil.parseInt(tb));
        } else throw new ParseException("SoT to be parsed does not contain trade balance", 0);

        matcher = nwPattern.matcher(text);
        if (matcher.find()) {
            String nw = matcher.group(1).trim();
            province.setNetworth(NumberUtil.parseInt(nw));
        } else throw new ParseException("SoT to be parsed does not contain networth", 0);

        matcher = soldiersPattern.matcher(text);
        if (matcher.find()) {
            String solds = matcher.group(1).trim();
            sot.setSoldiers(NumberUtil.parseInt(solds));
        } else throw new ParseException("SoT to be parsed does not contain soldiers", 0);

        matcher = offSpecsPattern.matcher(text);
        if (matcher.find()) {
            String os = matcher.group(1).trim();
            sot.setOffSpecs(NumberUtil.parseInt(os));
        } else throw new ParseException("SoT to be parsed does not contain off specs", 0);

        matcher = defSpecsPattern.matcher(text);
        if (matcher.find()) {
            String ds = matcher.group(1).trim();
            sot.setDefSpecs(NumberUtil.parseInt(ds));
        } else throw new ParseException("SoT to be parsed does not contain def specs", 0);

        matcher = elitesPattern.matcher(text);
        if (matcher.find()) {
            String elites = matcher.group(1).trim();
            sot.setElites(NumberUtil.parseInt(elites));
        } else throw new ParseException("SoT to be parsed does not contain elites", 0);

        matcher = warHorsesPattern.matcher(text);
        if (matcher.find()) {
            String horses = matcher.group(1).trim();
            sot.setWarHorses(NumberUtil.parseInt(horses));
        } else throw new ParseException("SoT to be parsed does not contain war horses", 0);

        matcher = prisonersPattern.matcher(text);
        if (matcher.find()) {
            String prisoners = matcher.group(1).trim();
            sot.setPrisoners(NumberUtil.parseInt(prisoners));
        } else sot.setPrisoners(0);

        matcher = modOffensePattern.matcher(text);
        if (matcher.find()) {
            String mo = matcher.group(1).trim();
            sot.setModOffense(NumberUtil.parseInt(mo));
        } else throw new ParseException("SoT to be parsed does not contain mod off", 0);

        matcher = modDefensePattern.matcher(text);
        if (matcher.find()) {
            String md = matcher.group(1).trim();
            sot.setModDefense(NumberUtil.parseInt(md));
        } else throw new ParseException("SoT to be parsed does not contain mod defense", 0);

        matcher = thievesPattern.matcher(text);
        if (matcher.find()) {
            String thieves = matcher.group(1).trim();
            province.setThieves(NumberUtil.parseInt(thieves));
            province.setStealth(Integer.parseInt(matcher.group(2)));
            province.setThievesLastUpdated(new Date());
        }

        matcher = wizardsPattern.matcher(text);
        if (matcher.find()) {
            String wizards = matcher.group(1).trim();
            province.setWizards(NumberUtil.parseInt(wizards));
            province.setMana(Integer.parseInt(matcher.group(2)));
            province.setWizardsLastUpdated(new Date());
        }

        matcher = plaguePattern.matcher(text);
        sot.setPlagued(matcher.find());

        matcher = overpopPattern.matcher(text);
        sot.setOverpopulated(matcher.find());

        matcher = hitPattern.matcher(text);
        if (matcher.find()) {
            String hit = matcher.group(1);
            sot.setHitStatus(hit);
        } else sot.setHitStatus("");

        if (!sot.getRace().isDragonImmune() && !sot.getPersonality().isDragonImmune()) {
            matcher = dragonPattern.matcher(text);
            if (matcher.find()) {
                Dragon dragon = dragonDAOProvider.get().getDragon(matcher.group("dragon"));
                province.getKingdom().setDragon(dragon);
            } else {
                province.getKingdom().setDragon(null);
            }
        }

        matcher = exportLinePattern.matcher(text);
        if (matcher.find()) {
            sot.setExportLine(matcher.group(1).trim());
        } else sot.setExportLine(null);

        sot.setSavedBy(savedBy);

        return sot;
    }

    @Override
    public String getIntelTypeHandled() {
        return SoT.class.getSimpleName();
    }
}
