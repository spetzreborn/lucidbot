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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static api.database.Transactions.inTransaction;
import static api.tools.text.StringUtil.isNotNullOrEmpty;
import static api.tools.text.StringUtil.isNullOrEmpty;

@Log4j
public class JettyPastedIntelHandler extends AbstractHandler {
    private static final Path INTEL_FILE = Paths.get("intel.html");

    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<IntelDAO> intelDAOProvider;
    private final IntelParserManager intelParserManager;

    @Inject
    JettyPastedIntelHandler(final Provider<BotUserDAO> userDAOProvider, final Provider<IntelDAO> intelDAOProvider,
                            final IntelParserManager intelParserManager) {
        this.userDAOProvider = userDAOProvider;
        this.intelDAOProvider = intelDAOProvider;
        this.intelParserManager = intelParserManager;
    }

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       final HttpServletResponse response) throws
            IOException,
            ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        final String username = request.getParameter("username");
        String password = request.getParameter("password");
        final String intel = request.getParameter("intel");
        if (isNullOrEmpty(username)) {
            List<String> lines = Files.readAllLines(INTEL_FILE, Charset.forName("UTF-8"));
            for (String line : lines) {
                response.getWriter().println(line);
            }
        } else if (isNotNullOrEmpty(intel) && isNotNullOrEmpty(password)) {
            try {
                if (userDAOProvider.get().passwordMatches(username.trim(), password.trim())) {
                    inTransaction(new CallableTransactionTask<Object>() {
                        @Override
                        public Object call(final DelayedEventPoster delayedEventPoster) throws Exception {
                            BotUser user = userDAOProvider.get().getUser(username.trim());
                            String cleanedIntel = intel.replace('\r', ' ').replace('\n', ' ').trim();
                            Map<String, IntelParser<?>> parsers = intelParserManager.getParsers(cleanedIntel);
                            if (parsers.isEmpty())
                                response.getWriter().println("Failed to parse any intel from the pasted text");
                            else {
                                List<Intel> results = new ArrayList<>();
                                List<String> ignored = new ArrayList<>();
                                List<String> failed = new ArrayList<>();
                                for (Map.Entry<String, IntelParser<?>> entry : parsers.entrySet()) {
                                    IntelParser<?> parser = entry.getValue();
                                    String intel = entry.getKey();
                                    try {
                                        Intel parsedIntel = parser.parse(username, intel);
                                        if (parsedIntel != null) results.add(parsedIntel);
                                        else ignored.add(parser.getIntelTypeHandled());
                                    } catch (Exception e) {
                                        failed.add(parser.getIntelTypeHandled() + " because: " + e.getClass().getSimpleName() + " >> " + e.getMessage());
                                        JettyPastedIntelHandler.log.fatal("Parsing intel failed", e);
                                    }
                                }
                                intelDAOProvider.get().saveIntel(results, user, delayedEventPoster);
                                response.getWriter().println("Saved:<br>");
                                for (Intel result : results) {
                                    response.getWriter().println(result.getDescription() + "<br>");
                                }
                                if (!ignored.isEmpty()) {
                                    response.getWriter().println("Parsed but not saved (likely because newer intel exists):<br>");
                                    for (String info : ignored) {
                                        response.getWriter().println(info + "<br>");
                                    }
                                }
                                if (!failed.isEmpty()) {
                                    response.getWriter().println("Failed to be parsed:<br>");
                                    for (String info : failed) {
                                        response.getWriter().println(info + "<br>");
                                    }
                                }
                            }
                            return null;
                        }
                    }, true);
                } else {
                    response.getWriter().println("Your username/password is not valid");
                }
            } catch (Exception e) {
                JettyPastedIntelHandler.log.error("", e);
                response.getWriter()
                        .println("Parsed the intel but could not save it in the database. <br>Error message: " + e.getMessage());
            }
        } else {
            response.getWriter().println("Username, password and actual intel are all required");
        }
        response.getWriter().flush();
    }
}
