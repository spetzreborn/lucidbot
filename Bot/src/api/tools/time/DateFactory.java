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

import javax.annotation.ParametersAreNonnullByDefault;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory class for Dates and formats
 */
@ParametersAreNonnullByDefault
public class DateFactory {
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_TIME_TIMEZONE_FORMAT = "yyyy-MM-dd HH:mm:ssZ";
    private static final String DATE_TIME_NO_SECONDS_FORMAT = "yyyy-MM-dd HH:mm";

    private DateFactory() {
    }

    /**
     * Creates a new Date based on the specified ISO formatted String isoFormattedDate
     *
     * @param isoFormattedDate a Date formatted according to the ISO standard, with or without time and seconds
     * @return a new Date
     */
    public static Date newDate(final String isoFormattedDate) {
        checkNotNull(isoFormattedDate);
        try {
            return getISODateTimeFormat().parse(isoFormattedDate);
        } catch (ParseException ignore) {
        }
        try {
            return getISOWithoutSecondsDateTimeFormat().parse(isoFormattedDate);
        } catch (ParseException ignore) {
        }
        try {
            return getISODateFormat().parse(isoFormattedDate);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Illegal date format: " + isoFormattedDate, e);
        }
    }

    /**
     * Creates a new Date based on the specified ISO formatted String isoFormattedDate
     *
     * @param isoFormattedDate a Date formatted according to the ISO standard, with or without time and seconds
     * @return a new Date
     */
    public static Date newGMTDate(final String isoFormattedDate) {
        checkNotNull(isoFormattedDate);
        try {
            DateFormat isoDateTimeFormat = getISODateTimeFormat();
            isoDateTimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return isoDateTimeFormat.parse(isoFormattedDate);
        } catch (ParseException ignore) {
        }
        try {
            DateFormat isoDateTimeFormat = getISOWithoutSecondsDateTimeFormat();
            isoDateTimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return isoDateTimeFormat.parse(isoFormattedDate);
        } catch (ParseException ignore) {
        }
        try {
            DateFormat isoDateFormat = getISODateFormat();
            isoDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return isoDateFormat.parse(isoFormattedDate);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Illegal date format: " + isoFormattedDate, e);
        }
    }

    /**
     * @return ISO Date format
     */
    public static DateFormat getISODateFormat() {
        return new SimpleDateFormat(DATE_FORMAT);
    }

    /**
     * @return ISO Date format as a String
     */
    public static String getISODateFormatAsString() {
        return DATE_FORMAT;
    }

    /**
     * @return ISO Date & Time format
     */
    public static DateFormat getISODateTimeFormat() {
        return new SimpleDateFormat(DATE_TIME_FORMAT);
    }

    /**
     * @return ISO Date & Time format as a String
     */
    public static String getISODateTimeFormatAsString() {
        return DATE_FORMAT;
    }

    /**
     * @return ISO Date & Time format with time zone specified
     */
    public static DateFormat getISODateTimeWithTimeZoneFormat() {
        return new SimpleDateFormat(DATE_TIME_TIMEZONE_FORMAT);
    }

    /**
     * @return ISO Date & Time format as a String with time zone specified
     */
    public static String getISODateTimeWithTimeZoneFormatAsString() {
        return DATE_TIME_TIMEZONE_FORMAT;
    }

    /**
     * @return ISO Date & Time format without seconds
     */
    public static DateFormat getISOWithoutSecondsDateTimeFormat() {
        return new SimpleDateFormat(DATE_TIME_NO_SECONDS_FORMAT);
    }

    /**
     * @return ISO Date & Time format without seconds as a String
     */
    public static String getISOWithoutSecondsDateTimeFormatAsString() {
        return DATE_FORMAT;
    }
}
