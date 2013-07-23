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

import api.tools.collections.CommandParams;
import api.tools.text.StringUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Test
public class CommandParamsTest {
    public void testCommandParams() {
        Map<String, String> map = new HashMap<>();
        map.put("int", "1");
        map.put("notint", "baloo");
        map.put("string", "something");
        map.put("long", "100000000000000000");
        map.put("notlong", "dfssdfsdf");
        map.put("boolean", "true");
        map.put("notboolean", "1");
        map.put("double", "42.05234");
        map.put("notdouble", "yrtsfdfs");
        map.put("array", "true false apa bapa");
        map.put("null", null);
        CommandParams commandParams = new CommandParams(map);
        Assert.assertFalse(commandParams.isEmpty());
        Assert.assertEquals(commandParams.size(), map.size());

        Assert.assertEquals(commandParams.getIntParameter("int"), Integer.parseInt(map.get("int")));
        Assert.assertEquals(commandParams.getIntParameter("null"), -1);
        Assert.assertEquals(commandParams.getParameter("string"), map.get("string"));
        Assert.assertNull(commandParams.getParameter("null"));
        Assert.assertEquals(commandParams.getLongParameter("long"), Long.parseLong(map.get("long")));
        Assert.assertEquals(commandParams.getLongParameter("null"), -1);
        Assert.assertEquals(commandParams.getBooleanParameter("boolean"), true);
        Assert.assertEquals(commandParams.getBooleanParameter("null"), false);
        Assert.assertEquals(commandParams.getBooleanParameter("notboolean"), false);
        Assert.assertEquals(commandParams.getDoubleParameter("double"), Double.parseDouble(map.get("double")));
        Assert.assertEquals(commandParams.getDoubleParameter("null"), -1d);
        Assert.assertEquals(commandParams.getParameters("array"), StringUtil.splitOnSpace(map.get("array")));
        Assert.assertNull(commandParams.getParameters("null"));

        Assert.assertFalse(commandParams.containsKey("null"));
        Assert.assertFalse(commandParams.containsKey("nonexistant"));
        Assert.assertTrue(commandParams.containsKey("int"));

        try {
            commandParams.getIntParameter("notint");
            Assert.fail();
        } catch (Exception ígnore) {
        }

        try {
            commandParams.getLongParameter("notlong");
            Assert.fail();
        } catch (Exception ígnore) {
        }

        try {
            commandParams.getDoubleParameter("notdouble");
            Assert.fail();
        } catch (Exception ígnore) {
        }

        Assert.assertTrue(new CommandParams(Collections.<String, String>emptyMap()).isEmpty());
    }
}
