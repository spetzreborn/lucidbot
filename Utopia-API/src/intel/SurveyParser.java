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
import api.tools.collections.MapFactory;
import api.tools.numbers.NumberUtil;
import api.tools.text.RegexUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.CommonEntitiesAccess;
import database.daos.ProvinceDAO;
import database.models.Building;
import database.models.Province;
import database.models.Survey;
import database.models.SurveyEntry;
import events.CacheReloadEvent;
import lombok.extern.log4j.Log4j;
import tools.parsing.UtopiaValidationType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static intel.SurveyEntryUtil.registerEntry;
import static intel.SurveyEntryUtil.removeEntryWithBuilding;

@Singleton
@Log4j
class SurveyParser implements IntelParser<Survey> {
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    private Pattern provincePattern;
    private Pattern buildingPattern;

    private Pattern identifierPattern;

    @Inject
    SurveyParser(final CommonEntitiesAccess commonEntitiesAccess, final Provider<ProvinceDAO> provinceDAOProvider,
                 final Provider<BotUserDAO> botUserDAOProvider, final EventBus eventBus) {
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        provincePattern = Pattern.compile("Our thieves scour the lands of ([^(]+)(" + UtopiaValidationType.KDLOC.getPatternString() +
                ") and learn");
        buildingPattern = Pattern.compile('(' + commonEntitiesAccess.getBuildingGroup() +
                ")\\s+((?:" + ValidationType.INT.getPattern() + "(?:\\s+|\\*))+)");

        identifierPattern = Pattern.compile("(?:(?:" + provincePattern.pattern() +
                ")|You will find that as we build more of certain building types(?=.*?Exploration/Construction Schedules))");
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
    public Survey parse(final String savedBy, final String text) throws Exception {
        Survey survey = new Survey();

        Matcher matcher = provincePattern.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            Province province = provinceDAOProvider.get().getOrCreateProvince(name, matcher.group(2));
            province.setName(name);
            if (province.getSurvey() != null) survey = province.getSurvey();
            else survey.setProvince(province);
        } else {
            BotUser user = botUserDAOProvider.get().getUser(savedBy);
            Province province = provinceDAOProvider.get().getProvinceForUser(user);
            if (province == null) throw new ParseException("Self survey contains no province name, and user has no registered province", 0);
            if (province.getSurvey() != null) survey = province.getSurvey();
            else survey.setProvince(province);
        }

        Map<String, Building> buildingsMap = MapFactory.newNameToObjectMapping(commonEntitiesAccess.getAllBuildings());
        Set<Building> usedBuildings = new HashSet<>();
        Set<SurveyEntry> registeredEntries = new HashSet<>();

        matcher = buildingPattern.matcher(text);
        while (matcher.find()) {
            Building building = buildingsMap.get(matcher.group(1));
            int amount = 0;
            SurveyEntry.SurveyEntryType type =
                    usedBuildings.contains(building) ? SurveyEntry.SurveyEntryType.IN_PROGRESS : SurveyEntry.SurveyEntryType.BUILT;
            usedBuildings.add(building);
            String[] split = RegexUtil.WHITESPACES_PATTERN.split(matcher.group(2));
            if (split.length == 1) {
                amount = NumberUtil.parseInt(split[0]);
            } else {
                for (String aSplit : split) {
                    amount += NumberUtil.parseInt(aSplit);
                }
            }
            if (amount > 0) {
                registeredEntries.add(registerEntry(survey, building, type, amount));
            }
        }

        Building unknownBuilding = buildingsMap.get("Unknown");
        for (Iterator<SurveyEntry> iter = survey.getBuildings().iterator(); iter.hasNext(); ) {
            SurveyEntry entry = iter.next();
            if (!entry.getBuilding().equals(unknownBuilding) && !registeredEntries.contains(entry)) iter.remove();
        }

        int known = 0;
        for (SurveyEntry entry : survey.getBuildings()) {
            if (!entry.getBuilding().equals(unknownBuilding)) known += entry.getValue();
        }
        int totalLand = survey.getProvince().getLand();
        if (totalLand - known > 0) {
            registerEntry(survey, unknownBuilding, SurveyEntry.SurveyEntryType.IN_PROGRESS, totalLand - known);
        } else removeEntryWithBuilding(survey, unknownBuilding);

        survey.setSavedBy(savedBy);
        survey.setLastUpdated(new Date());
        survey.setExportLine(null);

        return survey;
    }

    @Override
    public String getIntelTypeHandled() {
        return Survey.class.getSimpleName();
    }
}
