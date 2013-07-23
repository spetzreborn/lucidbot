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
@Table(name = "orders")
@NoArgsConstructor
@EqualsAndHashCode(of = {"order", "addedBy", "added"})
@Getter
@Setter
public class Order implements Comparable<Order>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bindings_id", updatable = false, nullable = false, unique = true)
    private Bindings bindings;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private OrderCategory category;

    @Lob
    @Column(name = "order_text", nullable = false, length = 1000)
    private String order;

    @Column(name = "added_by", updatable = false, nullable = false, length = 200)
    private String addedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added", updatable = false, nullable = false)
    private Date added;

    public Order(final Bindings bindings, final OrderCategory category, final String order, final String addedBy) {
        this.bindings = bindings;
        this.category = category;
        this.order = order;
        this.addedBy = addedBy;
        this.added = new Date();
    }

    @FilterEnabled(AgeFilter.class)
    public Date getAdded() {
        return new Date(added.getTime());
    }

    public String getCategoryName() {
        return getCategory() == null ? "?" : getCategory().getName();
    }

    @Override
    public int compareTo(final Order o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        int typeComp = getCategoryName().compareToIgnoreCase(o.getCategoryName());
        return typeComp == 0 ? getAdded().compareTo(o.getAdded()) : typeComp;
    }
}
