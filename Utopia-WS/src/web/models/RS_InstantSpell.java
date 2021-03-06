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
import com.sun.jersey.server.linking.Ref;
import database.models.InstantSpell;
import database.models.Province;
import database.models.SpellType;
import tools.validation.ExistsInDB;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

import static com.google.common.base.Objects.firstNonNull;

@XmlRootElement(name = "InstantSpell")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_InstantSpell implements HasNumericId {
    /**
     * The id for this op. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("spells/instant/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user casting the spell. Only the id needs to be populated on the user object.
     */
    @NotNull(message = "The casting user must not be null")
    @ExistsInDB(entity = BotUser.class, message = "No such user")
    @XmlElement(required = true, name = "Caster")
    private RS_User caster;

    /**
     * The target province. Only the id needs to be populated on the province object.
     */
    @NotNull(message = "The target province must not be null")
    @ExistsInDB(entity = Province.class, message = "No such province")
    @XmlElement(required = true, name = "Province")
    private RS_Province province;

    /**
     * The amount of times the spell was cast. What you get from the server will always be totals for this value (how many of this spell
     * has the user ever casted), but when you're sending in spell results you should only specify how many was cast this time (they're added to the
     * total automatically).
     */
    @NotNull(message = "Amount must not be null")
    @Min(value = 1, message = "The amount must be at least 1")
    @XmlElement(required = true, name = "Amount")
    private Integer amount;

    /**
     * The total damage that was done. What you get from the server will always be totals for this value (the total damage over time for this user),
     * but when you're sending in spell results you should only specify how much damage was done this time (they're added to the
     * total automatically).
     */
    @NotNull(message = "Damage must not be null")
    @Min(value = 0, message = "The damage cannot be negative")
    @XmlElement(required = true, name = "Damage")
    private Integer damage;

    /**
     * The type of spell this is. Only the id needs to be populated on the spell type object.
     */
    @NotNull(message = "The spell type must not be null")
    @ExistsInDB(entity = SpellType.class, message = "No such spell type")
    @XmlElement(required = true, name = "Type")
    private RS_SpellType type;

    public RS_InstantSpell() {
    }

    private RS_InstantSpell(final Long id,
                            final RS_User caster,
                            final RS_Province province,
                            final int amount,
                            final int damage,
                            final RS_SpellType type) {
        this.id = id;
        this.caster = caster;
        this.province = province;
        this.amount = amount;
        this.damage = damage;
        this.type = type;
    }

    public static RS_InstantSpell fromInstantSpell(final InstantSpell spell) {
        return new RS_InstantSpell(spell.getId(), RS_User.fromBotUser(spell.getCommitter(), false),
                RS_Province.fromProvince(spell.getProvince(), false), spell.getAmount(), spell.getDamage(),
                RS_SpellType.fromSpellType(spell.getType(), false));
    }

    public Long getId() {
        return id;
    }

    public RS_User getCaster() {
        return caster;
    }

    public RS_Province getProvince() {
        return province;
    }

    public int getAmount() {
        return firstNonNull(amount, 0);
    }

    public int getDamage() {
        return firstNonNull(damage, 0);
    }

    public RS_SpellType getType() {
        return type;
    }
}
