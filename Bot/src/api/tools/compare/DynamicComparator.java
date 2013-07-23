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

package api.tools.compare;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dynamic comparator that uses reflection. Given a method, it can compare objects which implement that method.
 * <p/>
 * Currently supported return types for the method (including primitives):
 * - String
 * - Double
 * - Long
 * - Integer
 * - Date
 *
 * @param <E> type param
 */
@ParametersAreNonnullByDefault
public class DynamicComparator<E> implements Comparator<E> {
    private final Method method;

    public DynamicComparator(final Method method) {
        this.method = checkNotNull(method);
    }

    @Override
    public int compare(final E o1, final E o2) {
        try {
            Object first = method.invoke(o1);
            Object second = method.invoke(o2);

            if (first == null && second == null) return 0;
            else if (second == null) return -1;
            else if (first == null) return 1;

            if (first instanceof String) {
                return ((String) first).compareTo((String) second);
            } else if (first instanceof Double) {
                return ((Double) first).compareTo((Double) second);
            } else if (first instanceof Long) {
                return ((Long) first).compareTo((Long) second);
            } else if (first instanceof Integer) {
                return ((Integer) first).compareTo((Integer) second);
            } else if (first instanceof Date) {
                return ((Date) first).compareTo((Date) second);
            }
            throw new IllegalArgumentException("Unsupported comparison type: " + first.getClass().getSimpleName());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
