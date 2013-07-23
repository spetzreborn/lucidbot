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
import filtering.filters.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "private_message")
@NoArgsConstructor
@EqualsAndHashCode(of = {"sent", "recipient", "sender", "message"})
@Getter
@Setter
public class PrivateMessage implements Comparable<PrivateMessage>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sent", updatable = false, nullable = false)
    private Date sent;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_user_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BotUser recipient;

    @Column(name = "sender", updatable = false, nullable = false, length = 50)
    private String sender;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Lob
    @Column(name = "message", nullable = false, updatable = false, length = 5000)
    private String message;

    public PrivateMessage(final BotUser recipient, final String sender, final String message) {
        this.sent = new Date();
        this.recipient = recipient;
        this.sender = sender;
        this.message = message;
    }

    @FilterEnabled(AgeFilter.class)
    public Date getSent() {
        return new Date(sent.getTime());
    }

    @FilterEnabled(RecipientFilter.class)
    public BotUser getRecipient() {
        return recipient;
    }

    @FilterEnabled(SenderFilter.class)
    public String getSender() {
        return sender;
    }

    @FilterEnabled(ReadFilter.class)
    public boolean isRead() {
        return read;
    }

    @FilterEnabled(ArchivedFilter.class)
    public boolean isArchived() {
        return archived;
    }

    @Override
    public int compareTo(final PrivateMessage o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getSent().compareTo(o.getSent());
    }
}
