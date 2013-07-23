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
import api.tools.text.StringUtil;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "sos_entry", uniqueConstraints = @UniqueConstraint(columnNames = {"parent_id", "type", "science_type_id"}))
@NoArgsConstructor
@EqualsAndHashCode(of = {"parent", "type", "scienceType"})
@Getter
@Setter
public class SoSEntry implements Comparable<SoSEntry>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SoS parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 100)
    private SoSEntryType type;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "science_type_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScienceType scienceType;

    @Column(name = "val", nullable = false)
    private double value;

    public SoSEntry(final SoS parent, final SoSEntryType type, final ScienceType scienceType, final double value) {
        this.parent = parent;
        this.type = type;
        this.scienceType = scienceType;
        this.value = value;
    }

    public enum SoSEntryType implements HasName {
        BOOKS, BOOKS_IN_PROGRESS, EFFECT;

        @Override
        public String getName() {
            return StringUtil.prettifyEnumName(this);
        }

        public static SoSEntryType fromName(final String name) {
            for (SoSEntryType type : values()) {
                if (type.getName().equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) return type;
            }
            return null;
        }
    }

    @Override
    public int compareTo(final SoSEntry o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        int sosComp = getParent().compareTo(o.getParent());
        return sosComp == 0 ? getScienceType().compareTo(o.getScienceType()) : sosComp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(200);
        sb.append("SoSEntry");
        sb.append("{type=").append(getType());
        sb.append(", scienceType=").append(getScienceType());
        sb.append(", value=").append(getValue());
        sb.append('}');
        return sb.toString();
    }
}
