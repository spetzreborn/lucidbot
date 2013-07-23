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

import api.database.models.BotUser;
import com.google.common.collect.Lists;
import database.models.Bindings;
import database.models.BindingsContainer;
import database.models.Personality;
import database.models.Race;
import web.tools.ISODateTimeAdapter;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlType(name = "Bindings")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Bindings implements BindingsContainer {
    /**
     * The users to bind to, if any. Must be specified, but may be empty. The user objects in the list need only have id's, nothing else.
     * <p/>
     * Updatable, meaning you need to specify it each time unless you want to lose data.
     */
    @XmlElementWrapper(name = "Users", nillable = true, required = true)
    @XmlElement(required = true, name = "User")
    private List<RS_User> users;

    /**
     * The races to bind to, if any. Must be specified, but may be empty. The race objects in the list need only have id's, nothing else.
     * <p/>
     * Updatable, meaning you need to specify it each time unless you want to lose data.
     */
    @XmlElementWrapper(name = "Races", nillable = true, required = true)
    @XmlElement(required = true, name = "Race")
    private List<RS_Race> races;

    /**
     * The personalities to bind to, if any. Must be specified, but may be empty. The personalities objects in the list need only have id's, nothing else.
     * <p/>
     * Updatable, meaning you need to specify it each time unless you want to lose data.
     */
    @XmlElementWrapper(name = "Personalities", nillable = true, required = true)
    @XmlElement(required = true, name = "Personality")
    private List<RS_Personality> personalities;

    /**
     * If the type the binding is used for is a publishable resource, this may be used to set a publish date. Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request. Only a date & time that's in the future is valid.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, nillable = true, name = "Publish")
    private Date publish;

    /**
     * If the type the binding is used for is a publishable resource, this may be used to set an expiry date. Format (ISO_8601) example: 2010-05-27 18:33:56+0800
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request. Only a date & time that's in the future is valid.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(required = true, nillable = true, name = "Expire")
    private Date expire;

    /**
     * A binding for admin users only.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @XmlElement(required = true, name = "AdminsOnly")
    private Boolean adminsOnly;

    public RS_Bindings() {
    }

    private RS_Bindings(final List<RS_User> users, final List<RS_Race> races, final List<RS_Personality> personalities, final Date publish,
                        final Date expire, final boolean adminsOnly) {
        this.users = users;
        this.races = races;
        this.personalities = personalities;
        this.publish = publish;
        this.expire = expire;
        this.adminsOnly = adminsOnly;
    }

    public static RS_Bindings fromBindings(final Bindings bindings) {
        List<RS_User> userList = Lists.newArrayList();
        for (BotUser user : bindings.getUsers()) {
            userList.add(RS_User.fromBotUser(user, false));
        }
        List<RS_Race> raceList = Lists.newArrayList();
        for (Race race : bindings.getRaces()) {
            raceList.add(RS_Race.fromRace(race, false));
        }
        List<RS_Personality> personalityList = Lists.newArrayList();
        for (Personality personality : bindings.getPersonalities()) {
            personalityList.add(RS_Personality.fromPersonality(personality, false));
        }
        return new RS_Bindings(userList, raceList, personalityList, bindings.getPublishDate(), bindings.getExpiryDate(),
                bindings.isAdminsOnly());
    }

    @Override
    public Set<RS_User> getUsers() {
        return new HashSet<>(toEmptyListIfNull(users));
    }

    @Override
    public Set<RS_Race> getRaces() {
        return new HashSet<>(toEmptyListIfNull(races));
    }

    @Override
    public Set<RS_Personality> getPersonalities() {
        return new HashSet<>(toEmptyListIfNull(personalities));
    }

    @Override
    public Date getPublishDate() {
        return publish;
    }

    @Override
    public Date getExpiryDate() {
        return expire;
    }

    @Override
    public boolean isAdminsOnly() {
        return adminsOnly != null && adminsOnly;
    }
}
