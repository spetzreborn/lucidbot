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
import filtering.filters.ExpiringFilter;
import filtering.filters.PersonalityFilter;
import filtering.filters.RaceFilter;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "aid", uniqueConstraints = @UniqueConstraint(columnNames = {"province_id", "type"}))
@NoArgsConstructor
@EqualsAndHashCode(of = {"province", "type"})
@Getter
@Setter
public class Aid implements Comparable<Aid>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province province;

    @Column(name = "type", updatable = false, nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private AidType type;

    @Column(name = "importance_type", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private AidImportanceType importanceType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added", nullable = false)
    private Date added;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiry_date")
    private Date expiryDate;

    public Aid(final Province province, final AidType type) {
        this.province = province;
        this.type = type;
        this.added = new Date();
    }

    public Aid(final Province province, final AidType type, final AidImportanceType importanceType, final int amount) {
        this(province, type);
        this.importanceType = importanceType;
        this.amount = amount;
    }

    public Aid(final Province province, final AidType type, final AidImportanceType importanceType, final int amount,
               final Date expiryDate) {
        this(province, type, importanceType, amount);
        this.expiryDate = expiryDate;
    }

    @FilterEnabled(AgeFilter.class)
    public Date getAdded() {
        return added;
    }

    @FilterEnabled(ExpiringFilter.class)
    public Date getExpiryDate() {
        return expiryDate;
    }

    @FilterEnabled(RaceFilter.class)
    public Race getRace() {
        return getProvince().getRace();
    }

    @FilterEnabled(PersonalityFilter.class)
    public Personality getPersonality() {
        return getProvince().getPersonality();
    }

    @Override
    public int compareTo(final Aid o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        int importance = getImportanceType().compareTo(o.getImportanceType());
        return importance == 0 ? getAdded().compareTo(o.getAdded()) : importance;
    }
}
