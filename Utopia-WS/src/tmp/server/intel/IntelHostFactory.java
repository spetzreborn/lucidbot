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
import com.google.inject.Provider;
import database.daos.IntelDAO;
import database.daos.NewsItemDAO;
import intel.IntelParserManager;
import tools.parsing.NewsParser;

import javax.inject.Inject;
import java.net.Socket;

class IntelHostFactory {
    private final IntelParserManager intelParserManager;
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<NewsItemDAO> newsItemDAOProvider;
    private final Provider<IntelDAO> intelDAOProvider;
    private final NewsParser newsParser;

    @Inject
    IntelHostFactory(final IntelParserManager intelParserManager, final Provider<BotUserDAO> botUserDAOProvider,
                     final Provider<NewsItemDAO> newsItemDAOProvider, final Provider<IntelDAO> intelDAOProvider,
                     final NewsParser newsParser) {
        this.intelParserManager = intelParserManager;
        this.botUserDAOProvider = botUserDAOProvider;
        this.newsItemDAOProvider = newsItemDAOProvider;
        this.intelDAOProvider = intelDAOProvider;
        this.newsParser = newsParser;
    }

    public IntelHost createIntelHost(final Socket socket) {
        return new IntelHost(socket, intelParserManager, botUserDAOProvider.get(), newsItemDAOProvider.get(), newsParser,
                intelDAOProvider.get());
    }
}
