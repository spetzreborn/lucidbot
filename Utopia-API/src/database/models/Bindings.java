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
import api.database.models.BotUser;
import api.filters.FilterEnabled;
import filtering.filters.ExpiringFilter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bindings")
@NoArgsConstructor
@Getter
@Setter
public class Bindings implements BindingsContainer, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pk.bindings", orphanRemoval = true)
    private Set<BotUserBinding> userBindings = new HashSet<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pk.bindings", orphanRemoval = true)
    private Set<RaceBinding> raceBindings = new HashSet<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pk.bindings", orphanRemoval = true)
    private Set<PersonalityBinding> personalityBindings = new HashSet<>();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "publish_date")
    private Date publishDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "remove_date")
    private Date expiryDate;

    @Column(name = "admins_only", nullable = false)
    private boolean adminsOnly;

    public void addRace(final Race race) {
        raceBindings.add(new RaceBinding(race, this));
    }

    public void addPersonality(final Personality personality) {
        personalityBindings.add(new PersonalityBinding(personality, this));
    }

    public void addUser(final BotUser user) {
        userBindings.add(new BotUserBinding(user, this));
    }

    @Override
    public Set<BotUser> getUsers() {
        Set<BotUser> out = new HashSet<>(userBindings.size());
        for (BotUserBinding binding : userBindings) {
            out.add(binding.getUser());
        }
        return out;
    }

    @Override
    public Set<Race> getRaces() {
        Set<Race> out = new HashSet<>(raceBindings.size());
        for (RaceBinding binding : raceBindings) {
            out.add(binding.getRace());
        }
        return out;
    }

    @Override
    public Set<Personality> getPersonalities() {
        Set<Personality> out = new HashSet<>(personalityBindings.size());
        for (PersonalityBinding binding : personalityBindings) {
            out.add(binding.getPersonality());
        }
        return out;
    }

    @Override
    @FilterEnabled(ExpiringFilter.class)
    public Date getExpiryDate() {
        return expiryDate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        return o instanceof Bindings && getId() != null && getId().equals(((Bindings) o).getId());
    }

    @Override
    public int hashCode() {
        return getId() == null ? System.identityHashCode(this) : getId().hashCode();
    }
}
