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
import database.daos.PersonalityDAO;
import database.models.*;
import spi.settings.EntitySetup;

import javax.inject.Inject;

import static api.database.Transactions.inTransaction;

public class Personalities implements EntitySetup {
    public final Personality merchant = new Personality("Merchant", "Wealthy", IntelAccuracySpecification.NEVER, false, null, null,
            Sets.<Bonus>newHashSet());
    public final Personality sage = new Personality("Sage", "Wise", IntelAccuracySpecification.NEVER, false, null, null, Sets.<Bonus>newHashSet());
    public final Personality rogue = new Personality("Rogue", "Rogue", IntelAccuracySpecification.NEVER, false, null, null,
            Sets.<Bonus>newHashSet());
    public final Personality mystic = new Personality("Mystic", "Sorcerer|Sorceress", IntelAccuracySpecification.NEVER, false, null, null,
            Sets.<Bonus>newHashSet());
    public final Personality warrior = new Personality("Warrior", "Warrior", IntelAccuracySpecification.NEVER, false, null, null,
            Sets.<Bonus>newHashSet());
    public final Personality tactician = new Personality("Tactician", "Conniving", IntelAccuracySpecification.IN_WAR, false, null, null,
            Sets.<Bonus>newHashSet());
    public final Personality cleric = new Personality("Cleric", "Blessed", IntelAccuracySpecification.NEVER, false, null, null,
            Sets.<Bonus>newHashSet());
    public final Personality warHero = new Personality("War Hero", "Hero", IntelAccuracySpecification.NEVER, true, null, null,
            Sets.<Bonus>newHashSet(
                    new Bonus("War Hero Honor Bonus", BonusType.HONOR, BonusApplicability.BOTH,
                            true, 1.5)));

    private final Provider<PersonalityDAO> personalityDAOProvider;

    @Inject
    public Personalities(final Provider<PersonalityDAO> personalityDAOProvider) {
        this.personalityDAOProvider = personalityDAOProvider;
    }

    @Override
    public void loadIntoDatabase() {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                PersonalityDAO personalityDAO = personalityDAOProvider.get();
                if (!personalityDAO.getAllPersonalities().isEmpty()) return;
                personalityDAO.save(Lists.newArrayList(cleric, merchant, mystic, rogue, sage, tactician, warHero, warrior));
            }
        });
    }
}
