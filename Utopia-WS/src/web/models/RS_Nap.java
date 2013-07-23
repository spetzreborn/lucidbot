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

import database.models.Kingdom;
import org.hibernate.validator.constraints.NotEmpty;
import web.tools.ISODateTimeAdapter;

import javax.annotation.Nonnull;
import javax.validation.constraints.Future;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

@XmlRootElement(name = "Nap")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Nap {
    /**
     * The date the NAP was added. For display purposes only (it's automatically set and if you were to send something in here it'd be ignored).
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, name = "Added")
    private Date added;

    /**
     * The date and time at which the NAP will expire (should correspond to an hour change). Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. End date is not mandatory, so this value may be left out
     * completely if desired (leaving it out will set it to null, regardless of what it was before). If you do specify it, it must be a date & time
     * in the future.
     */
    @Future(message = "The end date must be in the future")
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Ends")
    private Date ends;

    /**
     * The NAP description. This is meant to contain the agreement details, for example notice periods and such.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls.
     */
    @NotEmpty(message = "The NAP description may not be null or empty")
    @XmlElement(required = true, name = "Description")
    private String description;

    public RS_Nap() {
    }

    private RS_Nap(final Kingdom kingdom) {
        this.added = kingdom.getNapAdded();
        this.ends = kingdom.getNapEndDate();
        this.description = kingdom.getNapDescription();
    }

    /**
     * Returns a nap if the specified Kingdom actually has one, otherwise null
     *
     * @param kingdom the kingdom
     * @return a new nap or null
     */
    public static RS_Nap fromKingdom(@Nonnull final Kingdom kingdom) {
        checkNotNull(kingdom);
        return kingdom.getNapAdded() == null ? null : new RS_Nap(kingdom);
    }

    public Date getAdded() {
        return added;
    }

    public Date getEnds() {
        return ends;
    }

    public String getDescription() {
        return description;
    }
}
