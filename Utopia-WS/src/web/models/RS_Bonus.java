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

package web.models;

import database.models.Bonus;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static com.google.common.base.Objects.firstNonNull;

@XmlType(name = "Bonus")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Bonus {
    /**
     * The name of the bonus
     */
    @XmlElement(required = true, name = "Name")
    private String name;

    /**
     * The type of bonus
     */
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * In which situations the bonus is applicable
     */
    @XmlElement(required = true, name = "Applicability")
    private String applicability;

    /**
     * Whether this is actually a bonus (i.e. it adds something, makes something better) or a disadvantage
     */
    @XmlElement(required = true, name = "Positive")
    private Boolean positive;

    /**
     * The value of the bonus (0.5 signifies 50% etc.)
     */
    @XmlElement(required = true, name = "Value")
    private Double value;

    public RS_Bonus() {
    }

    private RS_Bonus(final Bonus bonus) {
        this.name = bonus.getName();
        this.type = bonus.getType().getName();
        this.applicability = bonus.getApplicability().getName();
        this.positive = bonus.isIncreasing();
        this.value = bonus.getBonusValue();
    }

    public static RS_Bonus fromBonus(final Bonus bonus) {
        return new RS_Bonus(bonus);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getApplicability() {
        return applicability;
    }

    public boolean isPositive() {
        return positive != null && positive;
    }

    public double getValue() {
        return firstNonNull(value, 0d);
    }
}
