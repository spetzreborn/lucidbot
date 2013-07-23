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

import api.tools.collections.ArrayUtil;
import api.tools.common.ReflectionUtil;
import com.google.inject.Provider;
import database.daos.KingdomDAO;
import database.models.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import tools.BestMatchFinder;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A manager class for intel related functionality
 */
public class IntelManager {
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final BestMatchFinder bestMatchFinder;

    @Inject
    public IntelManager(final Provider<KingdomDAO> kingdomDAOProvider, final BestMatchFinder bestMatchFinder) {
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.bestMatchFinder = bestMatchFinder;
    }

    /**
     * Finds the best matching province given the specified searchTerm. Checks for both province names and nicknames.
     *
     * @param searchTerm the search term
     * @return the best matching province, which is either a province that matches the name or the province belonging to some user who has
     *         a nick that matched the search term
     */
    public Province getBestMatch(final String searchTerm) {
        return bestMatchFinder.findBestMatch(searchTerm);
    }

    /**
     * Retrieves the specified type of resource for all the provinces in the specified kd
     *
     * @param resourceType the type of resource to retrieve
     * @param location     the location of the kd
     * @param methodParams any parameters needed to resolve the value of the resource
     * @return a List of key value pairs which are resource type mapped to resource value
     * @throws InvocationTargetException .
     * @throws IllegalAccessException    .
     */
    public List<ProvinceAndResource> getIntelValueForKD(final ProvinceResourceType resourceType, final String location,
                                                        final Object... methodParams) throws InvocationTargetException,
            IllegalAccessException {
        ClassMethodAndUpdatedInfo resourceCarrierAndMethod = getValueCarryingTypeAndMethod(resourceType,
                ArrayUtil.objectToTypeArray(methodParams));
        Kingdom kd = kingdomDAOProvider.get().getKingdom(location);

        if (kd == null) return Collections.emptyList();

        Method intelSourceMethod = getIntelSourceMethod(Kingdom.class, resourceCarrierAndMethod.getClazz());
        return createProvinceValuePairs((List<?>) intelSourceMethod.invoke(kd), resourceCarrierAndMethod.getMethod(), methodParams);
    }

    /**
     * Retrieves the specified type of resource for the specified province (or user's province)
     *
     * @param resourceType   the type of resource to retrieve
     * @param nickOrProvince the province, or the name of the province's owner
     * @param methodParams   any parameters needed to resolve the value of the resource
     * @return a key value pair which is resource type mapped to resource value
     * @throws InvocationTargetException .
     * @throws IllegalAccessException    .
     */
    public ProvinceAndResource getIntelValueForProvince(final ProvinceResourceType resourceType, final String nickOrProvince,
                                                        final Object... methodParams) throws InvocationTargetException,
            IllegalAccessException {
        ClassMethodAndUpdatedInfo resourceCarrierAndMethod = getValueCarryingTypeAndMethod(resourceType,
                ArrayUtil.objectToTypeArray(methodParams));
        Province province = getBestMatch(nickOrProvince);

        if (province == null) return null;

        Object intelSource = resourceCarrierAndMethod.getClazz().equals(Province.class) ? province : getIntelSourceMethod(Province.class,
                resourceCarrierAndMethod
                        .getClazz())
                .invoke(province);
        if (intelSource == null) return new ProvinceAndResource(province, null, null);

        List<ProvinceAndResource> provinceValuePairs = createProvinceValuePairs(Arrays.asList(intelSource),
                resourceCarrierAndMethod.getMethod(), methodParams);
        return provinceValuePairs.isEmpty() ? null : provinceValuePairs.get(0);
    }

    private static final List<Class<?>> VALUE_CARRYING_CLASSES = new ArrayList<Class<?>>(
            Arrays.asList(Province.class, SoT.class, SoM.class, SoS.class, Survey.class));

    private static ClassMethodAndUpdatedInfo getValueCarryingTypeAndMethod(final ProvinceResourceType resourceType,
                                                                           final Class<?>... paramTypes) {
        for (Class<?> valueCarryingClass : VALUE_CARRYING_CLASSES) {
            Method method = getMethodForResourceType(valueCarryingClass, resourceType, paramTypes);

            if (method != null) return new ClassMethodAndUpdatedInfo(valueCarryingClass, method);
        }
        throw new IllegalArgumentException("No method could be found in any known intel-carrying class to server that command");
    }

    private static Method getMethodForResourceType(Class<?> clazz, ProvinceResourceType resourceType, Class<?>... paramTypes) {
        for (Method method : clazz.getDeclaredMethods()) {
            ProvinceResourceProvider annotation = method.getAnnotation(ProvinceResourceProvider.class);
            if (annotation != null && annotation.value() == resourceType && Arrays.equals(method.getParameterTypes(), paramTypes)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static List<ProvinceAndResource> createProvinceValuePairs(final List<?> items, final Method method,
                                                                      final Object... params) throws InvocationTargetException,
            IllegalAccessException {
        List<ProvinceAndResource> out = new ArrayList<>(items.size());
        Object value;
        Province province;
        for (Object obj : items) {
            if (obj instanceof Province) province = (Province) obj;
            else if (obj instanceof ProvinceIntel) province = ((ProvinceIntel) obj).getProvince();
            else throw new IllegalArgumentException("items List contains unknown objects");
            value = method.invoke(obj, params);

            ProvinceResourceProvider annotation = method.getAnnotation(ProvinceResourceProvider.class);
            String updatedMethod = annotation.lastUpdatedMethod();
            Date lastUpdated = ReflectionUtil.invokeMethod(updatedMethod, obj, Date.class);

            if (value != null && lastUpdated != null) out.add(new ProvinceAndResource(province, value, lastUpdated));
        }
        return out;
    }

    /**
     * Retrieves the method that may be used to resolve the specified value carrying type
     *
     * @param baseClass         the class to find the method in
     * @param valueCarryingType the type of value the method wants to resolve
     * @return the matching method
     */
    public static Method getIntelSourceMethod(final Class<?> baseClass, final Class<?> valueCarryingType) {
        for (Method method : baseClass.getDeclaredMethods()) {
            IntelSourceProvider annotation = method.getAnnotation(IntelSourceProvider.class);
            if (annotation != null && annotation.value().equals(valueCarryingType)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new IllegalArgumentException("Base class does not contain such a method");
    }

    @Getter
    @AllArgsConstructor
    private static class ClassMethodAndUpdatedInfo {
        private final Class<?> clazz;
        private final Method method;
    }
}
