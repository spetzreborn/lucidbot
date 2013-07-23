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
import api.database.models.BotUser;
import api.filters.FilterEnabled;
import filtering.filters.AgeFilter;
import filtering.filters.PersonalityFilter;
import filtering.filters.RaceFilter;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.*;

import static api.tools.text.StringUtil.prettifyEnumName;

@Entity
@Table(name = "target", uniqueConstraints = @UniqueConstraint(columnNames = {"province_id", "type"}))
@NoArgsConstructor
@EqualsAndHashCode(of = {"province", "type"})
@Getter
@Setter
public class Target implements Comparable<Target>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bindings_id", unique = true)
    private Bindings bindings;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province province;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 100)
    private TargetType type;

    @Lob
    @Column(name = "details", nullable = false, length = 1000)
    private String details;

    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pk.target", orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TargetHitter> hitters = new ArrayList<>();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added", nullable = false)
    private Date added;

    public Target(Province province, TargetType type, String details, Bindings bindings) {
        this.province = province;
        this.type = type;
        this.details = details;
        this.bindings = bindings;
        this.added = new Date();
    }

    public void clear() {
        hitters.clear();
    }

    public int getAmountOfHitters() {
        return hitters.size();
    }

    public List<BotUser> getHitters() {
        Collections.sort(hitters);
        List<BotUser> out = new ArrayList<>();
        for (TargetHitter join : hitters) {
            out.add(join.getHitter());
        }
        return out;
    }

    public boolean containsUserAsHitter(final BotUser user) {
        for (TargetHitter hitter : hitters) {
            if (hitter.getHitter().equals(user)) return true;
        }
        return false;
    }

    public void insertHitter(final BotUser user, final int position) {
        insertHitter(position - 1, new TargetHitter(this, user, position));
    }

    private void insertHitter(final int position, final TargetHitter hitter) {
        Collections.sort(hitters);
        hitters.add(position, hitter);
        updatePositions();
    }

    public void moveHitter(final BotUser user, final int toPosition) {
        Collections.sort(hitters);
        TargetHitter toMove = removeHitter(user);
        insertHitter(toPosition - 1, toMove);
    }

    public TargetHitter removeHitter(final BotUser user) {
        for (Iterator<TargetHitter> iter = hitters.iterator(); iter.hasNext(); ) {
            TargetHitter next = iter.next();
            if (next.getHitter().equals(user)) {
                iter.remove();
                updatePositions();
                return next;
            }
        }
        return null;
    }

    private void updatePositions() {
        for (int i = 0; i < hitters.size(); ++i) {
            hitters.get(i).setPosition(i + 1);
        }
    }

    @FilterEnabled(AgeFilter.class)
    public Date getAdded() {
        return new Date(added.getTime());
    }

    @FilterEnabled(RaceFilter.class)
    public Race getRace() {
        return getProvince().getRace();
    }

    @FilterEnabled(PersonalityFilter.class)
    public Personality getPersonality() {
        return getProvince().getPersonality();
    }

    public enum TargetType implements HasName {
        FARM, CLAIM, MANUAL_TARGET, GENERATED_TARGET;

        @Override
        public String getName() {
            return prettifyEnumName(this);
        }

        public static TargetType fromName(final String name) {
            for (TargetType type : values()) {
                if (type.getName().equalsIgnoreCase(name)) return type;
            }
            return null;
        }
    }

    @Override
    public int compareTo(Target o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        int typeComp = getType().compareTo(o.getType());
        return typeComp == 0 ? getProvince().compareTo(o.getProvince()) : typeComp;
    }
}
