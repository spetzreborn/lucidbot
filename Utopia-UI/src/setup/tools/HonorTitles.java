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

import api.database.transactions.SimpleTransactionTask;
import api.events.DelayedEventPoster;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Provider;
import database.daos.HonorTitleDAO;
import database.models.Bonus;
import database.models.BonusApplicability;
import database.models.BonusType;
import database.models.HonorTitle;
import spi.settings.EntitySetup;

import javax.inject.Inject;

import static api.database.transactions.Transactions.inTransaction;

public class HonorTitles implements EntitySetup {
    public final HonorTitle peasant = new HonorTitle("Peasant", "Mr.|Mrs.", 0, 800, Sets.<Bonus>newHashSet());
    public final HonorTitle knight = new HonorTitle("Knight", "Sir|Lady", 801, 1500, Sets.<Bonus>newHashSet());
    public final HonorTitle lord = new HonorTitle("Lord", "Noble Lady", 1501, 2250, Sets.newHashSet(
            new Bonus("Lord WPA Bonus", BonusType.WPA, BonusApplicability.BOTH, true, 0.03),
            new Bonus("Lord TPA Bonus", BonusType.TPA, BonusApplicability.BOTH, true, 0.03)));
    public final HonorTitle baron = new HonorTitle("Baron", "Baroness", 2251, 3000, Sets.newHashSet(
            new Bonus("Baron WPA Bonus", BonusType.WPA, BonusApplicability.BOTH, true, 0.06),
            new Bonus("Baron TPA Bonus", BonusType.TPA, BonusApplicability.BOTH, true, 0.06)));
    public final HonorTitle viscount = new HonorTitle("Viscount", "Viscountess", 3001, 3750, Sets.newHashSet(
            new Bonus("Viscount WPA Bonus", BonusType.WPA, BonusApplicability.BOTH, true, 0.09),
            new Bonus("Viscount TPA Bonus", BonusType.TPA, BonusApplicability.BOTH, true, 0.09)));
    public final HonorTitle count = new HonorTitle("Count", "Countess", 3751, 4500, Sets.newHashSet(
            new Bonus("Count WPA Bonus", BonusType.WPA, BonusApplicability.BOTH, true, 0.15),
            new Bonus("Count TPA Bonus", BonusType.TPA, BonusApplicability.BOTH, true, 0.15)));
    public final HonorTitle marquis = new HonorTitle("Marquis", "Marchioness", 4501, 5500, Sets.newHashSet(
            new Bonus("Marquis WPA Bonus", BonusType.WPA, BonusApplicability.BOTH, true, 0.21),
            new Bonus("Marquis TPA Bonus", BonusType.TPA, BonusApplicability.BOTH, true, 0.21)));
    public final HonorTitle duke = new HonorTitle("Duke", "Duchess", 5501, 6999, Sets.newHashSet(
            new Bonus("Duke WPA Bonus", BonusType.WPA, BonusApplicability.BOTH, true, 0.27),
            new Bonus("Duke TPA Bonus", BonusType.TPA, BonusApplicability.BOTH, true, 0.27)));
    public final HonorTitle prince = new HonorTitle("Prince", "Princess", 7000, Integer.MAX_VALUE, Sets.newHashSet(
            new Bonus("Prince WPA Bonus", BonusType.WPA, BonusApplicability.BOTH, true, 0.33),
            new Bonus("Prince TPA Bonus", BonusType.TPA, BonusApplicability.BOTH, true, 0.33)));
    public final HonorTitle monarch = new HonorTitle("King", "Queen", Integer.MAX_VALUE, Integer.MAX_VALUE, Sets.<Bonus>newHashSet());

    private final Provider<HonorTitleDAO> honorTitleDAOProvider;

    @Inject
    public HonorTitles(final Provider<HonorTitleDAO> honorTitleDAOProvider) {
        this.honorTitleDAOProvider = honorTitleDAOProvider;
    }

    @Override
    public void loadIntoDatabase() {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                HonorTitleDAO honorTitleDAO = honorTitleDAOProvider.get();
                if (!honorTitleDAO.getAllHonorTitles().isEmpty()) return;
                honorTitleDAO.save(Lists.newArrayList(baron, count, duke, knight, lord, marquis, monarch, peasant, prince, viscount));
            }
        });
    }
}
