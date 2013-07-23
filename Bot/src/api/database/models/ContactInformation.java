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

package api.database.models;

import api.common.HasNumericId;
import lombok.AccessLevel;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "contact_information", uniqueConstraints = @UniqueConstraint(columnNames = {"bot_user_id", "information_type"}))
@NoArgsConstructor
@EqualsAndHashCode(exclude = "id")
@Getter
@Setter
public final class ContactInformation implements Comparable<ContactInformation>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    /**
     * The user this contact information belongs to
     */
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "bot_user_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BotUser user;

    /**
     * The type of contact information this is
     */
    @Column(name = "information_type", updatable = false, nullable = false, length = 200)
    private String informationType;

    /**
     * The actual information
     */
    @Lob
    @Column(name = "information", updatable = true, nullable = false, length = 5000)
    private String information;

    public ContactInformation(final BotUser user, final String informationType, final String information) {
        this.user = checkNotNull(user);
        this.informationType = checkNotNull(informationType);
        this.information = checkNotNull(information);
    }

    @Override
    public int compareTo(final ContactInformation o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        int userComp = getUser().compareTo(o.getUser());
        return userComp == 0 ? getInformationType().compareToIgnoreCase(o.getInformationType()) : userComp;
    }
}
