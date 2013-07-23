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

package filtering;

import api.tools.time.DateUtil;
import spi.filters.AbstractFilter;

import java.util.Date;

public abstract class AbstractDateBasedFilter extends AbstractFilter<Date> {
    protected static Date getDateHoursFromNow(final double d) {
        return new Date(System.currentTimeMillis() + DateUtil.hoursToMillis(d));
    }

    protected static Date getDateHoursAgo(final double d) {
        return new Date(System.currentTimeMillis() - DateUtil.hoursToMillis(d));
    }

    private final Date lowerBound;
    private final Date upperBound;

    protected AbstractDateBasedFilter(final Date lowerBound, final Date upperBound) {
        super(Date.class);
        this.lowerBound = lowerBound == null ? null : new Date(lowerBound.getTime());
        this.upperBound = upperBound == null ? null : new Date(upperBound.getTime());
    }

    @Override
    public boolean passesFilter(final Date value) {
        //If it's a range (both lower and upper bound available), check with possible equals
        if (lowerBound != null && upperBound != null) return value != null && value.compareTo(upperBound) <= 0 &&
                                                             value.compareTo(lowerBound) >= 0;
        //Not a range, so don't use equals
        return value != null && (lowerBound == null || value.compareTo(lowerBound) > 0) &&
               (upperBound == null || value.compareTo(upperBound) < 0);
    }
}
