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

import api.tools.numbers.NumberUtil;
import api.tools.text.StringUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of Params that is meant to be used for command parameters
 */
public class CommandParams implements Params {
    private final Map<String, String> map = new HashMap<>();

    public CommandParams(final Map<String, String> params) {
        map.putAll(params);
    }

    @Override
    public String getParameter(final String name) {
        String obj = map.get(name);
        return obj == null ? null : obj;
    }

    @Override
    public int getIntParameter(final String name) {
        String param = getParameter(name);
        if (param == null) return -1;
        return NumberUtil.parseInt(param);
    }

    @Override
    public double getDoubleParameter(final String name) {
        String param = getParameter(name);
        if (param == null) return -1;
        return NumberUtil.parseDouble(param);
    }

    @Override
    public boolean getBooleanParameter(final String name) {
        return Boolean.valueOf(getParameter(name));
    }

    @Override
    public int size() {
        int size = 0;
        for (String s : map.values()) {
            if (s != null) ++size;
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String[] getParameters(final String name) {
        String param = getParameter(name);
        if (param == null) return null;
        return StringUtil.splitOnSpace(param);
    }

    @Override
    public long getLongParameter(final String name) {
        String param = getParameter(name);
        if (param == null) return -1;
        return NumberUtil.parseLong(param);
    }

    @Override
    public boolean containsKey(final String key) {
        return map.containsKey(key) && map.get(key) != null;
    }
}
