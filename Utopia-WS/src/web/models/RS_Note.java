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
import database.models.Note;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ValidBindings;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "Note")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Note implements HasNumericId {
    /**
     * The id for this note. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("notes/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The date and time at which the note was added.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Added")
    private Date added;

    /**
     * Which user added the note.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlElement(required = true, name = "AddedBy")
    private String addedBy;

    /**
     * The actual note content.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotEmpty(message = "The message must not be null or empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Message")
    private String message;

    /**
     * Bindings for the event, allowing you to specify which users (as an example) the event is for.
     * <p/>
     * Not updatable, so it will be ignored for update operations. Not mandatory for add operations either.
     */
    @ValidBindings(nillable = true, message = "Invalid bindings", groups = {Add.class})
    @XmlElement(name = "Bindings")
    private RS_Bindings bindings;

    public RS_Note() {
    }

    private RS_Note(final Long id, final Date added, final String addedBy, final String message, final RS_Bindings bindings) {
        this.id = id;
        this.added = added;
        this.addedBy = addedBy;
        this.message = message;
        this.bindings = bindings;
    }

    public static RS_Note fromNote(final Note note) {
        return new RS_Note(note.getId(), note.getAdded(), note.getAddedBy(), note.getMessage(),
                RS_Bindings.fromBindings(note.getBindings()));
    }

    public Long getId() {
        return id;
    }

    public Date getAdded() {
        return added;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public String getMessage() {
        return message;
    }

    public RS_Bindings getBindings() {
        return bindings;
    }
}
