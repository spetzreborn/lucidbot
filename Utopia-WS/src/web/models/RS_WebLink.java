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
import database.models.WebLink;
import org.hibernate.validator.constraints.NotEmpty;
import web.validation.Add;
import web.validation.Update;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement(name = "WebLink")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_WebLink implements HasNumericId {
    /**
     * The id for this link. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("weblinks/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The name/description of the link.
     * <p/>
     * This value is updatable and is overwritten in update operations, so always send it in if you want to keep the data.
     */
    @NotEmpty(message = "The name must not be null or empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Name")
    private String name;

    /**
     * The link itself.
     * <p/>
     * This value is updatable and is overwritten in update operations, so always send it in if you want to keep the data.
     */
    @NotEmpty(message = "The link must not be null or empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "URL")
    private String url;

    public RS_WebLink() {
    }

    private RS_WebLink(final Long id, final String name, final String url) {
        this.id = id;
        this.name = name;
        this.url = url;
    }

    public static RS_WebLink fromWebLink(final WebLink link) {
        return new RS_WebLink(link.getId(), link.getName(), link.getLink());
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
