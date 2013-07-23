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
import java.util.concurrent.TimeUnit;

public class DateUtil {
    private static final long HOUR_IN_MILLIS = 60 * 60 * 1000;
    private static final long MINUTE_IN_MILLIS = 60 * 1000;

    public static long hoursToMillis(double hours) {
        return (long) (hours * HOUR_IN_MILLIS);
    }

    public static long minutesToMillis(double minutes) {
        return (long) (minutes * MINUTE_IN_MILLIS);
    }

    public static long hoursToMillis(long hours) {
        return TimeUnit.HOURS.toMillis(hours);
    }

    public static long minutesToMillis(long minutes) {
        return TimeUnit.MINUTES.toMillis(minutes);
    }

    public static double hoursFromMillis(long millis) {
        return millis * 1.0 / HOUR_IN_MILLIS;
    }

    public static boolean isAfter(final Date first, final Date second) {
        return first.getTime() > second.getTime();
    }

    public static boolean isBefore(final Date first, final Date second) {
        return first.getTime() < second.getTime();
    }

    private DateUtil() {
    }
}
