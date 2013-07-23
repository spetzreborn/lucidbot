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
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "bonus")
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
@Getter
@Setter
public class Bonus implements Comparable<Bonus>, HasName, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 100)
    private BonusType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicability", nullable = false, length = 100)
    private BonusApplicability applicability;

    @Column(name = "is_increasing", nullable = false)
    private boolean isIncreasing;

    @Column(name = "bonus_value", nullable = false, precision = 2)
    private double bonusValue;

    public static Bonus noBonus(final BonusType type, final BonusApplicability applicability) {
        return new Bonus("", type, applicability, true, 0);
    }

    public Bonus(final String name, final BonusType type, final BonusApplicability applicability, final boolean isIncreasing,
                 final double bonusValue) {
        this.name = name;
        this.type = type;
        this.applicability = applicability;
        this.isIncreasing = isIncreasing;
        this.bonusValue = bonusValue;
    }

    public double applyTo(final double value) {
        return isIncreasing() ? value * (1 + getBonusValue()) : value * (1 - getBonusValue());
    }

    public double applyTo(final double value, final double bonusModifiers) {
        return isIncreasing() ? value * (1 + getBonusValue() * bonusModifiers) : value * (1 - getBonusValue() * bonusModifiers);
    }

    @Override
    public int compareTo(final Bonus o) {
        return Double.valueOf(getBonusValue()).compareTo(o.getBonusValue());
    }
}
