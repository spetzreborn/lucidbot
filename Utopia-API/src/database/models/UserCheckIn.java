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
import filtering.filters.AgeFilter;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "check_in")
@NoArgsConstructor
@EqualsAndHashCode(of = "user")
@Getter
@Setter
public class UserCheckIn implements Comparable<UserCheckIn>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "bot_user_id", updatable = false, nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BotUser user;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province province;

    @Lob
    @Column(name = "checked_in", nullable = false, length = 500)
    private String checkedIn;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "check_in_time", nullable = false)
    private Date checkInTime;

    public UserCheckIn(BotUser user, Province province, String checkedIn) {
        this.user = user;
        this.province = province;
        this.checkedIn = checkedIn;
        this.checkInTime = new Date();
    }

    @FilterEnabled(AgeFilter.class)
    public Date getCheckInTime() {
        return new Date(checkInTime.getTime());
    }

    @Override
    public int compareTo(UserCheckIn o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getUser().compareTo(o.getUser());
    }
}
