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
import com.google.inject.Provider;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;

public class WebContextFactory {
    private final Provider<BotUserDAO> userDAOProvider;

    @Inject
    public WebContextFactory(final Provider<BotUserDAO> userDAOProvider) {
        this.userDAOProvider = userDAOProvider;
    }

    public WebContext createWebContext(final SecurityContext securityContext) {
        return new WebContextImpl(userDAOProvider, securityContext);
    }

    private static class WebContextImpl implements WebContext {
        private final Provider<BotUserDAO> userDAOProvider;
        private final SecurityContext securityContext;

        private BotUser botUser;

        private WebContextImpl(final Provider<BotUserDAO> userDAOProvider, final SecurityContext securityContext) {
            this.userDAOProvider = userDAOProvider;
            this.securityContext = securityContext;
        }

        @Override
        public String getName() {
            return securityContext.getUserPrincipal().getName();
        }

        @Override
        public boolean isInRole(final String role) {
            return securityContext.isUserInRole(role);
        }

        @Override
        public boolean isUser(final long userId) {
            BotUser user = getBotUser();
            return user != null && user.getId().equals(userId);
        }

        @Override
        public BotUser getBotUser() {
            return fetchBotUser();
        }

        private BotUser fetchBotUser() {
            if (botUser == null) {
                botUser = userDAOProvider.get().getUser(getName());
            }
            return botUser;
        }
    }
}
