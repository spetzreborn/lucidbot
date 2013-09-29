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

package api.tools.files;

import api.filters.FilterEnabled;
import api.tools.collections.ArrayUtil;
import api.tools.common.ReflectionUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import spi.filters.Filter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static api.tools.collections.CollectionUtil.isEmpty;

@ParametersAreNonnullByDefault
public class FilterUtil {
    private FilterUtil() {
    }

    /**
     * Applies the specified filters on the collection. The specified collection is modified directly,
     * so if you want to keep the original, send in a copy.
     *
     * @param collection the collection to filter
     * @param filters    the filters to apply
     */
    public static void applyFilters(@Nullable final Collection<?> collection, @Nullable final Collection<Filter<?>> filters) {
        if (isEmpty(collection) || isEmpty(filters)) return;
        for (Filter<?> filter : filters) {
            filter.filter(collection);
        }
    }

    /**
     * Applies the specified filters on the collection. The specified collection is modified directly,
     * so if you want to keep the original, send in a copy.
     *
     * @param collection the collection to filter
     * @param filters    the filters to apply
     * @param returnType the return type of the method containing the second set to filter against
     */
    public static <E> void applyFilters(@Nullable final Collection<E> collection, @Nullable final Collection<Filter<?>> filters, final Class<?> returnType) {
        if (isEmpty(collection) || isEmpty(filters)) return;

        try {
            Class<?> colClass = collection.iterator().next().getClass();
            Method method = ReflectionUtil.getMethodWithReturnType(colClass, returnType);

            applyFilters(collection, filters);
            Multimap<Object, E> map = ArrayListMultimap.create();
            for (E obj : collection) {
                map.put(method.invoke(obj), obj);
            }
            applyFilters(map.keySet(), filters);
            collection.clear();
            collection.addAll(map.values());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Searches for a method or field that specifies itself as filter enabled for the specified type of filter, and then
     * returns the value in case a matching method or field is found.
     *
     * @param obj         the object to traverse
     * @param filterType  the type of filter
     * @param valueType   the type of value
     * @param paramValues params a prospective method might require
     * @param <E>         .
     * @return the value of the filter enabled method or field, or null if no match is found
     */
    public static <E> FilterValue<E> getFilterEnabledMethodOrFieldValue(final Object obj, final Class<? extends Filter<?>> filterType,
                                                                        final Class<E> valueType, final Object... paramValues) {
        FilterValue<E> val = getFilterEnabledMethodValue(obj, filterType, valueType, paramValues);
        if (!val.isFilterValueFound()) val = getFilterEnabledFieldValue(obj, filterType, valueType);
        return val;
    }

    /**
     * Searches for a method that specifies itself as filter enabled for the specified type of filter, and then
     * returns the value in case a matching method is found.
     *
     * @param obj         the object to traverse
     * @param filterType  the type of filter
     * @param valueType   the type of value
     * @param paramValues params the method might require
     * @param <E>         .
     * @return a FilterValue object detailing the value that was found, if one was
     */
    public static <E> FilterValue<E> getFilterEnabledMethodValue(final Object obj, final Class<? extends Filter<?>> filterType,
                                                                 final Class<E> valueType, final Object... paramValues) {
        try {
            for (Method method : obj.getClass().getDeclaredMethods()) {
                if (hasCorrectFilterAnnotation(method, filterType) &&
                        Arrays.equals(method.getParameterTypes(), ArrayUtil.objectToTypeArray(paramValues))) {
                    method.setAccessible(true);
                    Object result = method.invoke(obj, paramValues);
                    return new FilterValue<>(result == null ? null : valueType.cast(result), true);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException ignore) {
        }
        return new FilterValue<>(null, false);
    }

    /**
     * Searches for a field that specifies itself as filter enabled for the specified type of filter, and then
     * returns the value in case a matching field is found.
     *
     * @param obj        the object to traverse
     * @param filterType the type of filter
     * @param valueType  the type of value
     * @param <E>        .
     * @return a FilterValue object detailing the value that was found, if one was
     */
    public static <E> FilterValue<E> getFilterEnabledFieldValue(final Object obj, final Class<? extends Filter<?>> filterType,
                                                                final Class<E> valueType) {
        try {
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (hasCorrectFilterAnnotation(field, filterType)) {
                    field.setAccessible(true);
                    Object result = field.get(obj);
                    return new FilterValue(result == null ? null : valueType.cast(result), true);
                }
            }
        } catch (IllegalAccessException ignore) {
        }
        return new FilterValue<>(null, false);
    }

    @Getter
    public static class FilterValue<T> {
        private final T value;
        private final boolean filterValueFound;

        public FilterValue(final T value, final boolean filterValueFound) {
            this.value = value;
            this.filterValueFound = filterValueFound;
        }
    }

    private static boolean hasCorrectFilterAnnotation(final AnnotatedElement element, final Class<? extends Filter<?>> filterType) {
        FilterEnabled annotation = element.getAnnotation(FilterEnabled.class);
        return annotation != null && annotation.value().equals(filterType);
    }
}
