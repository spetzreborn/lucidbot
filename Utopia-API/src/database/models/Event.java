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

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static api.tools.text.StringUtil.prettifyEnumName;

@Entity
@Table(name = "event")
@NoArgsConstructor
@EqualsAndHashCode(exclude = "id")
@Getter
@Setter
public class Event implements Comparable<Event>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "type", updatable = false, nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private EventType type;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bindings_id", updatable = false, unique = true)
    private Bindings bindings;

    @Lob
    @Column(name = "description", length = 1000)
    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "event_time", nullable = false)
    private Date eventTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added", updatable = false, nullable = false)
    private Date added;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttendanceStatus> attendanceInformation = new ArrayList<>();

    public Event(final EventType type, final String description, final Date eventTime) {
        this(type, description, eventTime, null);
    }

    public Event(final EventType type, final String description, final Date eventTime, final Bindings bindings) {
        this.type = type;
        this.description = description;
        this.eventTime = eventTime;
        this.attendanceInformation = new ArrayList<>();
        this.added = new Date();
        this.bindings = bindings;
    }

    public enum EventType implements HasName {
        WAVE, EVENT;

        @Override
        public String getName() {
            return prettifyEnumName(this);
        }

        public static EventType fromName(final String name) {
            for (EventType type : values()) {
                if (type.getName().equalsIgnoreCase(name)) return type;
            }
            return null;
        }

        public static String getRegexGroup() {
            return StringUtil.mergeNamed(values(), '|');
        }
    }

    @Override
    public int compareTo(final Event o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        int timeComp = getEventTime().compareTo(o.getEventTime());
        return timeComp == 0 ? getAdded().compareTo(o.getAdded()) : timeComp;
    }
}
