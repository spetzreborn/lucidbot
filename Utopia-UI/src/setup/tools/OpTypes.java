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
import api.irc.ValidationType;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import database.daos.OpDAO;
import database.models.Bonus;
import database.models.OpType;
import database.models.SpellOpCharacter;
import spi.settings.EntitySetup;

import javax.inject.Inject;
import java.util.Collections;

import static api.database.transactions.Transactions.inTransaction;

public class OpTypes implements EntitySetup {
    private static final String RESULT = "(?<result>" + ValidationType.INT.getPattern() + ')';

    public final OpType sabotageWizards = new OpType("Sabotage Wizards", "sw", "Reduces the target provinces mana by a small percentage.",
            null, null, SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final OpType bribeThieves = new OpType("Bribe Thieves", "bt", "Reduces thief effectiveness by 10%", null, null,
            SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final OpType bribeGenerals = new OpType("Bribe Generals", "bg",
            "Effectively a 1 in 5 chance per attack to increase troop losses by 15% on all combat.",
            null, null, SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final OpType propaganda = new OpType("Propaganda", null,
            "Attempts to convince enemy peasants, military, or wizards to revolt and join your province.",
            null, null, SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());

    public final OpType nightStrike = new OpType("Night Strike", "ns",
            "Assassinates a portion of your enemy's military, both at home and away.",
            "Our thieves assassinated " + RESULT +
                    " enemy troops", null, SpellOpCharacter.INSTANT_SPELLOP_WITHOUT_PROVINCE,
            Collections.<Bonus>emptySet());
    public final OpType riots = new OpType("Riots", null, "Decreases targets income by 20%.",
            "Our thieves have caused rioting. It is expected to last " +
                    RESULT + " days", null, SpellOpCharacter.FADING_SPELLOP_WITHOUT_PROVINCE,
            Collections.<Bonus>emptySet());
    public final OpType robGranaries = new OpType("Rob Granaries", "rg",
            "Steals up to a max of 31.5% (46% in war) enemy food at the rate of 95 (135 in war) bushels per thief.",
            "Our thieves have returned with " + RESULT +
                    " bushels", null, SpellOpCharacter.INSTANT_SPELLOP_WITHOUT_PROVINCE,
            Collections.<Bonus>emptySet());
    public final OpType robTowers = new OpType("Rob Towers", "rt",
            "Steals up to a max of 24.5% (35% in war) enemy runes at the rate of 18.2 (26 in war) runes per thief.",
            "Our thieves were able to steal " + RESULT +
                    " runes", null, SpellOpCharacter.INSTANT_SPELLOP_WITHOUT_PROVINCE,
            Collections.<Bonus>emptySet());
    public final OpType robVaults = new OpType("Rob Vaults", "rv",
            "Steals up to a max of 5.2% (14% in war) enemy gc at the rate of 40 (106 in war) gc per thief.",
            "Our thieves have returned with " + RESULT +
                    " gold coins", null, SpellOpCharacter.INSTANT_SPELLOP_WITHOUT_PROVINCE,
            Collections.<Bonus>emptySet());
    public final OpType kidnapping = new OpType("Kidnapping", null,
            "Steals up to a max of 1.75% (2.5% in war) enemy peasants at the rate of 0.1 (0.27 in war) peasants per thief.",
            "Our thieves kidnapped many people, but only were able to return with " +
                    RESULT +
                    " of them", null, SpellOpCharacter.INSTANT_SPELLOP_WITHOUT_PROVINCE,
            Collections.<Bonus>emptySet());
    public final OpType arson = new OpType("Arson", null, "Destroys a small amount of enemy buildings.",
            "Our thieves burned down " + RESULT +
                    " acres of buildings", null, SpellOpCharacter.INSTANT_SPELLOP_WITHOUT_PROVINCE,
            Collections.<Bonus>emptySet());
    public final OpType stealHorses = new OpType("Steal Horses", "sh",
            "Steals up to 20% of the targets horses at a rate of 0.35 horses per thief.",
            "Our thieves were able to release " + RESULT +
                    " horses", null, SpellOpCharacter.INSTANT_SPELLOP_WITHOUT_PROVINCE,
            Collections.<Bonus>emptySet());
    public final OpType freePrisoners = new OpType("Free Prisoners", "fp",
            "Releases up to a max of 12% (17% in war) of the targets prisoners at a rate of 0.06 (0.07 in war) prisoners per thief.",
            "Our thieves freed " + RESULT +
                    " prisoners from enemy dungeons", null,
            SpellOpCharacter.INSTANT_SPELLOP_WITHOUT_PROVINCE, Collections.<Bonus>emptySet());
    public final OpType assassinateWizards = new OpType("Assassinate Wizards", "aw",
            "Kills up to a max of 1.4% target wizards at a rate of 0.007 wizards per thief.",
            "Our thieves assassinated " + RESULT +
                    " wizards of the enemy's guilds!", null,
            SpellOpCharacter.INSTANT_SPELLOP_WITHOUT_PROVINCE, Collections.<Bonus>emptySet());

    private final Provider<OpDAO> opDAOProvider;

    @Inject
    public OpTypes(final Provider<OpDAO> opDAOProvider) {
        this.opDAOProvider = opDAOProvider;
    }

    @Override
    public void loadIntoDatabase() {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                OpDAO opDAO = opDAOProvider.get();
                if (!opDAO.getAllOpTypes().isEmpty()) return;
                opDAO.save(
                        Lists.newArrayList(arson, assassinateWizards, bribeGenerals, bribeThieves, freePrisoners, kidnapping, nightStrike,
                                propaganda, riots, robGranaries, robTowers, robVaults, sabotageWizards, stealHorses));
            }
        });
    }
}
