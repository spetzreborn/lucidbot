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

import api.database.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Provider;
import database.daos.DragonDAO;
import database.models.Bonus;
import database.models.BonusApplicability;
import database.models.BonusType;
import database.models.Dragon;
import spi.settings.EntitySetup;

import javax.inject.Inject;

import static api.database.Transactions.inTransaction;

public class Dragons implements EntitySetup {
    private final Dragon ruby = new Dragon("Ruby", Sets.<Bonus>newHashSet());
    private final Dragon gold = new Dragon("Gold", Sets.<Bonus>newHashSet());
    private final Dragon emerald = new Dragon("Emerald", Sets.newHashSet(
            new Bonus("Dragon Gains", BonusType.GAIN, BonusApplicability.BOTH, false, 0.10)));
    private final Dragon sapphire = new Dragon("Sapphire", Sets.<Bonus>newHashSet());

    private final Provider<DragonDAO> dragonDAOProvider;

    @Inject
    public Dragons(final Provider<DragonDAO> dragonDAOProvider) {
        this.dragonDAOProvider = dragonDAOProvider;
    }

    @Override
    public void loadIntoDatabase() {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                DragonDAO dragonDAO = dragonDAOProvider.get();
                if (!dragonDAO.getAllDragons().isEmpty()) return;
                dragonDAO.save(Lists.newArrayList(emerald, gold, ruby, sapphire));
            }
        });
    }
}
