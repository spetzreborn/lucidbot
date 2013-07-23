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

package web.tools;

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.bot.UserRemovedEvent;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;
import spi.events.EventListener;

import javax.inject.Inject;
import java.io.IOException;

public class LoginHandler extends MappedLoginService implements EventListener {
    private final Provider<BotUserDAO> userDAOProvider;

    @Inject
    public LoginHandler(final Provider<BotUserDAO> userDAOProvider) {
        this.userDAOProvider = userDAOProvider;
    }

    @Subscribe
    public void onUserRemoved(final UserRemovedEvent event) {
        getUsers().remove(event.getMainNick());
    }

    @Override
    protected UserIdentity loadUser(final String username) {
        BotUser user = userDAOProvider.get().getUser(username);
        if (user != null) {
            String[] roles = user.isAdmin() ? new String[]{SecurityHandler.USER_ROLE, SecurityHandler.ADMIN_ROLE}
                    : new String[]{SecurityHandler.USER_ROLE};
            return putUser(username, new CustomCredential(userDAOProvider, user.getMainNick()), roles);
        }
        return null;
    }

    @Override
    protected void loadUsers() throws IOException {
    }

    private static class CustomCredential extends Credential {
        private final Provider<BotUserDAO> userDAOProvider;
        private final String userName;

        private CustomCredential(final Provider<BotUserDAO> userDAOProvider, final String userName) {
            this.userDAOProvider = userDAOProvider;
            this.userName = userName;
        }

        @Override
        public boolean check(final Object credentials) {
            return userDAOProvider.get().passwordMatches(userName, credentials.toString());
        }
    }
}
