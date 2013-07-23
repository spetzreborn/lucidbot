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
class AngelSurveyParser implements IntelParser<Survey> {
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    private Pattern provinceNamePattern;
    private Pattern buildingsPattern;
    private Pattern buildingsInProgressPattern;
    private Pattern totalLandPattern;

    private Pattern identifierPattern;

    @Inject
    AngelSurveyParser(final CommonEntitiesAccess commonEntitiesAccess, final Provider<ProvinceDAO> provinceDAOProvider,
                      final Provider<BotUserDAO> botUserDAOProvider, final EventBus eventBus) {
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        provinceNamePattern = Pattern.compile("Buildings Report of ([^(]+)(" + UtopiaValidationType.KDLOC.getPatternString() + ')');
        Pattern selfSurveyPattern = Pattern.compile("Survey / Buildings Report Information");
        buildingsPattern = Pattern.compile('(' + commonEntitiesAccess.getBuildingGroup() +
                "): (" + ValidationType.INT.getPattern() + ')');
        buildingsInProgressPattern = Pattern.compile(buildingsPattern.pattern() + " \\([\\d.%]+\\) \\+ (" +
                ValidationType.INT.getPattern() + ") in progress");
        totalLandPattern = Pattern.compile("Total Land: (" + ValidationType.INT.getPattern() + ") Acres");

        identifierPattern = Pattern.compile('(' + provinceNamePattern.pattern() + '|' + selfSurveyPattern.pattern() +
                ")\\s*(?:\\[http://www.utopiatemple.com Angel|\\[http://www.thedragonportal.eu Ultima)");
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

        Matcher matcher = provinceNamePattern.matcher(text);
        if (matcher.find()) {
            ProvinceDAO provinceDao = provinceDAOProvider.get();
            String name = matcher.group(1).trim();
            Province province = provinceDao.getOrCreateProvince(name, matcher.group(2));
            province.setName(name);
            if (province.getSurvey() != null) survey = province.getSurvey();
            else survey.setProvince(province);
        } else {
            BotUser user = botUserDAOProvider.get().getUser(savedBy);
            Province province = provinceDAOProvider.get().getProvinceForUser(user);
            if (province == null)
                throw new ParseException("Self survey contains no province name, and user has no registered province", 0);
            if (province.getSurvey() != null) survey = province.getSurvey();
            else survey.setProvince(province);
        }

        Map<String, Building> buildingsMap = MapFactory.newNameToObjectMapping(commonEntitiesAccess.getAllBuildings());

        Set<SurveyEntry> registeredEntries = new HashSet<>();

        matcher = buildingsPattern.matcher(text);
        while (matcher.find()) {
            Building building = buildingsMap.get(matcher.group(1));
            int amount = NumberUtil.parseInt(matcher.group(2));
            registeredEntries.add(registerEntry(survey, building, SurveyEntry.SurveyEntryType.BUILT, amount));
        }

        matcher = buildingsInProgressPattern.matcher(text);
        while (matcher.find()) {
            Building building = buildingsMap.get(matcher.group(1));
            int amount = NumberUtil.parseInt(matcher.group(3));
            registeredEntries.add(registerEntry(survey, building, SurveyEntry.SurveyEntryType.IN_PROGRESS, amount));
        }

        Building unknownBuilding = buildingsMap.get("Unknown");
        Building barrenLands = buildingsMap.get("Barren Land");
        for (Iterator<SurveyEntry> iter = survey.getBuildings().iterator(); iter.hasNext(); ) {
            SurveyEntry entry = iter.next();
            if (!entry.getBuilding().equals(unknownBuilding) && !entry.getBuilding().equals(barrenLands) &&
                    !registeredEntries.contains(entry)) iter.remove();
        }

        matcher = totalLandPattern.matcher(text);
        boolean landPatternFound = matcher.find();
        int known = 0;
        for (SurveyEntry entry : survey.getBuildings()) {
            if (!entry.getBuilding().equals(unknownBuilding) && !entry.getBuilding().equals(barrenLands))
                known += entry.getValue();
        }
        if (landPatternFound) {
            int landFromSurvey = NumberUtil.parseInt(matcher.group(1));
            int barren = landFromSurvey - known;
            if (barren > 0) {
                registerEntry(survey, barrenLands, SurveyEntry.SurveyEntryType.BUILT, barren);
                known += barren;
            } else removeEntryWithBuilding(survey, barrenLands);
        } else removeEntryWithBuilding(survey, barrenLands);

        int totalLand = survey.getProvince().getLand();
        if (totalLand - known > 0) {
            registerEntry(survey, unknownBuilding, SurveyEntry.SurveyEntryType.IN_PROGRESS, totalLand - known);
        } else removeEntryWithBuilding(survey, unknownBuilding);

        matcher = exportLinePattern.matcher(text);
        if (matcher.find()) {
            survey.setExportLine(matcher.group(1));
        } else survey.setExportLine(null);
        survey.setSavedBy(savedBy);
        survey.setLastUpdated(new Date());

        return survey;
    }

    @Override
    public String getIntelTypeHandled() {
        return Survey.class.getSimpleName();
    }
}
