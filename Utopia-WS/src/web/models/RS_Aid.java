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
import database.models.Aid;
import database.models.AidImportanceType;
import database.models.AidType;
import database.models.Province;
import tools.validation.ExistsInDB;
import tools.validation.IsValidEnumName;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.Future;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

import static com.google.common.base.Objects.firstNonNull;

@XmlRootElement(name = "Aid")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Aid implements HasNumericId {
    /**
     * The id for this aid request/offer. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("aid/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The province asking for or offering the aid. You do not have to send in a whole province object here, as the id is the only value that will
     * be used. That id must, however, point to an actual province in the database.
     * <p/>
     * This value is not updatable, so new province => new aid request/offer. That also means you can leave it out when only doing update requests. On
     * add requests it's mandatory.
     */
    @NotNull(message = "You must specify a province", groups = {Add.class})
    @ExistsInDB(entity = Province.class, message = "There's no such province", groups = {Add.class})
    @XmlElement(required = true, name = "Province")
    private RS_Province province;

    /**
     * The type of aid request/offer, meaning what's being requested/offered. The type names are the same as on IRC when you use !addaid for example.
     * <p/>
     * This value is not updatable, so new type => new aid request/offer. That also means you can leave it out when only doing update requests. On
     * add requests it's mandatory.
     */
    @NotNull(message = "You must specify an aid type", groups = {Add.class})
    @IsValidEnumName(enumType = AidType.class, message = "There's no such aid type", groups = {Add.class})
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * The importance type of the request/offer. This defines either how urgent an aid request is, or if it's an offer instead. Just like for aid type
     * you can use the IRC values here.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotNull(message = "You must specify an aid importance type", groups = {Add.class, Update.class})
    @IsValidEnumName(enumType = AidImportanceType.class, message = "There's no such aid importance type", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Importance")
    private String importance;

    /**
     * The amount of aid requested/offered.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request. Only positive values are allowed (otherwise you should be removing the aid, not updating or adding it).
     */
    @NotNull(message = "You must specify an amount", groups = {Add.class, Update.class})
    @Min(value = 1, message = "The requested amount cannot be a negative number or zero", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Amount")
    private Integer amount;

    /**
     * The date and time at which the aid request/offer was added. Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request. Only a date & time that's in the past is valid.
     */
    @NotNull(message = "The added date may not be null", groups = {Add.class, Update.class})
    @Past(message = "The added date must be in the past", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, name = "Added")
    private Date added = new Date(System.currentTimeMillis() - 1);

    /**
     * The date and time at which the aid request/offer will expire. Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. Expiry date is not mandatory, so this value may be left out
     * completely if desired (leaving it out will set it to null, regardless of what it was before). If you do specify it, it must be a date & time
     * in the future.
     */
    @Future(message = "The expiry date must be in the future", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Expires")
    private Date expires;

    public RS_Aid() {
    }

    private RS_Aid(final Long id,
                   final RS_Province province,
                   final String type,
                   final String importance,
                   final int amount,
                   final Date added,
                   final Date expires) {
        this.id = id;
        this.province = province;
        this.type = type;
        this.importance = importance;
        this.amount = amount;
        this.added = added;
        this.expires = expires;
    }

    public static RS_Aid fromAid(final Aid aid) {
        return new RS_Aid(aid.getId(),
                RS_Province.fromProvince(aid.getProvince(), false),
                aid.getType().getTypeName(),
                aid.getImportanceType().getTypeName(),
                aid.getAmount(),
                aid.getAdded(),
                aid.getExpiryDate());
    }

    public static void toAid(final Aid aid, final RS_Aid updatedAid) {
        aid.setAmount(updatedAid.getAmount());
        aid.setExpiryDate(updatedAid.getExpires());
        aid.setAdded(updatedAid.getAdded());
        aid.setImportanceType(AidImportanceType.fromName(updatedAid.getImportance()));
    }

    @Override
    public Long getId() {
        return id;
    }

    public RS_Province getProvince() {
        return province;
    }

    public String getType() {
        return type;
    }

    public String getImportance() {
        return importance;
    }

    public int getAmount() {
        return firstNonNull(amount, 0);
    }

    public Date getAdded() {
        return added;
    }

    public Date getExpires() {
        return expires;
    }
}
