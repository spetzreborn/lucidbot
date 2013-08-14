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
import com.sun.jersey.server.linking.Ref;
import database.models.HelpTopic;
import database.models.HelpTopicCollection;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ExistsInDB;
import web.validation.Add;
import web.validation.Update;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement(name = "HelpTopic")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_HelpTopic implements HasNumericId {
    /**
     * The id for this topic. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("help/topics/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The name of the topic.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotEmpty(message = "The name must not be empty or null", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Name")
    private String name;

    /**
     * The help text.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotEmpty(message = "The text must not be empty or null", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "HelpText")
    private String helpText;

    /**
     * The topic collection this topic belongs to, if any. Only the id needs to be populated on the collection object if it's specified.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request if you want to keep the data as it was.
     */
    @ExistsInDB(entity = HelpTopicCollection.class, optional = true, message = "No such collection")
    @XmlElement(name = "Collection")
    private RS_HelpTopicCollection collection;

    public RS_HelpTopic() {
    }

    private RS_HelpTopic(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    private RS_HelpTopic(final HelpTopic topic) {
        this(topic.getId(), topic.getName());
        this.helpText = topic.getHelpText();
        this.collection = topic.getCollection() == null ? null : RS_HelpTopicCollection.fromHelpTopicCollection(topic.getCollection(), false);
    }

    public static RS_HelpTopic fromHelpTopic(final HelpTopic topic, final boolean full) {
        return full ? new RS_HelpTopic(topic) : new RS_HelpTopic(topic.getId(), topic.getName());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHelpText() {
        return helpText;
    }

    public RS_HelpTopicCollection getCollection() {
        return collection;
    }
}
