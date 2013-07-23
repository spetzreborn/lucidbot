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

package tools.collections;

import api.common.HasName;
import api.tools.collections.MapFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Test
public class MapFactoryTest {
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateMapNonEventAmount() {
        MapFactory.newMap(new Object());
    }

    public void testCreateMap() {
        String string = "string";
        Integer integer = -5;
        Map<Object, Object> map = new HashMap<>();
        map.put(string, integer);
        Assert.assertEquals(MapFactory.newMap(string, integer), map);
        Assert.assertTrue(MapFactory.newMap().isEmpty());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCreateMapWithNamedObjectsNonEventAmount() {
        MapFactory.newMapWithNamedObjects(new Object());
    }

    public void testCreateMapWithNamedObjects() {
        String string = "string";
        Integer integer = -5;
        Map<Object, Object> map = new HashMap<>();
        map.put(string, integer);
        Assert.assertEquals(MapFactory.newMapWithNamedObjects(string, integer), map);
        Assert.assertTrue(MapFactory.newMapWithNamedObjects().isEmpty());
    }

    public void testCreateClassToObjectMapping() {
        String string = "string";
        Integer integer = -5;
        Map<Object, Object> map = new HashMap<>();
        map.put(String.class, string);
        map.put(Integer.class, integer);
        Assert.assertEquals(MapFactory.newClassToObjectMapping(string, integer), map);
        Assert.assertTrue(MapFactory.newClassToObjectMapping().isEmpty());
        Assert.assertTrue(MapFactory.newClassToObjectMapping(null).isEmpty());
    }

    public void testCreateNameToObjectMapping() {
        Assert.assertTrue(MapFactory.newNameToObjectMapping(null).isEmpty());
        Assert.assertTrue(MapFactory.newNameToObjectMapping(new ArrayList<HasName>()).isEmpty());


        SomeObject object1 = new SomeObject("1");
        SomeObject object2 = new SomeObject("2");
        SomeObject object3 = new SomeObject("3");
        Map<String, SomeObject> map = new HashMap<>();
        map.put(object1.getName(), object1);
        map.put(object2.getName(), object2);
        map.put(object3.getName(), object3);

        Assert.assertEquals(MapFactory.newNameToObjectMapping(map.values()), map);
    }

    private static class SomeObject implements HasName {
        private final String name;

        private SomeObject(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
