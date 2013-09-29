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

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.database.transactions.CallableTransactionTask;
import api.events.DelayedEventPoster;
import database.daos.IntelDAO;
import database.daos.NewsItemDAO;
import database.models.NewsItem;
import intel.Intel;
import intel.IntelParser;
import intel.IntelParserManager;
import lombok.extern.log4j.Log4j;
import tools.parsing.NewsParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import static api.database.transactions.Transactions.inTransaction;

/**
 * A class that communicates with the forum agent site to save intel
 *
 * @author Fredrik
 */
@Log4j
public class IntelHost implements Runnable {
    private final Socket socket;
    private final IntelParserManager intelParserManager;
    private final BotUserDAO botUserDAO;
    private final NewsItemDAO newsItemDAO;
    private final NewsParser newsParser;
    private final IntelDAO intelDAO;

    public IntelHost(final Socket socket, final IntelParserManager intelParserManager, final BotUserDAO botUserDAO,
                     final NewsItemDAO newsItemDAO, final NewsParser newsParser, final IntelDAO intelDAO) {
        this.socket = socket;
        this.intelParserManager = intelParserManager;
        this.botUserDAO = botUserDAO;
        this.newsItemDAO = newsItemDAO;
        this.newsParser = newsParser;
        this.intelDAO = intelDAO;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String read = in.readLine();
            read = read == null ? null : read.trim();

            if ("Sending intel".equals(read)) {
                final String user = in.readLine();
                if (user != null) {
                    final String pass = in.readLine();
                    if (pass != null) {
                        if (botUserDAO.passwordMatches(user, pass)) {
                            inTransaction(new CallableTransactionTask<Object>() {
                                @Override
                                public Object call(final DelayedEventPoster delayedEventPoster) throws Exception {
                                    BotUser botUser = botUserDAO.getUser(user);
                                    out.write("Authentication successful\n");
                                    out.flush();
                                    StringBuilder intel = new StringBuilder(2000);
                                    String line;
                                    while ((line = in.readLine()) != null) {
                                        if ("Done".equals(line)) {
                                            break;
                                        }
                                        intel.append(line);
                                    }
                                    int counter = 0;
                                    for (Map.Entry<String, IntelParser<?>> entry : intelParserManager.getParsers(intel.toString())
                                            .entrySet()) {
                                        try {
                                            Intel parsed = entry.getValue().parse(user, entry.getKey());
                                            boolean success =
                                                    parsed == null || intelDAO.saveIntel(parsed, botUser.getId(), delayedEventPoster);
                                            out.write((success ? "+" : "-") + counter + '\n');
                                        } catch (Exception e) {
                                            out.write("-" + counter + '\n');
                                            log.fatal("Parsing intel failed", e);
                                        }
                                        out.flush();
                                        ++counter;
                                    }
                                    out.write("Finished\n");
                                    out.flush();
                                    return null;
                                }
                            }, true);
                        } else {
                            out.write("Authentication failed\n");
                            out.flush();
                            out.write("Finished\n");
                            out.flush();
                        }
                    }
                }
            } else if ("Sending news".equals(read)) {
                final String user = in.readLine();
                if (user != null) {
                    final String pass = in.readLine();
                    if (pass != null) {
                        if (botUserDAO.passwordMatches(user, pass)) {
                            StringBuilder raw = new StringBuilder(2000);
                            String line;
                            while ((line = in.readLine()) != null) {
                                line = line.trim();
                                if ("Done".equals(line)) {
                                    break;
                                }
                                raw.append(line).append('\n');
                            }
                            List<NewsItem> news = newsParser.parseNews(raw.toString());
                            boolean success = !news.isEmpty() && !newsItemDAO.save(news).isEmpty();
                            out.write(success ? "Success\n" : "Failed\n");
                            out.flush();
                        } else {
                            out.write("Authentication failed\n");
                            out.flush();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("", e);
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (final IOException ignored) {
            }
        }
    }
}
