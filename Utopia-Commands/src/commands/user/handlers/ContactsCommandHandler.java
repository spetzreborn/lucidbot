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

package commands.user.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.AccessLevel;
import api.database.models.BotUser;
import api.database.models.ContactInformation;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.common.ReflectionUtil;
import spi.commands.CommandHandler;
import spi.filters.Filter;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Iterator;

import static api.tools.text.StringUtil.lowerCase;

public class ContactsCommandHandler implements CommandHandler {
    private final BotUserDAO userDAO;

    @Inject
    public ContactsCommandHandler(final BotUserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            if (params.isEmpty() || params.containsKey("user")) {
                BotUser user = params.isEmpty() ? context.getBotUser() : userDAO.getClosestMatch(params.getParameter("user"));
                if (user == null) return CommandResponse.errorResponse("No user found");
                return CommandResponse.resultResponse("user", user);
            } else {
                BotUser specifiedUser = params.containsKey("optionalUser") ? userDAO.getUser(params.getParameter("optionalUser")) : null;
                if (params.containsKey("optionalUser") && specifiedUser == null)
                    return CommandResponse.errorResponse("No such user found");

                BotUser currentUser = context.getBotUser();
                if (specifiedUser != null && !specifiedUser.equals(currentUser) && !AccessLevel.ADMIN.allows(context.getUser(), context.getChannel()))
                    return CommandResponse.errorResponse("Only admins can add contacts for other users");

                BotUser actualUser = specifiedUser == null ? currentUser : specifiedUser;
                if (params.containsKey("add")) {
                    String type;
                    if (params.containsKey("knownType")) {
                        type = lowerCase(params.getParameter("knownType"));
                        boolean wasSet = ReflectionUtil
                                .setMethodOrFieldValue(actualUser, type, String.class, params.getParameter("contact"));
                        if (!wasSet) return CommandResponse.errorResponse("Failed to set value");
                    } else {
                        type = params.getParameter("unknownType");
                        addOrUpdateContactInformation(actualUser, new ContactInformation(actualUser, type, params.getParameter("contact")));
                    }
                    userDAO.save(actualUser);
                    return CommandResponse.resultResponse("type", type, "value", params.getParameter("contact"));
                } else {
                    String type;
                    if (params.containsKey("knownType")) {
                        type = lowerCase(params.getParameter("knownType"));
                        ReflectionUtil.setMethodOrFieldValue(actualUser, type, String.class, null);
                    } else {
                        type = params.getParameter("unknownType");
                        removeContactInformation(actualUser, params.getParameter("unknownType"));
                    }
                    userDAO.save(actualUser);
                    return CommandResponse.resultResponse("type", type);
                }
            }
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static void removeContactInformation(BotUser actualUser, String type) {
        for (Iterator<ContactInformation> iter = actualUser.getContactInformation().iterator(); iter.hasNext(); ) {
            if (iter.next().getInformationType().equalsIgnoreCase(type)) {
                iter.remove();
                return;
            }
        }
    }

    private static void addOrUpdateContactInformation(BotUser user, ContactInformation proposedNew) {
        for (ContactInformation contactInformation : user.getContactInformation()) {
            if (contactInformation.getInformationType().equalsIgnoreCase(proposedNew.getInformationType())) {
                contactInformation.setInformation(proposedNew.getInformation());
                return;
            }
        }
        user.getContactInformation().add(proposedNew);
    }
}
