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
import database.models.Province;
import database.models.ScienceType;
import database.models.SoS;
import database.models.SoSEntry;
import events.CacheReloadEvent;
import lombok.extern.log4j.Log4j;
import tools.parsing.UtopiaValidationType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static intel.SoSEntryUtil.registerEntry;

@Singleton
@Log4j
class SoSParser implements IntelParser<SoS> {
    private static final String ALLOCATING_BOOKS = "Allocating Books";

    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    private Pattern provincePattern;
    private Pattern sciencePattern;
    private Pattern selfSciencePattern;

    private Pattern identifierPattern;

    @Inject
    SoSParser(final CommonEntitiesAccess commonEntitiesAccess, final Provider<ProvinceDAO> provinceDAOProvider,
              final Provider<BotUserDAO> botUserDAOProvider, final EventBus eventBus) {
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        provincePattern = Pattern.compile("Our thieves visit the research centers of ([^(]+)(" +
                UtopiaValidationType.KDLOC.getPatternString() + ')');
        sciencePattern = Pattern.compile('(' + commonEntitiesAccess.getScienceTypeGroup() +
                ")\\s+(" + ValidationType.INT.getPattern() + ").*?\\+(" +
                ValidationType.DOUBLE.getPattern() + ")%");
        selfSciencePattern = Pattern.compile('(' + commonEntitiesAccess.getScienceTypeGroup() +
                ")\\s+(" + ValidationType.INT.getPattern() + ").*?\\+(" +
                ValidationType.DOUBLE.getPattern() + ")% " +
                "(?:.*?(" + ValidationType.INT.getPattern() + "))?");

        identifierPattern = Pattern.compile("(?:(?:" + provincePattern.pattern() +
                ")|The Arts & Sciences are the heart and soul of our people and our lands)");
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
    public SoS parse(final String savedBy, final String text) throws Exception {
        SoS sos = new SoS();

        Matcher matcher = provincePattern.matcher(text);
        if (matcher.find()) {
            ProvinceDAO provinceDao = provinceDAOProvider.get();
            String name = matcher.group(1).trim();
            Province province = provinceDao.getOrCreateProvince(name, matcher.group(2));
            province.setName(name);
            if (province.getSos() != null) {
                sos = province.getSos();
            } else sos.setProvince(province);
        } else {
            BotUser user = botUserDAOProvider.get().getUser(savedBy);
            Province province = provinceDAOProvider.get().getProvinceForUser(user);
            if (province == null)
                throw new ParseException("Self sos contains no province name, and user has no registered province", 0);
            if (province.getSos() != null) sos = province.getSos();
            else sos.setProvince(province);
        }

        Map<String, ScienceType> sciences = MapFactory.newNameToObjectMapping(commonEntitiesAccess.getAllScienceTypes());
        Set<SoSEntry> entries = new HashSet<>();

        String booksText;
        if (text.contains(ALLOCATING_BOOKS)) booksText = text.substring(text.indexOf(ALLOCATING_BOOKS));
        else booksText = text;

        matcher = sos.getProvince().getProvinceOwner() == null ? sciencePattern.matcher(booksText) : selfSciencePattern.matcher(booksText);
        while (matcher.find()) {
            ScienceType type = sciences.get(matcher.group(1));
            int finishedBooks = NumberUtil.parseInt(matcher.group(2));
            double percent = NumberUtil.parseDouble(matcher.group(3));
            entries.add(registerEntry(sos, SoSEntry.SoSEntryType.BOOKS, type, finishedBooks));
            entries.add(registerEntry(sos, SoSEntry.SoSEntryType.EFFECT, type, percent));
            if (matcher.groupCount() == 4) {
                entries.add(registerEntry(sos, SoSEntry.SoSEntryType.BOOKS_IN_PROGRESS, type, NumberUtil.parseInt(matcher.group(4))));
            }
        }

        for (Iterator<SoSEntry> iter = sos.getSciences().iterator(); iter.hasNext(); ) {
            SoSEntry entry = iter.next();
            if (!entries.contains(entry)) iter.remove();
        }

        sos.setSavedBy(savedBy);
        sos.setLastUpdated(new Date());
        sos.calcTotalBooks();
        sos.setExportLine(null);

        return sos;
    }

    @Override
    public String getIntelTypeHandled() {
        return SoS.class.getSimpleName();
    }
}
