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
import api.tools.numbers.CalculatorUtil;
import api.tools.time.DateUtil;
import api.tools.time.TimeUtil;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "duration_spell", uniqueConstraints = @UniqueConstraint(columnNames = {"province_id", "spell_type_id"}))
@NoArgsConstructor
@EqualsAndHashCode(of = {"province", "type"})
@Getter
@Setter
public class DurationSpell implements SpellsOrOpsDurationInfo, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bot_user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BotUser committer;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "province_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Province province;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires", nullable = false)
    private Date expires;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "spell_type_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SpellType type;

    public DurationSpell(final BotUser caster, final Province province, final Date expires, final SpellType spellType) {
        this.committer = caster;
        this.province = province;
        this.expires = new Date(expires.getTime());
        this.type = spellType;
    }

    @Override
    public String getTimeLeft() {
        return TimeUtil.compareDateToCurrent(getExpires());
    }

    @Override
    public String getTimeLeftInHours() {
        double hours = DateUtil.hoursFromMillis(getExpires().getTime() - System.currentTimeMillis());
        return CalculatorUtil.formatResult(hours, 1);
    }

    @Override
    public int compareTo(final SpellsOrOpsDurationInfo o) {
        return expires.compareTo(o.getExpires());
    }
}
