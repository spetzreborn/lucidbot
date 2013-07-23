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
import database.daos.ScienceTypeDAO;
import database.models.Bonus;
import database.models.BonusApplicability;
import database.models.BonusType;
import database.models.ScienceType;
import spi.settings.EntitySetup;

import javax.inject.Inject;

import static api.database.Transactions.inTransaction;

public class ScienceTypes implements EntitySetup {
    public final ScienceType alchemy = new ScienceType("Alchemy", "Income", "", 1.4, Sets.<Bonus>newHashSet());
    public final ScienceType tools = new ScienceType("Tools", "Building Effectiveness", "", 1.0, Sets.<Bonus>newHashSet());
    public final ScienceType housing = new ScienceType("Housing", "Population Limits", "", 0.65, Sets.<Bonus>newHashSet());
    public final ScienceType food = new ScienceType("Food", "Food Production", "", 8.0, Sets.<Bonus>newHashSet());
    public final ScienceType military = new ScienceType("Military", "Gains in Combat", "", 1.4, Sets.newHashSet(
            new Bonus("Gains Sci", BonusType.GAIN, BonusApplicability.OFFENSIVELY, true, 0.0)));
    public final ScienceType crime = new ScienceType("Crime", "Thievery Effectiveness", "", 6.0, Sets.newHashSet(
            new Bonus("Thievery Sci", BonusType.TPA, BonusApplicability.BOTH, true, 0.0)));
    public final ScienceType channeling = new ScienceType("Channeling", "Magic Effectiveness & Rune Production", "", 6.0, Sets.newHashSet(
            new Bonus("Wizardry Sci", BonusType.WPA, BonusApplicability.BOTH, true, 0.0)));

    private final Provider<ScienceTypeDAO> scienceTypeDAOProvider;

    @Inject
    public ScienceTypes(final Provider<ScienceTypeDAO> scienceTypeDAOProvider) {
        this.scienceTypeDAOProvider = scienceTypeDAOProvider;
    }

    @Override
    public void loadIntoDatabase() {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                ScienceTypeDAO scienceTypeDAO = scienceTypeDAOProvider.get();
                if (!scienceTypeDAO.getAllScienceTypes().isEmpty()) return;
                scienceTypeDAO.save(Lists.newArrayList(alchemy, tools, housing, food, military, crime, channeling));
            }
        });
    }
}
