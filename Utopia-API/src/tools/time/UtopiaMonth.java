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

package tools.time;

import api.common.HasName;
import api.tools.text.StringUtil;

/**
 * Represents a utopian month
 */
public enum UtopiaMonth implements HasName {
    JANUARY(0),
    FEBRUARY(1),
    MARCH(2),
    APRIL(3),
    MAY(4),
    JUNE(5),
    JULY(6);

    private final int id;

    UtopiaMonth(int id) {
        this.id = id;
    }

    /**
     * @param id the id of the month
     * @return the Month represented by the specified id, or null if no match was found
     */
    public static UtopiaMonth fromId(final int id) {
        for (UtopiaMonth month : values()) {
            if (month.id == id) return month;
        }
        return null;
    }

    /**
     * @param name the name of the month
     * @return the Month represented by the specified name, or null if no match was found
     */
    public static UtopiaMonth fromName(final String name) {
        for (UtopiaMonth utopiaMonth : values()) {
            if (utopiaMonth.getName().equalsIgnoreCase(name)) return utopiaMonth;
        }
        return null;
    }

    @Override
    public String getName() {
        return StringUtil.prettifyEnumName(this);
    }

    /**
     * @return the id of this month
     */
    public int getId() {
        return id;
    }

    /**
     * @return a regex pattern that can be used to match months
     */
    public static String getMonthPattern() {
        return StringUtil.mergeNamed(values(), '|');
    }
}
