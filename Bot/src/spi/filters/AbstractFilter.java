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

package spi.filters;

import api.tools.files.FilterUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Iterator;

/**
 * Skeleton implementation of a Filter
 *
 * @param <T> the type of filter
 */
@ParametersAreNonnullByDefault
public abstract class AbstractFilter<T> implements Filter<T> {
    private final Class<T> clazz;

    protected AbstractFilter(final Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void filter(final Collection<?> collection) {
        for (Iterator<?> iter = collection.iterator(); iter.hasNext(); ) {
            FilterUtil.FilterValue<T> filterResult = FilterUtil
                    .getFilterEnabledMethodOrFieldValue(iter.next(), getFilterType(), clazz, getMethodParameters());
            if (filterResult.isFilterValueFound() && (filterResult.getValue() == null || !passesFilter(filterResult.getValue()))) {
                iter.remove();
            }
        }
    }

    /**
     * @return the type of filter
     */
    protected abstract Class<? extends Filter<T>> getFilterType();

    @Override
    public Object[] getMethodParameters() {
        return new Object[0];
    }
}
