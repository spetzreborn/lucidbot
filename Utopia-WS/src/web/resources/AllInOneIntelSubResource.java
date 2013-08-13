package web.resources;

import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import com.google.common.base.Function;
import com.google.inject.Provider;
import database.daos.IntelDAO;
import database.models.*;
import intel.Intel;
import intel.IntelParser;
import intel.IntelParserManager;
import lombok.extern.log4j.Log4j;
import web.models.*;
import web.tools.WebContext;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j
public class AllInOneIntelSubResource {

    private static final Map<Class<? extends Intel>, Function<Intel, Object>> converters = new HashMap<>();

    static {
        converters.put(Kingdom.class, new Function<Intel, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable final Intel intel) {
                return RS_Kingdom.fromKingdom((Kingdom) intel, true);
            }
        });
        converters.put(SoM.class, new Function<Intel, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable final Intel intel) {
                return RS_SoM.fromSoM((SoM) intel, true);
            }
        });
        converters.put(SoS.class, new Function<Intel, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable final Intel intel) {
                return RS_SoS.fromSoS((SoS) intel, true);
            }
        });
        converters.put(SoT.class, new Function<Intel, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable final Intel intel) {
                return RS_SoT.fromSoT((SoT) intel, true);
            }
        });
        converters.put(Survey.class, new Function<Intel, Object>() {
            @Nullable
            @Override
            public Object apply(@Nullable final Intel intel) {
                return RS_Survey.fromSurvey((Survey) intel, true);
            }
        });
    }

    private final IntelDAO intelDAO;
    private final Provider<IntelParserManager> intelParserManagerProvider;
    private final Provider<DelayedEventPoster> delayedEventPosterProvider;

    @Inject
    public AllInOneIntelSubResource(final IntelDAO intelDAO,
                                    final Provider<IntelParserManager> intelParserManagerProvider,
                                    final Provider<DelayedEventPoster> delayedEventPosterProvider) {
        this.intelDAO = intelDAO;
        this.intelParserManagerProvider = intelParserManagerProvider;
        this.delayedEventPosterProvider = delayedEventPosterProvider;
    }

    List<Object> addIntel(final String newSoT, final WebContext webContext) throws Exception {
        Map<String, IntelParser<?>> parsers = intelParserManagerProvider.get().getParsers(newSoT);
        if (parsers.isEmpty()) throw new IllegalArgumentException("Data is not parsable");

        BotUser botUser = webContext.getBotUser();

        List<Object> parsedObjects = new ArrayList<>();
        for (Map.Entry<String, IntelParser<?>> entry : parsers.entrySet()) {
            String rawIntel = entry.getKey();
            IntelParser<?> parser = entry.getValue();

            try {
                Intel parsedIntel = parser.parse(botUser.getMainNick(), rawIntel);
                intelDAO.saveIntel(parsedIntel, botUser.getId(), delayedEventPosterProvider.get());
                Function<Intel, Object> converter = converters.get(parsedIntel.getIntelType());
                parsedObjects.add(converter.apply(parsedIntel));
            } catch (Exception e) {
                AllInOneIntelSubResource.log.error("Failed to parse or save intel posted from web service", e);
            }
        }

        return parsedObjects;
    }

}