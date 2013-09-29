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
import database.daos.RaceDAO;
import database.models.*;
import spi.settings.EntitySetup;

import javax.inject.Inject;

import static api.database.transactions.Transactions.inTransaction;

public class Races implements EntitySetup {
    public final Race avian = new Race("Avian", "AV", 1, 1.5, "Griffins", 4, "Harpies", 4, "Drakes", 6, 2, 5, 100,
            IntelAccuracySpecification.NEVER, false, null, null, Sets.<Bonus>newHashSet(), Lists.<SpellType>newArrayList());
    public final Race dwarf = new Race("Dwarf", "DW", 1, 1.5, "Warriors", 4, "Axemen", 4, "Berserkers", 6, 3, 5.25, 100,
            IntelAccuracySpecification.NEVER, false, null, null, Sets.newHashSet(new Bonus("Dwarf Gains", BonusType.GAIN, BonusApplicability.OFFENSIVELY, false, 0.1)),
            Lists.<SpellType>newArrayList());
    public final Race elf = new Race("Elf", "EL", 1, 1.5, "Rangers", 4, "Archers", 4, "Elf Lords", 5, 3, 5, 100,
            IntelAccuracySpecification.NEVER, false, null, null,
            Sets.newHashSet(new Bonus("Elf WPA", BonusType.WPA, BonusApplicability.BOTH, true, 0.3)),
            Lists.<SpellType>newArrayList());
    public final Race faery = new Race("Faery", "FA", 1, 1.5, "Magicians", 4, "Druids", 4, "Beastmasters", 3, 5, 5.25, 0,
            IntelAccuracySpecification.NEVER, false, null, null,
            Sets.<Bonus>newHashSet(), Lists.<SpellType>newArrayList());
    public final Race halfling = new Race("Halfling", "HA", 2, 3, "Strongarms", 4, "Slingers", 4, "Brutes", 4, 4, 4, 60,
            IntelAccuracySpecification.NEVER, false, null, null,
            Sets.newHashSet(new Bonus("Halfling TPA", BonusType.TPA, BonusApplicability.BOTH, true, 0.5)),
            Lists.<SpellType>newArrayList());
    public final Race human = new Race("Human", "HU", 1, 1.5, "Swordsmen", 4, "Archers", 4, "Knights", 6, 4, 5.5, 20,
            IntelAccuracySpecification.NEVER, false, null, null, Sets.<Bonus>newHashSet(),
            Lists.<SpellType>newArrayList());
    public final Race orc = new Race("Orc", "OR", 1, 1.5, "Goblins", 4, "Trolls", 4, "Ogres", 7, 1, 5.75, 100,
            IntelAccuracySpecification.NEVER, false, null, null,
            Sets.newHashSet(new Bonus("Orc Gains", BonusType.GAIN, BonusApplicability.OFFENSIVELY, true, 0.3)),
            Lists.<SpellType>newArrayList());
    public final Race undead = new Race("Undead", "UD", 1, 1.5, "Skeletons", 4, "Zombies", 4, "Ghouls", 7, 2, 6.0, 100,
            IntelAccuracySpecification.NEVER, false, null, null, Sets.<Bonus>newHashSet(),
            Lists.<SpellType>newArrayList());

    private final Provider<RaceDAO> raceDAOProvider;

    @Inject
    public Races(final Provider<RaceDAO> raceDAOProvider) {
        this.raceDAOProvider = raceDAOProvider;
    }

    @Override
    public void loadIntoDatabase() {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                RaceDAO raceDAO = raceDAOProvider.get();
                if (!raceDAO.getAllRaces().isEmpty()) return;
                raceDAO.save(Lists.newArrayList(avian, dwarf, elf, faery, halfling, human, orc, undead));
            }
        });
    }
}
