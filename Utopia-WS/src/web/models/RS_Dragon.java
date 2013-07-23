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
import database.models.Bonus;
import database.models.Dragon;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;
import static com.google.common.base.Preconditions.checkNotNull;

@XmlRootElement(name = "Dragon")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_Dragon implements HasNumericId {
    /**
     * The id for this dragon type. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("dragons/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The name of the dragon
     */
    @XmlElement(name = "Name")
    private String name;

    /**
     * The effects this dragon has
     */
    @XmlElementWrapper(required = true, name = "Bonuses")
    @XmlElement(name = "Bonus")
    private List<RS_Bonus> bonuses;

    public RS_Dragon() {
    }

    private RS_Dragon(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    private RS_Dragon(final Dragon dragon) {
        this(dragon.getId(), dragon.getName());
        this.bonuses = new ArrayList<>();
        for (Bonus bonus : dragon.getBonuses()) {
            this.bonuses.add(RS_Bonus.fromBonus(bonus));
        }
    }

    /**
     * Returns a RS_Dragon based on the specified Dragon
     *
     * @param dragon the dragon
     * @param full   whether to include all info, or just the minimal amount
     * @return a new RS_Dragon
     */
    public static RS_Dragon fromDragon(@Nonnull final Dragon dragon, final boolean full) {
        checkNotNull(dragon);
        return full ? new RS_Dragon(dragon) : new RS_Dragon(dragon.getId(), dragon.getName());
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<RS_Bonus> getBonuses() {
        return toEmptyListIfNull(bonuses);
    }
}
