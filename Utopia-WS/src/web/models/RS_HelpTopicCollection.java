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

package web.models;

import api.common.HasNumericId;
import com.google.common.collect.Lists;
import com.sun.jersey.server.linking.Ref;
import database.models.HelpTopic;
import database.models.HelpTopicCollection;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ExistsInDB;
import web.validation.Add;
import web.validation.Update;

import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlRootElement(name = "HelpTopicCollection")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_HelpTopicCollection implements HasNumericId {
    /**
     * The id for this help topic collection. The id is set by the database, so clients will only use it in the URL's.
     * <p/>
     * The server will simply ignore this value if you send it in with some request.
     */
    @XmlElement(name = "ID")
    private Long id;

    /**
     * A convenience link to this entity. Only used for navigation.
     * <p/>
     * The server will simply ignore this value if you send it in with some request.
     */
    @Ref("help/collections/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The name of the collection.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotEmpty(message = "The name must not be null or empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Name")
    private String name;

    /**
     * If this collection is a child of some other collection, this points out the parent collection. If sending this in, specifying the id
     * on the parent object is enough, you don't need to populate the whole thing.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @ExistsInDB(entity = HelpTopicCollection.class, optional = true, message = "No such collection", groups = {Add.class, Update.class})
    @XmlElement(name = "ParentCollection")
    private RS_HelpTopicCollection parent;

    /**
     * The children collections belonging to this collection. For display purposes only.
     */
    @XmlElementWrapper(name = "ChildrenCollections")
    @XmlElement(name = "ChildCollection")
    private List<RS_HelpTopicCollection> children;

    /**
     * The topics belonging to this collection. For display purposes only.
     */
    @XmlElementWrapper(name = "Topics")
    @XmlElement(name = "HelpTopic")
    private List<RS_HelpTopic> topics;

    public RS_HelpTopicCollection() {
    }

    private RS_HelpTopicCollection(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    private RS_HelpTopicCollection(final HelpTopicCollection collection) {
        this(collection.getId(), collection.getName());
        this.parent = collection.getParent() == null ? null : fromHelpTopicCollection(collection.getParent(), false);
        this.children = Lists.newArrayList();
        for (HelpTopicCollection topicCollection : collection.getChildren()) {
            children.add(fromHelpTopicCollection(topicCollection, false));
        }
        this.topics = Lists.newArrayList();
        for (HelpTopic topic : collection.getHelpTopics()) {
            topics.add(RS_HelpTopic.fromHelpTopic(topic, true));
        }
    }

    public static RS_HelpTopicCollection fromHelpTopicCollection(final HelpTopicCollection collection, final boolean full) {
        return full ? new RS_HelpTopicCollection(collection) : new RS_HelpTopicCollection(collection.getId(), collection.getName());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RS_HelpTopicCollection getParent() {
        return parent;
    }

    public List<RS_HelpTopicCollection> getChildren() {
        return toEmptyListIfNull(children);
    }

    public List<RS_HelpTopic> getTopics() {
        return toEmptyListIfNull(topics);
    }
}
