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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "news_item")
@NoArgsConstructor
@Getter
@Setter
public class NewsItem implements Comparable<NewsItem>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "source", updatable = false, length = 200)
    private String source;

    @Column(name = "target", updatable = false, length = 200)
    private String target;

    @Column(name = "news_type", updatable = false, nullable = false, length = 100)
    private String newsType;

    @Column(name = "item_value", updatable = false, length = 200)
    private String itemValue;

    @Column(name = "uto_date", updatable = false, nullable = false, length = 100)
    private String utoDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "real_date")
    private Date realDate;

    @Lob
    @Column(name = "original_message", updatable = false, nullable = false, length = 500)
    private String originalMessage;

    public NewsItem(final String source, final String target, final String newsType, final String itemValue, final String utoDate,
                    final String originalMessage) {
        this.source = source;
        this.target = target;
        this.newsType = newsType;
        this.itemValue = itemValue;
        this.utoDate = utoDate;
        this.originalMessage = originalMessage;
    }

    public String getUniqueKey() {
        return getUtoDate() + ' ' + getSource() + ' ' + getTarget() + ' ' + getItemValue() + ' ' +
                getNewsType();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        return o instanceof NewsItem && getId() != null && getId().equals(((NewsItem) o).getId());
    }

    @Override
    public int hashCode() {
        return getId() == null ? System.identityHashCode(this) : getId().hashCode();
    }

    @Override
    public int compareTo(final NewsItem o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        int diff = getRealDate().compareTo(o.getRealDate());
        return diff == 0 && getId() != null ? getId().compareTo(o.getId()) : diff;
    }
}
