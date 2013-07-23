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
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "building_formula")
@NoArgsConstructor
@EqualsAndHashCode(of = {"building", "resultText"})
@Getter
@Setter
public class BuildingFormula implements Comparable<BuildingFormula>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "building_id", nullable = false, updatable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Building building;

    @Column(name = "formula", nullable = false, length = 255)
    private String formula;

    @Lob
    @Column(name = "result_text", nullable = false, length = 500)
    private String resultText;

    @Column(name = "cap")
    private Double cap;

    @ManyToOne(optional = true)
    @JoinColumn(name = "bonus_id", nullable = true, updatable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Bonus bonus;

    public BuildingFormula(final String formula, final String resultText, final Double cap) {
        this.formula = formula;
        this.resultText = resultText;
        this.cap = cap;
    }

    public BuildingFormula(final String formula, final String resultText, final Double cap, final Bonus bonus) {
        this.formula = formula;
        this.resultText = resultText;
        this.cap = cap;
        this.bonus = bonus;
    }

    public BuildingFormula(final Building building, final String formula, final String resultText, final Double cap, final Bonus bonus) {
        this.building = building;
        this.formula = formula;
        this.resultText = resultText;
        this.cap = cap;
        this.bonus = bonus;
    }

    public boolean hasBonus(final BonusType type, final BonusApplicability applicability) {
        return bonus != null && bonus.getType() == type && bonus.getApplicability().is(applicability);
    }

    @Override
    public int compareTo(final BuildingFormula o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        int buildingComp = getBuilding().compareTo(o.getBuilding());
        return buildingComp == 0 ? getResultText().compareTo(o.getResultText()) : buildingComp;
    }
}
