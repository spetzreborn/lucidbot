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
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "user_activities")
@NoArgsConstructor
@EqualsAndHashCode(of = "user")
@Getter
@Setter
public class UserActivities implements Comparable<UserActivities>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "bot_user_id", nullable = false, unique = true, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BotUser user;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_activity", nullable = false)
    private Date lastActivity;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_seen", nullable = false)
    private Date lastSeen;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_build_check", nullable = false)
    private Date lastBuildCheck;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_orders_check", nullable = false)
    private Date lastOrdersCheck;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_aid_check", nullable = false)
    private Date lastAidCheck;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_events_check", nullable = false)
    private Date lastEventsCheck;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_wave_check", nullable = false)
    private Date lastWaveCheck;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_targets_check", nullable = false)
    private Date lastTargetsCheck;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_naps_check", nullable = false)
    private Date lastNapsCheck;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_notes_check", nullable = false)
    private Date lastNotesCheck;

    public UserActivities(BotUser user) {
        this.user = user;
        Date current = new Date();
        this.lastActivity = current;
        this.lastSeen = current;
        this.lastOrdersCheck = current;
        this.lastAidCheck = current;
        this.lastEventsCheck = current;
        this.lastWaveCheck = current;
        this.lastTargetsCheck = current;
        this.lastNapsCheck = current;
        this.lastBuildCheck = current;
        this.lastNotesCheck = current;
    }

    @Override
    public int compareTo(UserActivities o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getUser().compareTo(o.getUser());
    }
}
