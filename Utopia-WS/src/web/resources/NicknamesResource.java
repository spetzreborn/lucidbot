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

package web.resources;

import api.database.daos.BotUserDAO;
import api.database.daos.NicknameDAO;
import api.database.models.BotUser;
import api.database.models.Nickname;
import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import web.documentation.Documentation;
import web.models.RS_Nickname;
import web.tools.WebContext;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@ValidationEnabled
@Path("users/nicknames")
public class NicknamesResource {
    private final NicknameDAO nicknameDAO;
    private final Provider<BotUserDAO> userDAOProvider;

    @Inject
    public NicknamesResource(final NicknameDAO nicknameDAO,
                             final Provider<BotUserDAO> userDAOProvider) {
        this.nicknameDAO = nicknameDAO;
        this.userDAOProvider = userDAOProvider;
    }

    @Documentation("Adds a new nickname for the current user, provided the new nick isn't already taken. Returns the saved object")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Nickname addNickname(@Documentation(value = "The new nickname", itemName = "newNickname")
                                   @Valid final RS_Nickname newNickname,
                                   @Context final WebContext webContext) {
        Nickname existing = nicknameDAO.getNickname(newNickname.getName());
        checkArgument(existing == null, "That nickname is already taken");

        BotUser user = webContext.getBotUser();
        Nickname nickname = new Nickname(newNickname.getName(), user);
        user.getNickList().add(nickname);
        return RS_Nickname.fromNickname(nickname);
    }

    @Documentation("Returns the nickname with the specified id")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Nickname getNickname(@PathParam("id") final long id) {
        Nickname nickname = nicknameDAO.getNickname(id);

        if (nickname == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        return RS_Nickname.fromNickname(nickname);
    }

    @Documentation("Returns all nicknames, or optionally just the ones for the specified user")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Nickname>> getNicknames(@Documentation("The id of the user whose nicks to get")
                                                     @QueryParam("userId")
                                                     final Long userId) {
        List<RS_Nickname> nicknames = new ArrayList<>();
        if (userId == null) {
            for (Nickname nickname : nicknameDAO.getAllNicknames()) {
                nicknames.add(RS_Nickname.fromNickname(nickname));
            }
        } else {
            BotUser user = userDAOProvider.get().getUser(userId);
            checkNotNull(user, "There's no such user");
            for (Nickname nickname : user.getNickList()) {
                nicknames.add(RS_Nickname.fromNickname(nickname));
            }
        }
        return JResponse.ok(nicknames).build();
    }

    @Documentation("Deletes the specified nickname, provided it isn't the user's main nick. You can only delete your own nicknames")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteNickname(@PathParam("id") final long id,
                               @Context final WebContext webContext) {
        BotUser user = webContext.getBotUser();

        Nickname nickname = nicknameDAO.getNickname(id);
        if (nickname == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        if (!nickname.getUser().equals(user)) throw new WebApplicationException(Response.Status.FORBIDDEN);
        checkArgument(!user.getMainNick().equals(nickname.getName()), "You may not delete your main nick");

        nicknameDAO.delete(nickname);
    }
}
