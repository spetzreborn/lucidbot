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
import api.database.models.AccessLevel;
import com.google.common.collect.Lists;
import com.sun.jersey.server.linking.Ref;
import database.models.ForumSection;
import database.models.ForumThread;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.IsValidEnumName;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlRootElement(name = "ForumSection")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_ForumSection implements HasNumericId {
    /**
     * The id for this section. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("forum/sections/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The name of the section.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotEmpty(message = "The name must not be null or empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Name")
    private String name;

    /**
     * The minimum access level for the section (public, user or admin), meaning who is allowed to view and use it.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotNull(message = "The access level may not be null", groups = {Add.class, Update.class})
    @IsValidEnumName(enumType = AccessLevel.class, message = "No such access level", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "MinimumAccessLevel")
    private String minimumAccessLevel;

    /**
     * The threads in this section. For display purposes only, so there's no need to send this in, because it will be ignored.
     */
    @XmlElementWrapper(name = "Threads")
    @XmlElement(name = "ForumThread")
    private List<RS_ForumThread> threads;

    public RS_ForumSection() {
    }

    private RS_ForumSection(final Long id, final String name, final String minimumAccessLevel) {
        this.id = id;
        this.name = name;
        this.minimumAccessLevel = minimumAccessLevel;
    }

    private RS_ForumSection(final ForumSection section) {
        this(section.getId(), section.getName(), section.getAccessLevelName());
        this.threads = Lists.newArrayList();
        for (ForumThread thread : section.getThreads()) {
            threads.add(RS_ForumThread.fromForumThread(thread, false));
        }
    }

    public static RS_ForumSection fromForumSection(final ForumSection section, final boolean full) {
        return full ? new RS_ForumSection(section) : new RS_ForumSection(section.getId(), section.getName(), section.getAccessLevelName());
    }

    public static void toForumSection(final ForumSection section, final RS_ForumSection updatedSection) {
        section.setName(updatedSection.name);
        section.setMinimumAccessLevel(AccessLevel.fromName(updatedSection.minimumAccessLevel));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMinimumAccessLevel() {
        return minimumAccessLevel;
    }

    public List<RS_ForumThread> getThreads() {
        return toEmptyListIfNull(threads);
    }
}
