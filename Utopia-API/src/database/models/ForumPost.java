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
@Table(name = "forum_post", uniqueConstraints = @UniqueConstraint(columnNames = {"bot_user_id", "thread_id", "posted"}))
@NoArgsConstructor
@EqualsAndHashCode(of = {"user", "thread", "posted"})
@Getter
@Setter
public class ForumPost implements Comparable<ForumPost>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "bot_user_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BotUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "thread_id", updatable = false, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ForumThread thread;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "posted", updatable = false, nullable = false)
    private Date posted;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_edited")
    private Date lastEdited;

    @Lob
    @Column(name = "post", nullable = false, length = 10000)
    private String post;

    public ForumPost(final BotUser user, final ForumThread thread, final String post) {
        this.user = user;
        this.thread = thread;
        posted = new Date();
        this.post = post;
    }

    @FilterEnabled(AgeFilter.class)
    public Date getPosted() {
        return new Date(posted.getTime());
    }

    @Override
    public int compareTo(final ForumPost o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        return getPosted().compareTo(o.getPosted());
    }
}
