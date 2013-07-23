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

package database.models;

import api.tools.text.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static api.tools.text.StringUtil.lowerCase;

public enum AidImportanceType {
    OFFERING_AID(Arrays.asList("offering", "offer", "spare")),
    LOW_PRIORITY_REQUEST(Arrays.asList("low")),
    MEDIUM_PRIORITY_REQUEST(Arrays.asList("med", "medium")),
    HIGH_PRIORITY_REQUEST(Arrays.asList("high", "urgent", "now"));

    private final List<String> aliases;

    AidImportanceType(final List<String> aliases) {
        this.aliases = aliases;
    }

    public String getTypeName() {
        return StringUtil.prettifyEnumName(this);
    }

    public static AidImportanceType fromName(final String name) {
        for (AidImportanceType value : values()) {
            if (value.aliases.contains(lowerCase(name)) || value.getTypeName().equalsIgnoreCase(name))
                return value;
        }
        return null;
    }

    public static String getGroup() {
        Collection<String> all = new ArrayList<>();
        for (AidImportanceType type : values()) {
            all.addAll(type.aliases);
        }
        return StringUtil.merge(all, '|');
    }
}
