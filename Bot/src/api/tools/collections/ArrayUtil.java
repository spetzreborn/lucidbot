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

package api.tools.collections;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class ArrayUtil {
    private ArrayUtil() {
    }

    public static <E> boolean isEmpty(final E[] array) {
        E[] arrayWithoutNulls = removeNulls(array);
        return arrayWithoutNulls.length == 0;
    }

    public static <E> boolean isNotEmpty(final E[] array) {
        return !isEmpty(array);
    }

    public static <E> E[] removeNulls(final E[] array) {
        List<E> list = new ArrayList<>();
        if (array != null) {
            for (E object : array) {
                if (object != null) list.add(object);
            }
        }
        return (E[]) list.toArray();
    }

    public static <E> boolean containsNull(final E[] array) {
        for (E e : array) {
            if (e == null) return true;
        }
        return false;
    }

    /**
     * Checks if the supplied array contains the specified object. Uses equals() for matching.
     *
     * @param array  the array
     * @param object the object to match
     * @param <E>    the type of objects
     * @return true of the array contains the object
     */
    public static <E> boolean contains(final E[] array, @Nullable final E object) {
        for (E e : array) {
            if (e.equals(object)) return true;
        }
        return false;
    }

    /**
     * Creates an array of Class objects for the specified objects
     *
     * @param array the objects
     * @return a Class[] for the specified objects
     */
    public static Class<?>[] objectToTypeArray(final Object... array) {
        if (array.length == 0) return new Class<?>[0];
        List<Class<?>> list = new ArrayList<>(array.length);
        for (Object obj : array) {
            list.add(obj.getClass());
        }
        return list.toArray(new Class<?>[list.size()]);
    }
}
