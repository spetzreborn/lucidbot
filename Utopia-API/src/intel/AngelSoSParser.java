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
class AngelSoSParser implements IntelParser<SoS> {
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final CommonEntitiesAccess commonEntitiesAccess;

    private Pattern provinceNamePattern;
    private Pattern sciencePercentPattern;
    private Pattern scienceInProgressPattern;
    private Pattern landPattern;

    private Pattern identifierPattern;

    @Inject
    AngelSoSParser(final CommonEntitiesAccess commonEntitiesAccess,
                   final Provider<ProvinceDAO> provinceDAOProvider,
                   final EventBus eventBus,
                   final Provider<BotUserDAO> botUserDAOProvider) {
        this.commonEntitiesAccess = commonEntitiesAccess;
        this.provinceDAOProvider = provinceDAOProvider;
        this.botUserDAOProvider = botUserDAOProvider;

        compilePatterns();
        eventBus.register(this);
    }

    private void compilePatterns() {
        provinceNamePattern = Pattern.compile("Science (?:Intelligence|Intel) on ([^(]+)(" + UtopiaValidationType.KDLOC.getPatternString() + ')');
        Pattern selfSosPattern = Pattern.compile("Science (?:Intelligence|Intel) Formatted Report");
        sciencePercentPattern = Pattern.compile("([0-9.]+)% (" + this.commonEntitiesAccess.getScienceTypeGroup() + ')' +
                "\\s*\\((" + ValidationType.INT.getPattern() + ") books");
        scienceInProgressPattern = Pattern.compile('(' + this.commonEntitiesAccess.getScienceTypeGroup() +
                "): (" + ValidationType.INT.getPattern() + ") books in progress");
        landPattern = Pattern.compile("Land: (" + ValidationType.INT.getPattern() + ") Acres");

        identifierPattern = Pattern.compile(
                '(' + provinceNamePattern.pattern() + '|' + selfSosPattern.pattern() + ")\\s*(?:\\[http://www.utopiatemple.com Angel|\\[http://www.thedragonportal.eu Ultima)");
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
        SoS sos = getOrCreateSoS(savedBy, text);

        Map<String, ScienceType> sciences = mapScienceTypesToNames();

        Set<SoSEntry> entries = new HashSet<>();
        parseEffects(text, sos, sciences, entries);
        parseInProgress(text, sos, sciences, entries);
        removeNonPresentEntries(sos, entries);

        parseLand(text, sos);

        parseExportLine(text, sos);

        sos.setSavedBy(savedBy);
        sos.setLastUpdated(new Date());
        sos.calcTotalBooks();

        return sos;
    }

    private SoS getOrCreateSoS(final String savedBy, final String text) throws ParseException {
        SoS sos = new SoS();

        Matcher matcher = provinceNamePattern.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            Province province = provinceDAOProvider.get().getOrCreateProvince(name, matcher.group(2));
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
        return sos;
    }

    private Map<String, ScienceType> mapScienceTypesToNames() {
        Map<String, ScienceType> sciences = new HashMap<>();
        for (ScienceType type : commonEntitiesAccess.getAllScienceTypes()) {
            sciences.put(type.getName(), type);
            sciences.put(type.getAngelName(), type);
        }
        return sciences;
    }

    private void parseEffects(final String text, final SoS sos, final Map<String, ScienceType> sciences, final Set<SoSEntry> entries) {
        Matcher matcher = sciencePercentPattern.matcher(text);
        while (matcher.find()) {
            entries.add(registerEntry(sos, SoSEntry.SoSEntryType.BOOKS, sciences.get(matcher.group(2)),
                    NumberUtil.parseDouble(matcher.group(3))));
            entries.add(registerEntry(sos, SoSEntry.SoSEntryType.EFFECT, sciences.get(matcher.group(2)),
                    NumberUtil.parseDouble(matcher.group(1))));
        }
    }

    private void parseInProgress(final String text, final SoS sos, final Map<String, ScienceType> sciences, final Set<SoSEntry> entries) {
        Matcher matcher = scienceInProgressPattern.matcher(text);
        while (matcher.find()) {
            entries.add(registerEntry(sos, SoSEntry.SoSEntryType.BOOKS_IN_PROGRESS, sciences.get(matcher.group(1)),
                    NumberUtil.parseDouble(matcher.group(2))));
        }
    }

    private static void removeNonPresentEntries(final SoS sos, final Set<SoSEntry> entries) {
        for (Iterator<SoSEntry> iter = sos.getSciences().iterator(); iter.hasNext(); ) {
            SoSEntry entry = iter.next();
            if (!entries.contains(entry)) iter.remove();
        }
    }

    private void parseLand(final String text, final SoS sos) {
        Matcher matcher = landPattern.matcher(text);
        if (matcher.find()) {
            String land = matcher.group(1).replace(",", "").trim();
            sos.getProvince().setLand(NumberUtil.parseInt(land));
        }
    }

    private static void parseExportLine(final String text, final SoS sos) {
        Matcher matcher = exportLinePattern.matcher(text);
        if (matcher.find()) {
            sos.setExportLine(matcher.group(1));
        } else sos.setExportLine(null);
    }

    @Override
    public String getIntelTypeHandled() {
        return SoS.class.getSimpleName();
    }
}
