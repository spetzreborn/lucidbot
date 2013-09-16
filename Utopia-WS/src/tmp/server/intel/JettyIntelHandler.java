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

package tmp.server.intel;

import api.database.CallableTransactionTask;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import com.google.inject.Provider;
import database.daos.IntelDAO;
import intel.Intel;
import intel.IntelParser;
import intel.IntelParserManager;
import lombok.extern.log4j.Log4j;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static api.database.Transactions.inTransaction;
import static api.tools.text.StringUtil.isNotNullOrEmpty;
import static com.google.common.base.Objects.firstNonNull;

@Log4j
public class JettyIntelHandler extends AbstractHandler {
    private static final String API_ENGINE_VERSION = "1";
    private static final String MINIMUM_AGENT_VERSION = "20";
    private static final String BULK_MODE = "true";

    private static final String FORUM_AGENT_HEADER = "[FORUM AGENT API]";
    private static final String INSTRUCTION_PREFIX = "FORUMAGENT:";

    private final IntelParserManager intelParserManager;
    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<IntelDAO> intelDAOProvider;

    @Inject
    JettyIntelHandler(final IntelParserManager intelParserManager, final Provider<IntelDAO> intelDAOProvider,
                      final Provider<BotUserDAO> userDAOProvider) {
        this.intelParserManager = intelParserManager;
        this.intelDAOProvider = intelDAOProvider;
        this.userDAOProvider = userDAOProvider;
    }

    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
                       final HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if (username == null) {
            handShake(response);
        } else {
            try {
                final BotUser botUser = loginUser(username, password, userDAOProvider.get());
                if (botUser != null) {
                    inTransaction(new CallableTransactionTask<Object>() {
                        @Override
                        public Object call(final DelayedEventPoster delayedEventPoster) throws Exception {
                            response.getWriter().println("+LOGIN");
                            response.getWriter().flush();
                            String data = firstNonNull(request.getParameter("bulk_data"), request.getParameter("data"));
                            data = data.replace('\r', ' ').replace('\n', ' ');
                            Map<String, Intel> map = parseIntel(data, botUser);
                            if (map.isEmpty()) {
                                response.getWriter().println("-0");
                            } else {
                                for (Map.Entry<String, Intel> entry : map.entrySet()) {
                                    if (entry.getValue() == null ||
                                            intelDAOProvider.get().saveIntel(entry.getValue(), botUser.getId(), delayedEventPoster))
                                        response.getWriter().println(entry.getKey());
                                    else response.getWriter().println(entry.getKey().replace('+', '-'));
                                }
                            }
                            response.getWriter().flush();
                            return null;
                        }
                    }, true);
                } else {
                    response.getWriter().println("-LOGIN");
                    response.getWriter().flush();
                }
                response.flushBuffer();
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    private static void handShake(final HttpServletResponse response) throws IOException {
        response.getWriter().println(
                FORUM_AGENT_HEADER + INSTRUCTION_PREFIX + "api_engine_version=\"" + API_ENGINE_VERSION + '\"' + INSTRUCTION_PREFIX +
                        "minimum_forum_agent_version=\"" + MINIMUM_AGENT_VERSION + '\"' + INSTRUCTION_PREFIX + "bulk_mode=\"" + BULK_MODE + '\"' +
                        INSTRUCTION_PREFIX + "debug_mode=\"false\"");
        response.getWriter().flush();
    }

    private static BotUser loginUser(final String username, final String password, final BotUserDAO userDAO) {
        return isNotNullOrEmpty(username) && isNotNullOrEmpty(password) && userDAO.passwordMatches(username, password) ? userDAO
                .getUser(username) : null;
    }

    private LinkedHashMap<String, Intel> parseIntel(final String data, final BotUser user) {
        LinkedHashMap<String, Intel> out = new LinkedHashMap<>();
        int counter = 0;
        for (Map.Entry<String, IntelParser<?>> entry : intelParserManager.getParsers(data).entrySet()) {
            try {
                Intel parsed = entry.getValue().parse(user.getMainNick(), entry.getKey());
                out.put("+" + counter++, parsed);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                out.put("-" + counter++ + ' ' + e.getMessage(), null);
            }
        }
        return out;
    }
}