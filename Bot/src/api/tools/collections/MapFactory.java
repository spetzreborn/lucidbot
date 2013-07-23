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

import api.common.HasName;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory class for Maps
 */
@ParametersAreNonnullByDefault
public class MapFactory {
    private MapFactory() {
    }

    /**
     * Takes objects and puts them into a new Map. Object 1 becomes a key, Object 2 the value for that key etc.
     * The amount of Objects must be even
     * <p/>
     * No arguments or a null causes an empty Map to be returned
     *
     * @param objects an even amount of objects, ordered in pairs
     * @return a new Map containing the specified objects
     * @throws IllegalArgumentException if there's an odd number of objects
     */
    public static Map<Object, Object> newMap(@Nullable final Object... objects) {
        if (objects == null || objects.length == 0) return new HashMap<>();
        if (objects.length % 2 != 0) throw new IllegalArgumentException("Needs to be an even number of objects");
        Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < objects.length; i += 2) {
            map.put(objects[i], objects[i + 1]);
        }
        return map;
    }

    /**
     * Creates a Map by pairing the received objects and using the String representation of the first
     * object in each pair as the key and the second object as the value.
     * The amount of objects has to be even, and each key-object is expected to either be a String or have a useful toString()
     * <p/>
     * No arguments or a null causes an empty Map to be returned
     *
     * @param objects an even amount of objects, ordered in pairs
     * @return a new Map containing the specified objects
     * @throws IllegalArgumentException if there's an odd number of objects
     */
    public static Map<String, Object> newMapWithNamedObjects(@Nullable final Object... objects) {
        if (objects == null || objects.length == 0) return new HashMap<>();
        if (objects.length % 2 != 0) throw new IllegalArgumentException("Needs to be an even number of name-object pairs");
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < objects.length; i += 2) {
            map.put(objects[i].toString(), objects[i + 1]);
        }
        return map;
    }

    /**
     * Maps the specified objects to their class types.
     * Null or no objects returns an empty map.
     *
     * @param objects the objects to map
     * @return a Map where the specified classes are mapped with their class types as keys
     */
    public static Map<Class<?>, Object> newClassToObjectMapping(@Nullable final Object... objects) {
        if (objects == null || objects.length == 0) return new HashMap<>();
        Map<Class<?>, Object> map = new HashMap<>();
        for (Object obj : objects) {
            map.put(obj.getClass(), obj);
        }
        return map;
    }

    /**
     * Takes objects implementing the HasName interface, and maps the names to the corresponding objects.
     * Example: SomeObject implements HasName. The key is then someObject.getName() and the value is someObject.
     * <p/>
     * A null value leads to an empty map being returned. Null values among the namedObjects are ignored.
     *
     * @param namedObjects the named objects to map
     * @param <E>          some type that implements the HasName interface
     * @return a Map with the specified objects mapped by their names
     */
    public static <E extends HasName> Map<String, E> newNameToObjectMapping(@Nullable final Iterable<E> namedObjects) {
        if (namedObjects == null) return new HashMap<>();
        Map<String, E> map = new HashMap<>();
        for (E obj : namedObjects) {
            if (obj != null) map.put(obj.getName(), obj);
        }
        return map;
    }
}
