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

import api.common.HasName;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
public class ListUtil {
    private ListUtil() {
    }

    public static <E> boolean containsNull(final Iterable<E> iterable) {
        for (E e : iterable) {
            if (e == null) return true;
        }
        return false;
    }

    public static <E> List<E> toEmptyListIfNull(final List<E> possiblyNullList) {
        return possiblyNullList == null ? Collections.<E>emptyList() : possiblyNullList;
    }

    /**
     * Checks if the contents of the two lists are equal
     *
     * @param left  .
     * @param right .
     * @param <E>   the type contained in the lists
     * @return true if the two lists contain equal elements in the same order
     */
    public static <E> boolean listContentsAreEqual(final List<E> left, final List<E> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); ++i) {
            if (left.get(i) == null || !left.get(i).equals(right.get(i))) return false;
        }
        return true;
    }

    public static List<String> getNames(final Collection<? extends HasName> named) {
        List<String> out = new ArrayList<>(named.size());
        for (HasName hasName : named) {
            out.add(hasName.getName());
        }
        return out;
    }
}
