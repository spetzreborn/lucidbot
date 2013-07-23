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
import com.google.common.collect.Lists;
import com.sun.jersey.server.linking.Ref;
import database.models.ForumPost;
import database.models.ForumSection;
import database.models.ForumThread;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ExistsInDB;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;
import java.util.List;

import static api.tools.collections.ListUtil.toEmptyListIfNull;

@XmlRootElement(name = "ForumThread")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_ForumThread implements HasNumericId {
    /**
     * The id for this thread. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("forum/threads/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The name of the thread.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotEmpty(message = "The thread name must not be null or empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Name")
    private String name;

    /**
     * The date and time at which the thread was first added.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Added")
    private Date added;

    /**
     * The nick of the user that first added the thread.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlElement(name = "AddedBy")
    private String addedBy;

    /**
     * The section the thread is in. You do not have to send in a whole thread object here, as the id is the only value that will
     * be used. That id must, however, point to an actual section in the database.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotNull(message = "The section must not be null", groups = {Add.class, Update.class})
    @ExistsInDB(entity = ForumSection.class, message = "No such section", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "ForumSection")
    private RS_ForumSection section;

    /**
     * Whether the thread is locked or not. Users cannot post in a locked thread.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @XmlElement(required = true, name = "Locked")
    private Boolean locked;

    /**
     * Whether the thread is stickied or not.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @XmlElement(required = true, name = "Stickied")
    private Boolean stickied;

    /**
     * All the posts in this thread. For display purposes, so there's no need to ever specify this, since it'll be ignored anyway.
     */
    @XmlElementWrapper(name = "Posts")
    @XmlElement(name = "ForumPost")
    private List<RS_ForumPost> posts;

    public RS_ForumThread() {
    }

    private RS_ForumThread(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    public RS_ForumThread(final ForumThread thread) {
        this(thread.getId(), thread.getName());
        this.added = thread.getCreated();
        this.addedBy = thread.getCreatedBy();
        this.section = RS_ForumSection.fromForumSection(thread.getSection(), false);
        this.locked = thread.isLocked();
        this.stickied = thread.isStickied();
        this.posts = Lists.newArrayList();
        for (ForumPost post : thread.getPosts()) {
            posts.add(RS_ForumPost.fromForumPost(post));
        }
    }

    public static RS_ForumThread fromForumThread(final ForumThread thread, final boolean full) {
        return full ? new RS_ForumThread(thread) : new RS_ForumThread(thread.getId(), thread.getName());
    }

    public static void toForumThread(final ForumThread thread, final RS_ForumThread updatedThread) {
        thread.setName(updatedThread.getName());
        thread.setLocked(updatedThread.isLocked());
        thread.setStickied(updatedThread.isStickied());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getAdded() {
        return added;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public RS_ForumSection getSection() {
        return section;
    }

    public boolean isLocked() {
        return locked != null && locked;
    }

    public boolean isStickied() {
        return stickied != null && stickied;
    }

    public List<RS_ForumPost> getPosts() {
        return toEmptyListIfNull(posts);
    }
}
