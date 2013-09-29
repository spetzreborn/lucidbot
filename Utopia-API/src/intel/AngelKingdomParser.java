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
import database.daos.KingdomDAO;
import database.daos.ProvinceDAO;
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
class AngelKingdomParser implements IntelParser<Kingdom> {
    private static final String LAND_MARKER = "** Land **";
    private static final String NETWORTH_MARKER = "** Networth **";
    private static final String RANKS_MARKER = "** Ranks **";

    private final CommonEntitiesAccess commonEntitiesAccess;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;

    private Pattern nameAndLocPattern;
    private Pattern amountOfProvsPattern;
    private Pattern landProv;
    private Pattern nwProv;
    private Pattern titleProv;

    @Inject
    AngelKingdomParser(final CommonEntitiesAccess commonEntitiesAccess,
                       final Provider<KingdomDAO> kingdomDAOProvider,
                       final Provider<ProvinceDAO> provinceDAOProvider,
                       final EventBus eventBus) {
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        nameAndLocPattern = Pattern.compile("^([^ ].+?) (" + UtopiaValidationType.KDLOC.getPatternString() +
                ") Kingdom Analysis");
        amountOfProvsPattern = Pattern.compile("Provinces in Kingdom: (" + ValidationType.INT.getPattern() + ')');
        landProv = Pattern.compile("\\d{1,2}\\. (.+?) \\[(" + this.commonEntitiesAccess.getRaceGroup() +
                ")\\] - (" + ValidationType.INT.getPattern() + ") Acres \\([^)]+\\)");
        nwProv = Pattern.compile("\\d{1,2}\\. (.+?) \\[(" + this.commonEntitiesAccess.getRaceGroup() +
                ")\\] - (" + ValidationType.INT.getPattern() + ")gc \\([^)]+\\)");
        titleProv = Pattern.compile("\\d{1,2}\\. (.+?) \\[(" + this.commonEntitiesAccess.getRaceGroup() +
                ")\\] - (" + this.commonEntitiesAccess.getHonorTitleGroup() + ')');
    }

    @Subscribe
    public void onCacheReload(final CacheReloadEvent event) {
        compilePatterns();
    }

    @Override
    public Pattern getIdentifierPattern() {
        return nameAndLocPattern;
    }

    @Override
    public Kingdom parse(final String savedBy, final String text) throws ParseException {
        Kingdom kingdom = getOrCreateKingdom(text);

        Map<String, Province> provs = mapExistingProvincesByName(kingdom);

        int amountOfProvs = getExpectedAmountOfProvinces(text);

        ProvinceDAO provinceDAO = provinceDAOProvider.get();
        Set<Province> noLongerPresentProvinces = new HashSet<>(kingdom.getProvinces());

        String remainingText = parseLandSection(text, kingdom, provs, noLongerPresentProvinces, provinceDAO);
        remainingText = parseNetworthSection(provs, remainingText);
        parseHonorRanksSection(provs, remainingText);

        if (provs.size() < amountOfProvs)
            throw new ParseException("Only found " + provs.size() + " provinces, although " + "there should have been " + amountOfProvs, 0);

        provinceDAO.delete(noLongerPresentProvinces);

        kingdom.setSavedBy(savedBy);
        kingdom.setLastUpdated(new Date());
        return kingdom;
    }

    private static Map<String, Province> mapExistingProvincesByName(final Kingdom kingdom) {
        Map<String, Province> provs = new HashMap<>();
        for (Province province : kingdom.getProvinces()) {
            provs.put(lowerCase(province.getName()), province);
        }
        return provs;
    }

    private Kingdom getOrCreateKingdom(final String text) throws ParseException {
        Matcher matcher = nameAndLocPattern.matcher(text);
        if (matcher.find()) {
            Kingdom kingdom = kingdomDAOProvider.get().getOrCreateKingdom(matcher.group(2));
            kingdom.setName(matcher.group(1));
            return kingdom;
        } else throw new ParseException("KD to be parsed does not contain name and location", 0);
    }

    private String parseLandSection(final String text,
                                    final Kingdom kingdom,
                                    final Map<String, Province> provs,
                                    final Set<Province> removedProvinces,
                                    final ProvinceDAO provinceDAO) {
        String remainingText = text.substring(text.indexOf(LAND_MARKER));
        Matcher matcher = landProv.matcher(remainingText);
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
            Race race = commonEntitiesAccess.getRace(matcher.group(2));
            province.setRace(race);
            province.setLand(NumberUtil.parseInt(matcher.group(3)));
        }
        return remainingText;
    }

    private String parseNetworthSection(final Map<String, Province> provs, String remainingText) {
        remainingText = remainingText.substring(remainingText.indexOf(NETWORTH_MARKER));
        Matcher matcher = nwProv.matcher(remainingText);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String lcName = lowerCase(name);
            if (provs.containsKey(lcName)) {
                provs.get(lcName).setNetworth(NumberUtil.parseInt(matcher.group(3)));
            }
        }
        return remainingText;
    }

    private void parseHonorRanksSection(final Map<String, Province> provs, String remainingText) {
        remainingText = remainingText.contains(RANKS_MARKER) ? remainingText.substring(remainingText.indexOf(RANKS_MARKER)) : "";
        Matcher matcher = titleProv.matcher(remainingText);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String lcName = lowerCase(name);
            if (provs.containsKey(lcName)) {
                provs.get(lcName).setHonorTitle(commonEntitiesAccess.getHonorTitle(matcher.group(3)));
            }
        }
    }

    private int getExpectedAmountOfProvinces(final String text) {
        Matcher matcher = amountOfProvsPattern.matcher(text);
        return matcher.find() ? NumberUtil.parseInt(matcher.group(1)) : 0;
    }

    @Override
    public String getIntelTypeHandled() {
        return Kingdom.class.getSimpleName();
    }
}
