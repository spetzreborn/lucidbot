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
import database.models.Attack;
import database.models.AttackType;
import database.models.Province;
import tools.validation.ExistsInDB;
import tools.validation.IsValidEnumName;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

import static com.google.common.base.Objects.firstNonNull;

@XmlRootElement(name = "Attack")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Attack implements HasNumericId {
    /**
     * The id for this attack. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("attacks/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The province that performed the attack. You do not have to send in a whole province object here, as the id is the only value that will
     * be used. That id must, however, point to an actual province in the database.
     * <p/>
     * This value is not updatable. That means you can leave it out when only doing update requests. On add requests it's mandatory.
     */
    @NotNull(message = "The attacking province must not be null", groups = {Add.class})
    @ExistsInDB(entity = Province.class, message = "The attacking province cannot be found", groups = {Add.class})
    @XmlElement(required = true, name = "Attacker")
    private RS_Province attacker;

    /**
     * The province that was the target of the attack. You do not have to send in a whole province object here, as the id is the only value that will
     * be used. That id must, however, point to an actual province in the database.
     * <p/>
     * This value is not updatable. That means you can leave it out when only doing update requests. On add requests it's mandatory.
     */
    @NotNull(message = "The defending province must not be null", groups = {Add.class})
    @ExistsInDB(entity = Province.class, message = "The defending province cannot be found")
    @XmlElement(required = true, name = "Defender")
    private RS_Province defender;

    /**
     * Whatever was gained in the attack. This value isn't used for anything other than display purposes by the bot, so it's fine
     * to include explanatory text here if desired (just not too long).
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request (unless you're fine with an empty String, at which case just leave it out).
     */
    @NotNull(message = "The gain must not be null", groups = {Add.class, Update.class})
    @XmlElement(name = "Gain")
    private String gain = "";

    /**
     * How many troops were killed or captured in the attack by the attacker.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request (unless you're fine with 0, at which case just leave it out).
     */
    @Min(value = 0, message = "Kills cannot be negative", groups = {Add.class, Update.class})
    @XmlElement(name = "Kills")
    private Integer kills;

    /**
     * How much offense was sent in the attack.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request (unless you're fine with 0, at which case just leave it out).
     */
    @Min(value = 0, message = "Offense sent cannot be negative", groups = {Add.class, Update.class})
    @XmlElement(name = "OffenseSent")
    private Integer offenseSent;

    /**
     * The type of attack
     * <p/>
     * This value is not updatable, so new type => new attack. That also means you can leave it out when only doing update requests. On
     * add requests it's mandatory.
     */
    @NotNull(message = "The attack type must not be null", groups = {Add.class})
    @IsValidEnumName(enumType = AttackType.class, message = "No such attack type", groups = {Add.class})
    @XmlElement(required = true, name = "Type")
    private String type;

    /**
     * The date and time at which the attack was performed.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, name = "Added")
    private Date added;

    /**
     * Wether the attacker got plagued as a result of this attack.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request (unless you're fine with false, at which case just leave it out).
     */
    @XmlElement(name = "GotPlagued")
    private Boolean gotPlagued;

    /**
     * Wether the attacker spread plague into the defending province in the attack.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request (unless you're fine with false, at which case just leave it out).
     */
    @XmlElement(name = "SpreadPlague")
    private Boolean spreadPlague;

    public RS_Attack() {
    }

    public RS_Attack(final Long id, final RS_Province attacker, final RS_Province defender, final String gain, final int kills,
                     final int offenseSent, final String type, final Date added, final boolean gotPlagued, final boolean spreadPlague) {
        this.id = id;
        this.attacker = attacker;
        this.defender = defender;
        this.gain = gain;
        this.kills = kills;
        this.offenseSent = offenseSent;
        this.type = type;
        this.added = added;
        this.gotPlagued = gotPlagued;
        this.spreadPlague = spreadPlague;
    }

    public static RS_Attack fromAttack(final Attack attack) {
        return new RS_Attack(attack.getId(), RS_Province.fromProvince(attack.getAttacker(), false),
                RS_Province.fromProvince(attack.getTarget(), false), attack.getGain(), attack.getKills(),
                attack.getOffenseSent(), attack.getType().getName(), attack.getTimeOfAttack(), attack.isGotPlagued(),
                attack.isSpreadPlague());
    }

    public static void toAttack(final Attack attack, final RS_Attack updatedAttack) {
        attack.setGain(updatedAttack.getGain());
        attack.setGotPlagued(updatedAttack.isGotPlagued());
        attack.setKills(updatedAttack.getKills());
        attack.setOffenseSent(updatedAttack.getOffenseSent());
        attack.setSpreadPlague(updatedAttack.isSpreadPlague());
    }

    @Override
    public Long getId() {
        return id;
    }

    public RS_Province getAttacker() {
        return attacker;
    }

    public RS_Province getDefender() {
        return defender;
    }

    public String getGain() {
        return gain;
    }

    public int getKills() {
        return firstNonNull(kills, 0);
    }

    public int getOffenseSent() {
        return firstNonNull(offenseSent, 0);
    }

    public String getType() {
        return type;
    }

    public Date getAdded() {
        return added;
    }

    public boolean isGotPlagued() {
        return gotPlagued != null && gotPlagued;
    }

    public boolean isSpreadPlague() {
        return spreadPlague != null && spreadPlague;
    }
}
