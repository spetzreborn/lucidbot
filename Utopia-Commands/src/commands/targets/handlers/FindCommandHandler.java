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

package commands.targets.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.events.DelayedEventPoster;
import api.filters.SortEnabled;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import api.tools.collections.Params;
import api.tools.compare.DynamicComparator;
import api.tools.numbers.NumberUtil;
import api.tools.text.StringUtil;
import api.tools.time.TimeUtil;
import database.daos.ProvinceDAO;
import database.models.Province;
import filtering.filters.KingdomLocationFilter;
import lombok.extern.log4j.Log4j;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import static tools.UtopiaPropertiesConfig.FINDER_MAX_RESULTS;
import static tools.UtopiaPropertiesConfig.INTRA_KD_LOC;

@Log4j
public class FindCommandHandler implements CommandHandler {
    private final ProvinceDAO provinceDAO;
    private final PropertiesCollection properties;

    @Inject
    public FindCommandHandler(final ProvinceDAO provinceDAO, final PropertiesCollection properties) {
        this.provinceDAO = provinceDAO;
        this.properties = properties;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            if (filters.isEmpty()) return CommandResponse.errorResponse("Syntax error, no valid filters found");

            //0. Filter away self kd unless it's already handled in the filters list
            Filter<?> selfKDRemovalFilter = new KingdomLocationFilter.Builder().parseAndBuild('!' + properties.get(INTRA_KD_LOC));
            if (!filters.contains(selfKDRemovalFilter)) filters.add(selfKDRemovalFilter);

            //1. Get all provinces that pass the filters
            List<Province> provinces = provinceDAO.getProvincesPassingFilters(filters);
            if (provinces.isEmpty())
                return CommandResponse.errorResponse("No provinces matched the specified criteria");

            //2. Apply any sorting specified
            List<ProvinceWithResource> provinceWithResources = new ArrayList<>(provinces.size());
            boolean hasSortingSpecified = params.containsKey("sorting");
            if (hasSortingSpecified) {
                initLazyCollections(provinceWithResources);
                String sorting = StringUtil.splitOnSpace(params.getParameter("sorting"))[1];
                Method sortingMethod = getSortingMethod(sorting);
                if (sortingMethod == null)
                    return CommandResponse.errorResponse("Syntax error, sorting option not recognized");
                sortingMethod.setAccessible(true);
                Collections.sort(provinces, new DynamicComparator<Province>(sortingMethod));
                if ("desc".equalsIgnoreCase(params.getParameter("sortingSpec"))) Collections.reverse(provinces);
                try {
                    for (Province province : provinces) {
                        Object value = sortingMethod.invoke(province);
                        if (value != null)
                            provinceWithResources.add(new ProvinceWithResource(province, formatSortValue(value)));
                    }
                } catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
                    FindCommandHandler.log.error("Couldn't get sorting values to display in the find command", e);
                }
            }

            //3. Cut off the list if there are too many results
            int maxResults = properties.getInteger(FINDER_MAX_RESULTS);
            if (params.containsKey("limit")) {
                String limitSpec = params.getParameter("limit");
                maxResults = Math.min(maxResults, NumberUtil.parseInt(StringUtil.splitOnSpace(limitSpec)[1]));
            }
            if (!hasSortingSpecified && maxResults < provinces.size()) {
                provinces = new ArrayList<>(provinces.subList(0, maxResults));
            }
            if (hasSortingSpecified && maxResults < provinceWithResources.size()) {
                provinceWithResources = new ArrayList<>(provinceWithResources.subList(0, maxResults));
            }


            return CommandResponse.resultResponse("found", hasSortingSpecified ? provinceWithResources : provinces, "hasSortingSpecified",
                    hasSortingSpecified);
        } catch (SecurityException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static void initLazyCollections(final List<ProvinceWithResource> provinceWithResources) {
        for (ProvinceWithResource provinceWithResource : provinceWithResources) {
            Province province = provinceWithResource.getProvince();
            if (province.getRace() != null) {
                province.getRace().getBonuses();
                province.getRace().getSpellbook();
            }
        }
    }

    private static Method getSortingMethod(final String name) {
        for (Method method : Province.class.getDeclaredMethods()) {
            SortEnabled annotation = method.getAnnotation(SortEnabled.class);
            if (annotation != null && name.matches(annotation.value())) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static String formatSortValue(final Object value) {
        if (value instanceof String) {
            return value.toString();
        } else if (value instanceof Double) {
            NumberFormat formatter = DecimalFormat.getInstance(Locale.US);
            formatter.setMaximumFractionDigits(2);
            formatter.setMinimumFractionDigits(0);
            return formatter.format(value);
        } else if (value instanceof Long || value instanceof Integer) {
            NumberFormat formatter = DecimalFormat.getIntegerInstance(Locale.US);
            return formatter.format(value);
        } else if (value instanceof Date) {
            return TimeUtil.compareDateToCurrent((Date) value);
        }
        throw new IllegalArgumentException("Unknown type for value: " + value);
    }

    public static class ProvinceWithResource {
        private final Province province;
        private final String resource;

        public ProvinceWithResource(final Province province, final String resource) {
            this.province = province;
            this.resource = resource;
        }

        public Province getProvince() {
            return province;
        }

        public String getResource() {
            return resource;
        }
    }
}
