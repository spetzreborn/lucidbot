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

import api.tools.time.DateUtil;
import tools.parsing.UtopiaValidationType;

import java.util.Date;
import java.util.regex.Matcher;

/**
 * A class that represents time, meaning both utopia time and the corresponding real life time
 */
public class UtopiaTime implements Comparable<UtopiaTime> {
    private final int tickLength;
    private final Date ageStart;
    private final int utyear;
    private final UtopiaMonth utmonth;
    private final int utday;
    private final long time;

    UtopiaTime(final Long time, final Date ageStart, final int tickLength) {
        this.tickLength = tickLength;
        this.ageStart = ageStart;

        long ageStartTime = ageStart.getTime();
        long timeDifferenceInSeconds = Math.max((time - ageStartTime) / 1000L, 0);

        long utoYearLength = 7L * 24 * tickLength * 60;
        long utoMonthLength = 24L * tickLength * 60;
        long utoDayLength = 60L * tickLength;

        this.utyear = (int) (timeDifferenceInSeconds / utoYearLength);
        this.utmonth = UtopiaMonth.fromId((int) (timeDifferenceInSeconds % utoYearLength / utoMonthLength));
        this.utday = (int) (timeDifferenceInSeconds % utoYearLength % utoMonthLength / utoDayLength + 1);

        this.time = ageStartTime +
                DateUtil.minutesToMillis(utyear * 7 * 24 * tickLength + utmonth.getId() * 24 * tickLength + (utday - 1) * tickLength);
    }

    UtopiaTime(final String utdate, final Date ageStart, final int tickLength) {
        this.tickLength = tickLength;
        this.ageStart = ageStart;

        Matcher matcher = UtopiaValidationType.UTODATE_WITH_GROUPS.getPattern().matcher(utdate);
        if (!matcher.matches()) throw new IllegalArgumentException("Utodate not correctly formatted: " + utdate);

        this.utyear = Integer.parseInt(matcher.group("year"));
        this.utmonth = UtopiaMonth.fromName(matcher.group("month"));
        this.utday = Integer.parseInt(matcher.group("day"));

        this.time = ageStart.getTime() +
                DateUtil.minutesToMillis(utyear * 7 * 24 * tickLength + utmonth.getId() * 24 * tickLength + (utday - 1) * tickLength);
    }

    public static Date calculateAgeStart(final String newCurrentUtoDate, final UtopiaTime currentUtoDate) {
        Matcher matcher = UtopiaValidationType.UTODATE_WITH_GROUPS.getPattern().matcher(newCurrentUtoDate);
        if (!matcher.matches()) throw new IllegalArgumentException("Unparsable uto date: " + newCurrentUtoDate);
        int utyearDiff = Integer.parseInt(matcher.group("year")) - currentUtoDate.getYear();
        int utmonthDiff = UtopiaMonth.fromName(matcher.group("month")).getId() - currentUtoDate.getMonth().getId();
        int utdayDiff = Integer.parseInt(matcher.group("day")) - currentUtoDate.getDay();

        int tickLength = currentUtoDate.tickLength;
        long totalDiff = DateUtil
                .minutesToMillis(utyearDiff * 7 * 24 * tickLength + utmonthDiff * 24 * tickLength + utdayDiff * tickLength);

        return new Date(currentUtoDate.ageStart.getTime() - totalDiff);
    }

    /**
     * @return a formatted utopia date
     */
    public String formattedUT() {
        return utmonth.getName() + ' ' + utday + ", YR" + utyear;
    }

    /**
     * @return the day in utopia, which is a number between 1 and 24
     */
    public int getDay() {
        return utday;
    }

    /**
     * @return the day in utopia, with a suffix, for example 1st or 22nd
     */
    public String getDayWithSuffix() {
        return utday + getDaySuffix();
    }

    private String getDaySuffix() {
        if (utday == 1 || utday == 21) {
            return "st";
        } else if (utday == 2 || utday == 22) {
            return "nd";
        } else if (utday == 3 || utday == 23) {
            return "rd";
        } else {
            return "th";
        }
    }

    /**
     * @return the month in utopia
     */
    public UtopiaMonth getMonth() {
        return utmonth;
    }

    /**
     * @return the year in utopia
     */
    public int getYear() {
        return utyear;
    }

    /**
     * @return the real life time represented by this object
     */
    public long getTime() {
        return time;
    }

    /**
     * @return the real life date and time represented by this object
     */
    public Date getDate() {
        return new Date(time);
    }

    @Override
    public int compareTo(UtopiaTime o) {
        if (utyear != o.utyear) return Integer.compare(utyear, o.utyear);
        if (utmonth != o.utmonth) return utmonth.compareTo(o.utmonth);
        if (utday != o.utday) return Integer.compare(utday, o.utday);
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof UtopiaTime)) return false;

        UtopiaTime that = (UtopiaTime) o;
        return utyear == that.utyear && utmonth == that.utmonth && utday == that.utday;
    }

    @Override
    public int hashCode() {
        int result = utyear;
        result = 31 * result + (utmonth != null ? utmonth.hashCode() : 0);
        result = 31 * result + utday;
        return result;
    }

    @Override
    public String toString() {
        return formattedUT();
    }

    /**
     * Returns a new UtopiaTime instance with the amount of ticks added.
     *
     * @param ticks the amount of ticks to add
     * @return a new UtopiaTime instance with the amount of ticks added.
     */
    public UtopiaTime increment(final int ticks) {
        return new UtopiaTime(DateUtil.minutesToMillis(ticks * 1L * tickLength) + time, ageStart, tickLength);
    }
}
