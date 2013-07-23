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

import api.common.HasName;
import api.common.HasNumericId;
import com.google.common.collect.Lists;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "building")
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
@Getter
@Setter
public class Building implements HasName, Comparable<Building>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "short_name", nullable = false, unique = true, length = 10)
    private String shortName;

    @Lob
    @Column(name = "syntax", length = 1000)
    private String syntax;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BuildingFormula> formulas = new ArrayList<>();

    public Building(final String name, final String shortName, final BuildingFormula... formulaes) {
        this.name = name;
        this.shortName = shortName;
        formulas = formulaes == null ? Collections.<BuildingFormula>emptyList() : Lists.newArrayList(formulaes);
    }

    public Building(final String name, final String shortName, final String syntax, final List<BuildingFormula> formulas) {
        this.name = name;
        this.shortName = shortName;
        this.formulas.addAll(formulas);
        this.syntax = syntax;
    }

    @Override
    public int compareTo(final Building o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getName().compareToIgnoreCase(o.getName());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(75);
        sb.append("Building");
        sb.append("{name='").append(getName()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
