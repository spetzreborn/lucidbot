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
import api.tools.text.StringUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "help_topic")
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
@Getter
@Setter
public class HelpTopic implements Comparable<HelpTopic>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "collection_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HelpTopicCollection collection;

    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Lob
    @Column(name = "help_text", nullable = false, length = 10000)
    private String helpText;

    public HelpTopic(final HelpTopicCollection collection, final String name, final String helpText) {
        this.collection = collection;
        this.name = name;
        this.helpText = helpText;
    }

    /**
     * @return the help text, split on EOL. Meant to be used for reads only
     */
    public List<String> getSplitText() {
        return Arrays.asList(StringUtil.splitOnEndOfLine(getHelpText()));
    }

    @Override
    public int compareTo(final HelpTopic o) {
        if (getId() != null && getId().equals(o.getId())) return 0;
        int collectionComp = getCollection().compareTo(o.getCollection());
        return collectionComp == 0 ? getName().compareToIgnoreCase(o.getName()) : collectionComp;
    }
}
