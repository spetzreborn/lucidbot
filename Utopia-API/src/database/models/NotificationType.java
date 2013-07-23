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

import api.common.HasName;
import api.tools.text.StringUtil;

public enum NotificationType implements HasName {
    ORDER_ADDED("orders"),
    AID_ADDED("aid"),
    NAP_ADDED("nap"),
    NOTE_ADDED("note"),
    BUILD_ADDED("build"),
    EVENT_ADDED("event added"),
    WAVE_ADDED("wave added"),
    TARGET_ADDED("target"),
    ARMY_HOME("army"),
    USER_CHECKIN("checkin"),
    WAIT("wait"),
    ALARM("alarm"),
    EVENT("event"),
    WAVE("wave");

    private final String name;

    NotificationType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public static NotificationType getByName(String name) {
        for (NotificationType notificationType : values()) {
            if (notificationType.name.equalsIgnoreCase(name)) return notificationType;
        }
        return null;
    }

    public static String getRegexGroup() {
        return StringUtil.mergeNamed(values(), '|');
    }
}
