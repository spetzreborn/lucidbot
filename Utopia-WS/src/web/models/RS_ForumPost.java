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
import database.models.ForumPost;
import database.models.ForumThread;
import org.hibernate.validator.constraints.NotEmpty;
import tools.validation.ExistsInDB;
import web.tools.ISODateTimeAdapter;
import web.validation.Add;
import web.validation.Update;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.URI;
import java.util.Date;

@XmlRootElement(name = "ForumPost")
@XmlAccessorType(XmlAccessType.FIELD)
public class RS_ForumPost implements HasNumericId {
    /**
     * The id for this post. The id is set by the database, so clients will only use it in the URL's.
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
    @Ref("forum/posts/{id}")
    @XmlElement(name = "Link")
    private URI link;

    /**
     * The user who made the post. For display purposes only, so it never needs to be specified. On add operations the user is taken
     * from the context (who is logged in) automatically, since you can't post things for other users.
     */
    @XmlElement(required = true, name = "User")
    private RS_User user;

    /**
     * The thread the post is in. You do not have to send in a whole thread object here, as the id is the only value that will
     * be used. That id must, however, point to an actual thread in the database.
     * <p/>
     * This value is not updatable. That means you can leave it out when only doing update requests. On add requests it's mandatory.
     */
    @NotNull(message = "Thread must not be null", groups = {Add.class})
    @ExistsInDB(entity = ForumThread.class, message = "No such thread", groups = {Add.class})
    @XmlElement(required = true, name = "Thread")
    private RS_ForumThread thread;

    /**
     * The date and time at which the post was first added.
     * <p/>
     * This value is not updatable and is set automatically on add operations, so it never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "Added")
    private Date added;

    /**
     * The date and time at which the post was last edited.
     * <p/>
     * This value is updatable but is set automatically on update operations, so it never needs to be specified.
     */
    @XmlJavaTypeAdapter(ISODateTimeAdapter.class)
    @XmlElement(name = "LastEdited")
    private Date lastEdited;

    /**
     * The actual post content.
     * <p/>
     * This value is updatable, so it will always be overwritten when you do update calls. I.e. it always has to be specified, regardless of it's an
     * update or add request.
     */
    @NotEmpty(message = "The post may not be empty", groups = {Add.class, Update.class})
    @XmlElement(required = true, name = "Post")
    private String post;

    public RS_ForumPost() {
    }

    private RS_ForumPost(final Long id,
                         final RS_User user,
                         final RS_ForumThread thread,
                         final Date added,
                         final Date lastEdited,
                         final String post) {
        this.id = id;
        this.user = user;
        this.thread = thread;
        this.added = added;
        this.lastEdited = lastEdited;
        this.post = post;
    }

    public static RS_ForumPost fromForumPost(final ForumPost post) {
        return new RS_ForumPost(post.getId(), RS_User.fromBotUser(post.getUser(), false),
                RS_ForumThread.fromForumThread(post.getThread(), false), post.getPosted(), post.getLastEdited(),
                post.getPost());
    }

    public static void toForumPost(final ForumPost forumPost, final RS_ForumPost updatedPost) {
        forumPost.setLastEdited(new Date());
        forumPost.setPost(updatedPost.post);
    }

    @Override
    public Long getId() {
        return id;
    }

    public RS_User getUser() {
        return user;
    }

    public RS_ForumThread getThread() {
        return thread;
    }

    public Date getAdded() {
        return added;
    }

    public Date getLastEdited() {
        return lastEdited;
    }

    public String getPost() {
        return post;
    }
}
