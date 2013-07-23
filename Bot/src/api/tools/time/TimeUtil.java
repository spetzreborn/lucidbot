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

package api.tools.time;

import java.util.Date;

public class TimeUtil {
    private TimeUtil() {
    }

    /**
     * @param date the date
     * @return a String detailing the hours, minutes and seconds between the specified date and the current time
     */
    public static String compareDateToCurrent(final Date date) {
        return compareTimeToCurrent(date.getTime());
    }

    /**
     * @param time the time in millis
     * @return a String detailing the hours, minutes and seconds between the specified time and the current time
     */
    public static String compareTimeToCurrent(final long time) {
        final long currentTime = System.currentTimeMillis();
        long remains = time - currentTime;
        return formatTimeDifference(remains);
    }

    /**
     * Formats the time difference to a String describing how many hours, seconds etc. it represents
     *
     * @param timeDifference the time difference to format
     * @return a String with the time formatted
     */
    public static String formatTimeDifference(final long timeDifference) {
        long remains = timeDifference;
        if (remains < 0) {
            remains *= -1;
        }
        remains /= 1000;
        final long hours = remains / 3600;
        final long mins = remains % 3600 / 60;
        final long secs = remains % 3600 % 60;

        String stringFormat = "";
        if (hours > 0) {
            stringFormat += hours + "h, ";
        }
        if (mins > 0) {
            stringFormat += mins + "m, ";
        }
        if (secs > 0) {
            stringFormat += secs + "s";
        }
        if (stringFormat.endsWith(", ")) {
            stringFormat = stringFormat.substring(0, stringFormat.length() - 2);
        }
        return stringFormat;
    }
}
