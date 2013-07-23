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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "build")
@NoArgsConstructor
@Getter
@Setter
public class Build implements Comparable<Build>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bindings_id", updatable = false, unique = true)
    private Bindings bindings;

    @Column(name = "type", updatable = false, nullable = false, length = 200)
    private String type = "default";

    @Column(name = "land", nullable = false)
    private int land;

    @Column(name = "ospa", nullable = false)
    private double ospa;

    @Column(name = "dspa", nullable = false)
    private double dspa;

    @Column(name = "epa", nullable = false)
    private double epa;

    @Column(name = "tpa", nullable = false)
    private double tpa;

    @Column(name = "wpa", nullable = false)
    private double wpa;

    @Column(name = "bpa", nullable = false)
    private double bpa;

    @OneToMany(mappedBy = "build", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuildEntry> buildings = new ArrayList<>();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added", updatable = false, nullable = false)
    private Date added;

    @Column(name = "added_by", nullable = false, updatable = false, length = 100)
    private String addedBy;

    public Build(final Bindings bindings, final String type, final String addedBy) {
        this.bindings = bindings;
        this.type = type;
        this.added = new Date();
        this.addedBy = addedBy;
    }

    @FilterEnabled(AgeFilter.class)
    public Date getAdded() {
        return new Date(added.getTime());
    }

    public Race getRace() {
        return bindings.getRaces().iterator().next();
    }

    public Personality getPersonality() {
        return bindings.getPersonalities().isEmpty() ? null : bindings.getPersonalities().iterator().next();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        return o instanceof Build && getId() != null && getId().equals(((Build) o).getId());
    }

    @Override
    public int hashCode() {
        return getId() == null ? System.identityHashCode(this) : getId().hashCode();
    }

    @Override
    public int compareTo(final Build o) {
        return getAdded().compareTo(o.getAdded());
    }
}
