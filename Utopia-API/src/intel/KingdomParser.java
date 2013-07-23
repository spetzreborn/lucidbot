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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.CommonEntitiesAccess;
import database.daos.HonorTitleDAO;
import database.daos.KingdomDAO;
import database.daos.ProvinceDAO;
import database.daos.RaceDAO;
import database.models.HonorTitle;
import database.models.Kingdom;
import database.models.Province;
import database.models.Race;
import events.CacheReloadEvent;
import lombok.extern.log4j.Log4j;
import tools.parsing.UtopiaValidationType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.tools.text.StringUtil.lowerCase;

@Singleton
@Log4j
class KingdomParser implements IntelParser<Kingdom> {
    private final Provider<RaceDAO> raceDAOProvider;
    private final Provider<HonorTitleDAO> honorTitleDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    private Pattern nameLocPattern;
    private Pattern amountOfProvsPattern;
    private Pattern provListStart;
    private Pattern provincePattern;

    @Inject
    KingdomParser(final CommonEntitiesAccess commonEntitiesAccess, final Provider<HonorTitleDAO> honorTitleDAOProvider,
                  final Provider<RaceDAO> raceDAOProvider, final Provider<ProvinceDAO> provinceDAOProvider,
                  final Provider<KingdomDAO> kingdomDAOProvider, final EventBus eventBus) {
        this.honorTitleDAOProvider = honorTitleDAOProvider;
        this.raceDAOProvider = raceDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.commonEntitiesAccess = commonEntitiesAccess;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        nameLocPattern = Pattern.compile("Current kingdom is ([^(]+)(" + UtopiaValidationType.KDLOC.getPatternString() + ')');
        amountOfProvsPattern = Pattern.compile("Total Provinces\\s*(" + ValidationType.INT + ')');
        provListStart = Pattern.compile("Slot\\s+Province\\s+Race\\s+Land\\s+Net Worth\\s+Net Worth/Acre\\s+Nobility(.+)");
        provincePattern = Pattern.compile("\\d{1,2}(?!\\s+-\\s+-\\s+-)\\s+(.+?)(?:\\*|\\+|\\^| \\(M\\)| \\(S\\))*" +
                "\\s+(" + commonEntitiesAccess.getRaceGroup() + ')' +
                "\\s+(" + ValidationType.INT.getPattern() + ") acres" +
                "\\s+(" + ValidationType.INT.getPattern() + ")gc" +
                "\\s+" + ValidationType.INT.getPattern() + "gc" +
                "\\s+(" + commonEntitiesAccess.getHonorTitleGroup() + ')');
    }

    @Subscribe
    public void onCacheReload(final CacheReloadEvent event) {
        compilePatterns();
    }

    @Override
    public Pattern getIdentifierPattern() {
        return nameLocPattern;
    }

    @Override
    public Kingdom parse(final String savedBy, final String text) throws ParseException {
        Kingdom kingdom;

        Matcher matcher = nameLocPattern.matcher(text);
        if (matcher.find()) {
            kingdom = kingdomDAOProvider.get().getOrCreateKingdom(matcher.group(2));
            kingdom.setName(matcher.group(1).trim());
        } else throw new ParseException("KD to be parsed does not contain name and location", 0);

        Map<String, Province> provs = new HashMap<>();
        for (Province province : kingdom.getProvinces()) {
            provs.put(lowerCase(province.getName()), province);
        }
        Set<Province> removedProvinces = new HashSet<>(kingdom.getProvinces());

        matcher = amountOfProvsPattern.matcher(text);
        int amountOfProvs = matcher.find() ? NumberUtil.parseInt(matcher.group(1)) : 0;

        matcher = provListStart.matcher(text);
        if (!matcher.find()) throw new ParseException("KD pasted contained no province list", 0);

        String provList = matcher.group(1);

        ProvinceDAO provinceDAO = provinceDAOProvider.get();
        matcher = provincePattern.matcher(provList);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            Province province = provs.get(lowerCase(name));
            if (province == null) {
                Province existingInOtherKingdom = provinceDAO.getProvince(name);
                if (existingInOtherKingdom == null) province = provinceDAO.save(new Province(name, kingdom));
                else {
                    province = existingInOtherKingdom;
                    province.setKingdom(kingdom);
                }
                kingdom.getProvinces().add(province);
                provs.put(lowerCase(province.getName()), province);
            } else {
                province.setName(name);
                province.setKingdom(kingdom);
                removedProvinces.remove(province);
            }
            Race race = raceDAOProvider.get().getRace(matcher.group(2).trim());
            province.setRace(race);
            province.setLand(NumberUtil.parseInt(matcher.group(3).trim()));
            province.setNetworth(NumberUtil.parseInt(matcher.group(4).trim()));
            HonorTitle title = honorTitleDAOProvider.get().getHonorTitle(matcher.group(5).trim());
            province.setHonorTitle(title);
        }

        if (provs.size() < amountOfProvs)
            throw new ParseException("Only found " + provs.size() + " provinces, although " +
                    "there should have been " + amountOfProvs, 0);

        provinceDAO.delete(removedProvinces);

        kingdom.setSavedBy(savedBy);
        kingdom.setLastUpdated(new Date());
        return kingdom;
    }

    @Override
    public String getIntelTypeHandled() {
        return Kingdom.class.getSimpleName();
    }
}
