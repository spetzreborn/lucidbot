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
import api.database.models.BotUser;
import com.google.common.collect.Lists;
import com.sun.jersey.server.linking.Ref;
import database.models.Province;
import database.models.Target;
import tools.validation.ExistsInDB;
import tools.validation.IsValidEnumName;
import tools.validation.ValidBindings;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlRootElement(name = "Target")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Target implements HasNumericId {
    /**
     * The id for this target. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("targets/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The target province. You do not have to send in a whole province object here, as the id is the only value that will
     * be used. That id must, however, point to an actual province in the database.
     * <p/>
     * This value is not updatable. That means you can leave it out when only doing update requests. On add requests it's mandatory.
     */
    @NotNull(message = "The target province must not be null", groups = {Add.class})
    @ExistsInDB(entity = Province.class, message = "The target province cannot be found", groups = {Add.class})
    @XmlElement(required = true, name = "Province")
    private RS_Province province;

    /**
     * The type of target this is.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotNull(message = "The target type must not be null", groups = {Add.class, Update.class})
    @IsValidEnumName(enumType = Target.TargetType.class, message = "No such target type", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * Details about the target. Could be something like target land to drop to etc. May be left out if desired.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @XmlElement(name = "Details")
    private String details = "";

    /**
     * The date and time at which the target was first added.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Added")
    private Date added;

    /**
     * Bindings for the target, allowing you to specify which users (as an example) the event is for.
     * <p/>
     * Not updatable, so it will be ignored for update operations. Not mandatory for add operations either.
     */
    @ValidBindings(message = "Invalid bindings", nillable = true, groups = {Add.class})
    @XmlElement(name = "Bindings")
    private RS_Bindings bindings;

    /**
     * The list of hitters for this target, ordered in the order they're hitting. Only set on adds, and updated by other hitter specific requests instead.
     */
    @Valid
    @XmlElementWrapper(required = true, name = "Hitters")
    @XmlElement(name = "TargetHitter")
    private List<RS_TargetHitter> hitters;

    public RS_Target() {
    }

    public RS_Target(final Long id, final RS_Province province, final String type, final String details) {
        this.id = id;
        this.province = province;
        this.type = type;
        this.details = details;
    }

    public RS_Target(final Target target) {
        this(target.getId(), RS_Province.fromProvince(target.getProvince(), false), target.getType().getName(), target.getDetails());
        this.added = target.getAdded();
        this.bindings = RS_Bindings.fromBindings(target.getBindings());
        this.hitters = Lists.newArrayList();
        List<BotUser> hittingUsers = target.getHitters();
        for (int i = 0; i < hittingUsers.size(); ++i) {
            BotUser hitter = hittingUsers.get(i);
            hitters.add(new RS_TargetHitter(this, RS_User.fromBotUser(hitter, false), i));
        }
    }

    public static RS_Target fromTarget(final Target target, final boolean full) {
        return full ? new RS_Target(target)
                : new RS_Target(target.getId(), RS_Province.fromProvince(target.getProvince(), false), target.getType().getName(),
                target.getDetails());
    }

    public Long getId() {
        return id;
    }

    public RS_Province getProvince() {
        return province;
    }

    public String getType() {
        return type;
    }

    public String getDetails() {
        return details;
    }

    public Date getAdded() {
        return added;
    }

    public RS_Bindings getBindings() {
        return bindings;
    }

    public List<RS_TargetHitter> getHitters() {
        return toEmptyListIfNull(hitters);
    }
}
