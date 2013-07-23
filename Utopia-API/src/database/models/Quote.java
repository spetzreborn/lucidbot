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

import api.common.HasNumericId;
import api.filters.FilterEnabled;
import filtering.filters.AgeFilter;
import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "quote")
@NoArgsConstructor
@EqualsAndHashCode(of = {"added", "addedBy", "quote"})
@Getter
@Setter
public class Quote implements Comparable<Quote>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "added_by", updatable = false, nullable = false, length = 200)
    private String addedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added", updatable = false, nullable = false)
    private Date added;

    @Lob
    @Column(name = "quote", updatable = false, nullable = false, length = 5000)
    private String quote;

    public Quote(final String addedBy, final String quote) {
        this.addedBy = addedBy;
        this.added = new Date();
        this.quote = quote;
    }

    @FilterEnabled(AgeFilter.class)
    public Date getAdded() {
        return new Date(added.getTime());
    }

    @Override
    public int compareTo(final Quote o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getAdded().compareTo(o.getAdded());
    }
}
