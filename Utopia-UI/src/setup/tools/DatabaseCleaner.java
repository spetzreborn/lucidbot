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

package setup.tools;

import api.database.JDBCWorkExecutor;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.database.transactions.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import com.google.inject.Provider;
import database.daos.IntelDAO;
import database.daos.KingdomDAO;
import database.daos.ProvinceDAO;
import database.models.Kingdom;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.Date;

import static api.database.transactions.Transactions.inTransaction;

@Log4j
public class DatabaseCleaner {
    private final Provider<IntelDAO> intelDAOProvider;
    private final Provider<JDBCWorkExecutor> jdbcWorkExecutorProvider;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<KingdomDAO> kingdomDAOProvider;
    private final Provider<BotUserDAO> userDAOProvider;

    @Inject
    public DatabaseCleaner(final Provider<IntelDAO> intelDAOProvider, final Provider<JDBCWorkExecutor> jdbcWorkExecutorProvider,
                           final Provider<ProvinceDAO> provinceDAOProvider, final Provider<KingdomDAO> kingdomDAOProvider,
                           final Provider<BotUserDAO> userDAOProvider) {
        this.intelDAOProvider = intelDAOProvider;
        this.provinceDAOProvider = provinceDAOProvider;
        this.kingdomDAOProvider = kingdomDAOProvider;
        this.userDAOProvider = userDAOProvider;
        this.jdbcWorkExecutorProvider = jdbcWorkExecutorProvider;
    }

    public void clean() {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                try {
                    intelDAOProvider.get().clearIntel(new Date(), jdbcWorkExecutorProvider.get());
                } catch (SQLException e) {
                    DatabaseCleaner.log.error("", e);
                }

                KingdomDAO kingdomDAO = kingdomDAOProvider.get();
                ProvinceDAO provinceDAO = provinceDAOProvider.get();
                for (Kingdom kingdom : kingdomDAO.getAllKingdoms()) {
                    provinceDAO.delete(kingdom.getProvinces());
                    kingdomDAO.delete(kingdom);
                }

                for (BotUser user : userDAOProvider.get().getAllUsers()) {
                    user.getStats().clear();
                }
            }
        });
    }
}
