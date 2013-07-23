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
import database.models.Army;
import database.models.Province;
import tools.validation.ExistsInDB;
import tools.validation.IsValidEnumName;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.Future;
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

@XmlRootElement(name = "Army")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Army implements HasNumericId {
    /**
     * The id for this army. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("armies/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The province to which the army belongs. You do not have to send in a whole province object here, as the id is the only value that will
     * be used. That id must, however, point to an actual province in the database.
     * <p/>
     * This value is not updatable, so new province => new army. That also means you can leave it out when only doing update requests. On
     * add requests it's mandatory.
     */
    @NotNull(message = "You must specify a province", groups = {Add.class})
    @ExistsInDB(entity = Province.class, message = "There's no such province")
    @XmlElement(required = true, name = "Province")
    private RS_Province province;

    /**
     * The number of the army.
     * <p/>
     * This value is not updatable, so new army number => new army. That also means you can leave it out when only doing update requests. On
     * add requests it's mandatory.
     */
    @NotNull(message = "Army number must not be null", groups = {Add.class})
    @Min(value = 1, message = "Army number must be at least 1", groups = {Add.class})
    @XmlElement(name = "ArmyNumber")
    private Integer armyNumber;

    /**
     * The type of army.
     * <p/>
     * This value is not updatable, so new army type => new army. That also means you can leave it out when only doing update requests. On
     * add requests it's mandatory.
     */
    @NotNull(message = "Army type must not be null", groups = {Add.class})
    @IsValidEnumName(enumType = Army.ArmyType.class, message = "No such army type", groups = {Add.class})
    @XmlElement(name = "Type")
    private String type;

    /**
     * The amount of soldiers in the army.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (0). Only zero and positive values are allowed.
     */
    @Min(value = 0, message = "Negative value is not valid for soldiers", groups = {Add.class, Update.class})
    @XmlElement(name = "Soldiers")
    private Integer soldiers;

    /**
     * The amount of offensive specialists in the army.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (0). Only zero and positive values are allowed.
     */
    @Min(value = 0, message = "Negative value is not valid for offSpecs", groups = {Add.class, Update.class})
    @XmlElement(name = "OffSpecs")
    private Integer offSpecs;

    /**
     * The amount of defensive specialists in the army.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (0). Only zero and positive values are allowed.
     */
    @Min(value = 0, message = "Negative value is not valid for defSpecs", groups = {Add.class, Update.class})
    @XmlElement(name = "DefSpecs")
    private Integer defSpecs;

    /**
     * The amount of elites in the army.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (0). Only zero and positive values are allowed.
     */
    @Min(value = 0, message = "Negative value is not valid for elites", groups = {Add.class, Update.class})
    @XmlElement(name = "Elites")
    private Integer elites;

    /**
     * The amount of war horses in the army.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (0). Only zero and positive values are allowed.
     */
    @Min(value = 0, message = "Negative value is not valid for warHorses", groups = {Add.class, Update.class})
    @XmlElement(name = "WarHorses")
    private Integer warHorses;

    /**
     * The amount of thieves in the army (only interesting for armies in training).
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (0). Only zero and positive values are allowed.
     */
    @Min(value = 0, message = "Negative value is not valid for thieves", groups = {Add.class, Update.class})
    @XmlElement(name = "Thieves")
    private Integer thieves;

    /**
     * The amount of modified offense in the army.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (0). Only zero and positive values are allowed.
     */
    @Min(value = 0, message = "Negative value is not valid for modOffense", groups = {Add.class, Update.class})
    @XmlElement(name = "ModOffense")
    private Integer modOffense;

    /**
     * The amount of modified defense in the army.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (0). Only zero and positive values are allowed.
     */
    @Min(value = 0, message = "Negative value is not valid for modDefense", groups = {Add.class, Update.class})
    @XmlElement(name = "ModDefense")
    private Integer modDefense;

    /**
     * The amount of generals in the army.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (1). Only positive values are allowed.
     */
    @Min(value = 1, message = "The amount of generals must be at least 1", groups = {Add.class, Update.class})
    @XmlElement(name = "Generals")
    private Integer generals;

    /**
     * The amount of land gained by the army (plunders and such should not be included here, only land gains).
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request, unless you're ok with the default value (0). Only zero and positive values are allowed.
     */
    @Min(value = 0, message = "Negative value is not valid for gain", groups = {Add.class, Update.class})
    @XmlElement(name = "Gain")
    private Integer gain;

    /**
     * The date and time at which the army will return. Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. The returning date isn't mandatory, since some army types
     * actually stay at home at all times, so this value may be left out
     * completely if desired (leaving it out will set it to null, regardless of what it was before). If you do specify it, it must be a date & time
     * in the future.
     */
    @Future(message = "Return time must be in the future", groups = {Add.class, Update.class})
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Returning")
    private Date returning;

    public RS_Army() {
    }

    private RS_Army(final Long id, final RS_Province province) {
        this.id = id;
        this.province = province;
    }

    private RS_Army(final Army army) {
        this(army.getId(), RS_Province.fromProvince(army.getProvince(), false));
        this.armyNumber = army.getArmyNumber();
        this.type = army.getType().getTypeName();
        this.soldiers = army.getSoldiers();
        this.offSpecs = army.getOffSpecs();
        this.defSpecs = army.getDefSpecs();
        this.elites = army.getElites();
        this.warHorses = army.getWarHorses();
        this.thieves = army.getThieves();
        this.modOffense = army.getModOffense();
        this.modDefense = army.getModDefense();
        this.generals = army.getGenerals();
        this.gain = army.getLandGained();
        this.returning = army.getReturningDate();
    }

    public static RS_Army fromArmy(final Army army, final boolean full) {
        return full ? new RS_Army(army) : new RS_Army(army.getId(), RS_Province.fromProvince(army.getProvince(), false));
    }

    public static void toArmy(final Army army, final RS_Army updatedArmy) {
        army.setReturningDate(updatedArmy.getReturning());
        army.setLandGained(updatedArmy.getGain());
        army.setDefSpecs(updatedArmy.getDefSpecs());
        army.setElites(updatedArmy.getElites());
        army.setGenerals(updatedArmy.getGenerals());
        army.setLandGained(updatedArmy.getGain());
        army.setModDefense(updatedArmy.getModDefense());
        army.setModOffense(updatedArmy.getModOffense());
        army.setOffSpecs(updatedArmy.getOffSpecs());
        army.setSoldiers(updatedArmy.getSoldiers());
        army.setThieves(updatedArmy.getThieves());
        army.setWarHorses(updatedArmy.getWarHorses());
    }

    @Override
    public Long getId() {
        return id;
    }

    public RS_Province getProvince() {
        return province;
    }

    public int getArmyNumber() {
        return firstNonNull(armyNumber, 0);
    }

    public String getType() {
        return type;
    }

    public int getSoldiers() {
        return firstNonNull(soldiers, 0);
    }

    public int getOffSpecs() {
        return firstNonNull(offSpecs, 0);
    }

    public int getDefSpecs() {
        return firstNonNull(defSpecs, 0);
    }

    public int getElites() {
        return firstNonNull(elites, 0);
    }

    public int getWarHorses() {
        return firstNonNull(warHorses, 0);
    }

    public int getThieves() {
        return firstNonNull(thieves, 0);
    }

    public int getModOffense() {
        return firstNonNull(modOffense, 0);
    }

    public int getModDefense() {
        return firstNonNull(modDefense, 0);
    }

    public int getGenerals() {
        return firstNonNull(generals, 0);
    }

    public int getGain() {
        return firstNonNull(gain, 0);
    }

    public Date getReturning() {
        return returning;
    }
}
