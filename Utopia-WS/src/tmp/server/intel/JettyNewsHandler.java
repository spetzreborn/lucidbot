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

import api.database.DBException;
import api.database.daos.BotUserDAO;
import com.google.inject.Provider;
import database.daos.NewsItemDAO;
import database.models.NewsItem;
import lombok.extern.log4j.Log4j;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import tools.parsing.NewsParser;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Log4j
public class JettyNewsHandler extends AbstractHandler {
    private static final Path NEWS_FILE = Paths.get("news.html");

    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<NewsItemDAO> newsItemDAOProvider;
    private final NewsParser newsParser;

    @Inject
    JettyNewsHandler(final Provider<BotUserDAO> userDAOProvider, final Provider<NewsItemDAO> newsItemDAOProvider,
                     final NewsParser newsParser) {
        this.userDAOProvider = userDAOProvider;
        this.newsItemDAOProvider = newsItemDAOProvider;
        this.newsParser = newsParser;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String news = request.getParameter("news");
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            List<String> lines = Files.readAllLines(NEWS_FILE, Charset.forName("UTF-8"));
            for (String line : lines) {
                response.getWriter().println(line);
            }
        } else if (news != null && !news.isEmpty()) {
            try {
                if (userDAOProvider.get().passwordMatches(username.trim(), password.trim())) {
                    List<NewsItem> newsItems = newsParser.parseNews(news);
                    if (newsItems.isEmpty())
                        response.getWriter().println("Failed to parse any news from the pasted text");
                    else {
                        newsItemDAOProvider.get().save(newsItems);
                        response.getWriter().println("Saved news successfully");
                    }
                } else {
                    response.getWriter().println("Your username/password is not valid");
                }
            } catch (DBException e) {
                log.error("", e);
                response.getWriter().println("Parsed the news but could not save them in the database");
            }
        } else {
            response.getWriter().println("Username, password and actual news are all required");
        }
        response.getWriter().flush();
    }
}
