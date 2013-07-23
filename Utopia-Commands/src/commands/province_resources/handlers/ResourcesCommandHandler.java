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

package commands.province_resources.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import api.tools.collections.CollectionUtil;
import api.tools.collections.Params;
import api.tools.compare.DynamicComparator;
import api.tools.files.FilterUtil;
import com.google.common.collect.Lists;
import database.models.Province;
import intel.IntelManager;
import intel.ProvinceAndResource;
import intel.ProvinceResourceType;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.UtopiaPropertiesConfig;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ResourcesCommandHandler implements CommandHandler {
    private final PropertiesCollection properties;
    private final IntelManager intelManager;
    private final ProvinceResourceType resourceType;

    public ResourcesCommandHandler(final PropertiesCollection properties, final ProvinceResourceType resourceType,
                                   final IntelManager intelManager) {
        this.properties = properties;
        this.resourceType = resourceType;
        this.intelManager = intelManager;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Object[] details =
                    resourceType == ProvinceResourceType.BUILDING_PERCENTAGE ? new Object[]{context.getCommand().getName()} : new Object[0];
            if (params.isEmpty() || params.containsKey("location") && params.size() == 1) {
                String location = params.isEmpty() ? properties.get(UtopiaPropertiesConfig.INTRA_KD_LOC) : params.getParameter("location");
                List<ProvinceAndResource> intelValueForKD = new LinkedList<>(
                        intelManager.getIntelValueForKD(resourceType, location, details));
                if (CollectionUtil.isNotEmpty(filters)) filterResults(intelValueForKD, filters);
                DynamicComparator<ProvinceAndResource> comparator = new DynamicComparator<>(
                        ProvinceAndResource.class.getDeclaredMethod("getResource"));
                Collections.sort(intelValueForKD, comparator);
                Collections.reverse(intelValueForKD);

                int sum = 0;
                if (resourceType.shouldSum()) {
                    for (ProvinceAndResource provinceAndResource : intelValueForKD) {
                        sum += ((Number) provinceAndResource.getResource()).intValue();
                    }
                }

                return intelValueForKD.isEmpty() ? CommandResponse.errorResponse("No matches found")
                        : CommandResponse.resultResponse("provincesWithResources", intelValueForKD, "sum", sum);
            } else {
                ProvinceAndResource provinceIntel = intelManager
                        .getIntelValueForProvince(resourceType, params.getParameter("nickOrProvince").trim(), details);
                if (provinceIntel == null) return CommandResponse.errorResponse("Found no intel");
                else if (provinceIntel.getResource() == null) return CommandResponse.errorResponse("Lacking intel");
                else return CommandResponse.resultResponse("provincesWithResources", Lists.newArrayList(provinceIntel));
            }
        } catch (SecurityException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static void filterResults(final List<ProvinceAndResource> intelValueForKD, final Collection<Filter<?>> filters) {
        Set<Province> filteredProvinces = new HashSet<>();
        for (ProvinceAndResource provinceAndResource : intelValueForKD) {
            filteredProvinces.add(provinceAndResource.getProvince());
        }
        FilterUtil.applyFilters(filteredProvinces, filters);
        for (Iterator<ProvinceAndResource> iter = intelValueForKD.iterator(); iter.hasNext(); ) {
            ProvinceAndResource provinceAndResource = iter.next();
            if (!filteredProvinces.contains(provinceAndResource.getProvince())) iter.remove();
        }
    }
}
