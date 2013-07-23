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
import api.irc.ValidationType;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Provider;
import database.daos.SpellDAO;
import database.models.*;
import spi.settings.EntitySetup;
import tools.parsing.UtopiaValidationType;

import javax.inject.Inject;
import java.util.Collections;

import static api.database.Transactions.inTransaction;

public class SpellTypes implements EntitySetup {
    private static final String RESULT = "(?<result>" + ValidationType.INT.getPattern() + ')';
    private static final String PROVINCE = "(?<target>[^(]+" + UtopiaValidationType.KDLOC.getPatternString() + ')';

    public final SpellType minorProtection = new SpellType("Minor Protection", "mp", "Increases defensive military efficiency by 5%.",
            "Our realm is now under a sphere of protection for " + RESULT + " days", null,
            SpellOpCharacter.SELF_SPELLOP, Collections.<Bonus>emptySet());
    public final SpellType greaterProtection = new SpellType("Greater Protection", "gp", "Increases defensive military efficiency by 5%.",
            null, null, SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType magicShield = new SpellType("Magic Shield", "mshield", "Increases your defensive magic efficiency by 20%.",
            "The magical auras within our province will protect us from the black magic of our enemies for " +
                    RESULT + " days!", null, SpellOpCharacter.SELF_SPELLOP,
            Collections.<Bonus>emptySet());
    public final SpellType mysticAura = new SpellType("Mystic Aura", "ma",
            "Repels the next offensive spell cast upon you (except own spells).", null, null,
            SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType fertileLands = new SpellType("Fertile Lands", "fl", "Increases food production by 25%.",
            "We have made our lands extraordinarily fertile for " + RESULT + " days!", null,
            SpellOpCharacter.SELF_SPELLOP, Collections.<Bonus>emptySet());
    public final SpellType naturesBlessing = new SpellType("Nature's Blessing", "nb",
            "Protects your land against Storms and Drought. Has a 20% chance of curing Plague (per cast).",
            "Our lands have been blessed by nature for " + RESULT +
                    " days, and will be protected from drought and storms", null,
            SpellOpCharacter.SELF_SPELLOP, Collections.<Bonus>emptySet());
    public final SpellType loveAndPeace = new SpellType("Love & Peace", "l&p", "Increases base birth rate from 2.05% to 2.80%.",
            "Our peasantry is influenced by a magical calm. We expect birth rates to be higher for " +
                    RESULT + " days!", null, SpellOpCharacter.SELF_SPELLOP,
            Collections.<Bonus>emptySet());
    public final SpellType treeOfGold = new SpellType("Tree of Gold", "tog",
            "Magically creates a small amount of gold (from 40 to 80% of your daily income).",
            RESULT + " gold coins have fallen from the trees!", null,
            SpellOpCharacter.INSTANT_SELF_SPELLOP, Collections.<Bonus>emptySet());
    public final SpellType quickFeet = new SpellType("Quick Feet", "qf", "Decreases your attack times by 10% for your next attack.", null,
            null, SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType buildersBoon = new SpellType("Builder's Boon", "bb",
            "Decreases your construction times by 10% for building set to build while active.",
            "Our builders have been blessed with unnatural speed for " + RESULT + " days!",
            null, SpellOpCharacter.SELF_SPELLOP, Collections.<Bonus>emptySet());
    public final SpellType inspireArmy = new SpellType("Inspire Army", "ia",
            "Decreases your military wages by 15%. Decreases your military training time by 20%.",
            "Our army has been inspired to train harder. We expect maintenance costs to be reduced for " +
                    RESULT + " days!", null, SpellOpCharacter.SELF_SPELLOP,
            Collections.<Bonus>emptySet());
    public final SpellType anonymity = new SpellType("Anonymity", null,
            "Hides your province name during your next attack at the cost of no honor gains, " +
                    "causing the attacked province to be unable to ambush that attack. " +
                    "Increases your attach times by 15% for your next attack.", null, null,
            SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType invisibility = new SpellType("Invisibility", null, "Increases your offensive thievery efficiency by 10%.",
            "Our thieves have been made partially invisible for " + RESULT + " days!", null,
            SpellOpCharacter.SELF_SPELLOP, Sets.newHashSet(
            new Bonus("Invisibility", BonusType.TPA, BonusApplicability.OFFENSIVELY, true, 0.1)));
    public final SpellType clearSight = new SpellType("Clear Sight", "cs",
            "Automatically catches 25% of the thieves' operations conducted against your province.",
            "Our police have been blessed with Clear Sight for " + RESULT + " days!", null,
            SpellOpCharacter.SELF_SPELLOP, Collections.<Bonus>emptySet());
    public final SpellType warSpoils = new SpellType("War Spoils", "ws",
            "Makes the land gained from Traditional March immediately available.", null, null,
            SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType fanaticism = new SpellType("Fanaticism", null,
            "Increases your offensive military effienciency by 5%. Decreases your defensive military effienciency by 3%.",
            "Our army will fight with fanatical fervor for " + RESULT + " days!", null,
            SpellOpCharacter.SELF_SPELLOP, Collections.<Bonus>emptySet());
    public final SpellType magesFury = new SpellType("Mage's Fury", "mf",
            "This spell increases the province's WPA by 20% for offensive purposes while decreasing it by 20% for defensive",
            "The fire of Mage's Fury burns in our wizards' eyes for " + RESULT + " days!", null,
            SpellOpCharacter.SELF_SPELLOP, Sets.newHashSet(
            new Bonus("Mage's Fury Off", BonusType.WPA, BonusApplicability.OFFENSIVELY, true, 0.2),
            new Bonus("Mage's Fury Def", BonusType.WPA, BonusApplicability.DEFENSIVELY, false, 0.2)));
    public final SpellType fountainOfKnowledge = new SpellType("Fountain of Knowledge", "fok",
            "Increases your science research by 10% while the spell is active.",
            "Our students are blessed with excellent concentration for " + RESULT +
                    " days!", null, SpellOpCharacter.SELF_SPELLOP,
            Collections.<Bonus>emptySet());
    public final SpellType townWatch = new SpellType("Town Watch", "tw",
            "Every 4 of your peasants will defend your land with 1 point of defense.",
            "Our peasants will help defend our lands for " + RESULT + " days!", null,
            SpellOpCharacter.SELF_SPELLOP, Sets.newHashSet(
            new Bonus("Town Watch", BonusType.PEASANT_DEFENSE, BonusApplicability.DEFENSIVELY, true, 0.25)));
    public final SpellType aggression = new SpellType("Aggression", null,
            "Turns all your soldiers into 2/0 (Halfling: 3/1) troops before modifiers.",
            "Our soldiers will fight with unique aggression for " + RESULT + " days!", null,
            SpellOpCharacter.SELF_SPELLOP, Collections.<Bonus>emptySet());
    public final SpellType animateDead = new SpellType("Animate Dead", "ad",
            "Raises 50% of your dead troops into basic soldiers during your next defensive battle.",
            null, null, SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType reflectMagic = new SpellType("Reflect Magic", "rm",
            "Has a 25% chance of reflecting offensive spells cast upon your province.",
            "Some of the spells cast upon our lands will be reflected back upon their creators for " +
                    RESULT + " days!", null, SpellOpCharacter.SELF_SPELLOP,
            Collections.<Bonus>emptySet());
    public final SpellType shadowlight = new SpellType("Shadowlight", null,
            "Reveals the name of the next province performing a successful thievery operation upon your province.",
            null, null, SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType bloodlust = new SpellType("Bloodlust", null,
            "A province under Bloodlust will inflict 15% more kills and suffer 5% higher losses on their next attack.",
            null, null, SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType patriotism = new SpellType("Patriotism", null,
            "Increases military draft speed by 30%. Also provides some protection against Propaganda.",
            "Our people are excited about the military and will signup more quickly for " +
                    RESULT + " days!", null, SpellOpCharacter.SELF_SPELLOP,
            Collections.<Bonus>emptySet());
    public final SpellType paradise = new SpellType("Paradise", null, "Creates a small amount of land per cast.",
            "Our mages created " + RESULT + " acres more land for us to use", null,
            SpellOpCharacter.INSTANT_SELF_SPELLOP, Collections.<Bonus>emptySet());

    public final SpellType storms = new SpellType("Storms", null,
            "Kills 1.5% of total population per day. Negates any Drougts already cast.",
            "Storms will ravage " + PROVINCE + " for " + RESULT + " days!", null,
            SpellOpCharacter.FADING_SPELLOP_WITH_PROVINCE, Collections.<Bonus>emptySet());
    public final SpellType droughts = new SpellType("Droughts", null,
            "Decreases food production by 25%, military draft rate by 15% and horses production by 50%. Negates any Storms already cast.",
            "A drought will reign over the lands of " + PROVINCE + " for " + RESULT + " days!",
            null, SpellOpCharacter.FADING_SPELLOP_WITH_PROVINCE, Collections.<Bonus>emptySet());
    public final SpellType vermin = new SpellType("Vermin", null, "Food decays at a rate of 6%, up from a base of 1%.",
            "Vermin will feast on the granaries of " + PROVINCE + " for " + RESULT + " days", null,
            SpellOpCharacter.FADING_SPELLOP_WITH_PROVINCE, Collections.<Bonus>emptySet());
    public final SpellType exposeThieves = new SpellType("Expose Thieves", "et",
            "Decreases available stealth of target province to 93% of its original amount.",
            null, null, SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType greed = new SpellType("Greed", null, "Increases military wages by 25%.",
            "Our mages have caused our enemy's soldiers to turn greedy for " + RESULT + " days", null,
            SpellOpCharacter.FADING_SPELLOP_WITHOUT_PROVINCE, Collections.<Bonus>emptySet());
    public final SpellType foolsGold = new SpellType("Fool's Gold", "fg", "Destroys some gold.", "Our mages have turned " + RESULT +
            " gold coins in " + PROVINCE +
            " to worthless lead", null,
            SpellOpCharacter.INSTANT_SPELLOP_WITH_PROVINCE, Collections.<Bonus>emptySet());
    public final SpellType pitfalls = new SpellType("Pitfalls", null, "Increases defensive military losses by 25%.",
            "Pitfalls will haunt the lands of " + PROVINCE + " for " + RESULT + " days", null,
            SpellOpCharacter.FADING_SPELLOP_WITH_PROVINCE, Collections.<Bonus>emptySet());
    public final SpellType fireball = new SpellType("Fireball", null,
            "Kills a small and random portion of peasants. Averages 3% of peasants.",
            "A fireball burns through the skies of " + PROVINCE + ". " + RESULT +
                    " peasants are killed in the destruction!", null,
            SpellOpCharacter.INSTANT_SPELLOP_WITH_PROVINCE, Collections.<Bonus>emptySet());
    public final SpellType chastity = new SpellType("Chastity", null, "Suspends births, preventing population from growing naturally.",
            "Much to the chagrin of their men, the womenfolk of " + PROVINCE +
                    " have taken a vow of chastity for " +
                    RESULT + " days!", null, SpellOpCharacter.FADING_SPELLOP_WITH_PROVINCE,
            Collections.<Bonus>emptySet());
    public final SpellType lightningStrike = new SpellType("Lightning Strike", "ls", "Destroys a random portion of runes between 30-65%.",
            "Lightning strikes the Towers in " + PROVINCE + " and incinerates " +
                    RESULT + " runes!", null, SpellOpCharacter.INSTANT_SPELLOP_WITH_PROVINCE,
            Collections.<Bonus>emptySet());
    public final SpellType explosions = new SpellType("Explosions", null, "50% chance to reduce aid shipment to 55%-80% of original size.",
            "Explosions will rock aid shipments to and from " + PROVINCE + " for " + RESULT +
                    " days!", null, SpellOpCharacter.FADING_SPELLOP_WITH_PROVINCE,
            Collections.<Bonus>emptySet());
    public final SpellType amnesia = new SpellType("Amnesia", null,
            "Turns up to 13% of opponent's science back into research, 65% of this science is relearned, the rest is destroyed.",
            "You were able to make the people of " + PROVINCE + " temporarily forget " + RESULT +
                    " books of knowledge!", null, SpellOpCharacter.INSTANT_SPELLOP_WITH_PROVINCE,
            Collections.<Bonus>emptySet());
    public final SpellType nightmares = new SpellType("Nightmare", null,
            "Returns around 1.5% of the military troops currently at home (specialists, elites and thieves) under training for 8 days. Soldiers simply quit the army.",
            "During the night, " + RESULT +
                    " of the men in the armies and thieves' guilds of " + PROVINCE + " had nightmares",
            null, SpellOpCharacter.INSTANT_SPELLOP_WITH_PROVINCE, Collections.<Bonus>emptySet());
    public final SpellType mysticVortex = new SpellType("Mystic Vortex", "mv",
            "Nullifies spells on the target province (50% chance per spell).", null, null,
            SpellOpCharacter.OTHER, Collections.<Bonus>emptySet());
    public final SpellType meteorShowers = new SpellType("Meteor Shower", "ms",
            "Kills peasants and troops (soldiers, specialists and elites) at home each Utopian Day the spell is active.",
            "Meteors will rain across the lands of " + PROVINCE + " for " + RESULT +
                    " days", null, SpellOpCharacter.FADING_SPELLOP_WITH_PROVINCE,
            Collections.<Bonus>emptySet());
    public final SpellType tornadoes = new SpellType("Tornadoes", null, "Destroys a small and random portion of buildings.",
            "Tornadoes scour the lands of " + PROVINCE + ", laying waste to " + RESULT +
                    " acres of buildings!", null, SpellOpCharacter.INSTANT_SPELLOP_WITH_PROVINCE,
            Collections.<Bonus>emptySet());
    public final SpellType landLust = new SpellType("Land Lust", "ll", "Captures a small and random (up to 1,35%) of the enemy land.",
            "Our Land Lust over " + PROVINCE + " has given us another " + RESULT +
                    " acres of land!", null, SpellOpCharacter.INSTANT_SPELLOP_WITH_PROVINCE,
            Collections.<Bonus>emptySet());

    private final Provider<SpellDAO> spellDAOProvider;

    @Inject
    public SpellTypes(final Provider<SpellDAO> spellDAOProvider) {
        this.spellDAOProvider = spellDAOProvider;
    }

    @Override
    public void loadIntoDatabase() {
        inTransaction(new SimpleTransactionTask() {
            @Override
            public void run(final DelayedEventPoster delayedEventBus) {
                SpellDAO spellDAO = spellDAOProvider.get();
                if (!spellDAO.getAllSpellTypes().isEmpty()) return;
                spellDAO.save(Lists.newArrayList(aggression, amnesia, animateDead, anonymity, bloodlust, buildersBoon, chastity, clearSight,
                        droughts, explosions, exposeThieves, fanaticism, fertileLands, fireball, foolsGold,
                        fountainOfKnowledge, greaterProtection, greed, inspireArmy, invisibility, landLust,
                        lightningStrike, loveAndPeace, magesFury, magesFury, magicShield, meteorShowers,
                        minorProtection, mysticAura, mysticVortex, naturesBlessing, nightmares, paradise,
                        patriotism, pitfalls, quickFeet, reflectMagic, shadowlight, storms, tornadoes, townWatch,
                        treeOfGold, vermin, warSpoils));
            }
        });
    }
}
