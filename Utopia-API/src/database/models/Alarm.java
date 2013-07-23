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
import api.tools.time.TimeUtil;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "alarm")
@NoArgsConstructor
@EqualsAndHashCode(exclude = "id")
@Getter
@Setter
public class Alarm implements Comparable<Alarm>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "alarm_time", updatable = false, nullable = false)
    private Date alarmTime;

    @Lob
    @Column(name = "message", updatable = false, nullable = false, length = 400)
    private String message;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "bot_user_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BotUser user;

    public Alarm(final Date alarmTime, final String message, final BotUser user) {
        this.alarmTime = new Date(alarmTime.getTime());
        this.message = message;
        this.user = user;
    }

    public String getAlarmTimeInUsersLocalTime() {
        return getUser().getDateInUsersLocalTime(getAlarmTime());
    }

    public String getTimeLeft() {
        return TimeUtil.compareDateToCurrent(getAlarmTime());
    }

    @Override
    public int compareTo(final Alarm o) {
        int compared = getAlarmTime().compareTo(o.getAlarmTime());
        if (compared == 0) return getMessage().compareTo(o.getMessage());
        return compared;
    }
}
