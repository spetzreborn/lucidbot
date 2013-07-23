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

package tools.parsing;

import database.daos.DragonDAO;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static tools.parsing.NewsParser.DATE;
import static tools.parsing.NewsParser.KD;

public class NonAttackNewsTypes {
    public static final String AID = "Aid";

    //DRAGON
    public static final String INCOMING_DRAGON_STARTED = "Incoming dragon started";
    public static final String OUTGOING_DRAGON_STARTED = "Outgoing dragon started";
    public static final String INCOMING_DRAGON_CANCELLED = "Incoming dragon cancelled";
    public static final String OUTGOING_DRAGON_CANCELLED = "Outgoing dragon cancelled";
    public static final String INCOMING_DRAGON_SENT = "Incoming dragon sent";
    public static final String OUTGOING_DRAGON_SENT = "Outgoing dragon sent";
    public static final String DRAGON_SLAIN = "Dragon slain";

    //PROVINCES MOVING
    public static final String INCOMING_DEFECTION = "Incoming defection";
    public static final String OUTGOING_DEFECTION = "Outgoing defection";
    public static final String DEAD_PROVINCE = "Dead province";
    public static final String KILLED_PROVINCE = "Killed province";
    public static final String ABANDONED_PROVINCE = "Abandoned province";
    public static final String PLAYER_LEAVING = "Player leaving";
    public static final String PLAYER_LEAVING_DESTROYING = "Player leaving and destroying";
    public static final String PLAYER_JOINING = "Player joining";

    //CF
    public static final String INCOMING_CF_OFFERED = "Incoming cf offered";
    public static final String OUTGOING_CF_OFFERED = "Outgoing cf offered";
    public static final String INCOMING_CF_ACCEPTED = "Incoming cf accepted";
    public static final String OUTGOING_CF_ACCEPTED = "Outgoing cf accepted";
    public static final String INCOMING_CF_BROKEN = "Incoming cf broken";
    public static final String OUTGOING_CF_BROKEN = "Outgoing cf broken";
    public static final String INCOMING_CF_REJECTED = "Incoming cf rejected";
    public static final String OUTGOING_CF_REJECTED = "Outgoing cf rejected";
    public static final String CF_WITHDRAWN = "CF withdrawn";

    //WAR
    public static final String INCOMING_WAR_DECLARED = "Incoming war declared";
    public static final String OUTGOING_WAR_DECLARED = "Outgoing war declared";
    public static final String MAX_HOSTILITY_AUTO_WAR_DECLARATION = "Max hostility auto war";
    public static final String INCOMING_WITHDRAWAL = "Incoming withdrawal";
    public static final String OUTGOING_WITHDRAWAL = "Outgoing withdrawal";
    public static final String INCOMING_MUTUAL_PEACE_ACCEPTED = "Incoming mutual peace accepted";
    public static final String OUTGOING_MUTUAL_PEACE_ACCEPTED = "Outgoing mutual peace accepted";
    public static final String END_OF_WAR_CF_CANCELLED = "End of war cf cancelled";

    private final Map<String, Pattern> newsTypes = new HashMap<>();

    @Inject
    public NonAttackNewsTypes(final DragonDAO dragonDAO) {
        String dragonGroup = dragonDAO.getDragonGroup();

        newsTypes.put(AID, Pattern.compile(DATE + "\\s*(?<source>.+?) has sent an aid shipment to (?<target>.+?)\\."));
        newsTypes.put(INCOMING_DRAGON_STARTED, Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has begun a (?<value>" + dragonGroup +
                ") Dragon project against us!"));
        newsTypes.put(OUTGOING_DRAGON_STARTED, Pattern.compile(DATE + "\\s*Our kingdom has begun a (?<value>" + dragonGroup +
                ") Dragon project targetted at [^(]+(?<target>" + KD + ")\\."));
        newsTypes.put(INCOMING_DRAGON_CANCELLED,
                Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has cancelled their dragon project targetted at us\\."));
        newsTypes.put(OUTGOING_DRAGON_CANCELLED,
                Pattern.compile(DATE + "\\s*Our kingdom has cancelled the dragon project to [^(]+(?<target>" + KD + ")\\."));
        newsTypes.put(INCOMING_DRAGON_SENT, Pattern.compile(DATE + "\\s*(?:A|An) (?<value>" + dragonGroup + ") Dragon from " +
                "[^(]+(?<source>" + KD + ") has begun ravaging our lands!"));
        newsTypes.put(OUTGOING_DRAGON_SENT, Pattern.compile(DATE + "\\s*Our dragon has set flight to ravage (?<target>[^(]+ " + KD + ')'));
        newsTypes.put(DRAGON_SLAIN, Pattern.compile(DATE + "\\s*(?<source>.+?) has slain the dragon ravaging our lands!"));
        newsTypes.put(INCOMING_DEFECTION,
                Pattern.compile(DATE + "\\s*The province of (?<source>.+?) has defected to us from (?<value>[^(]+ " + KD + ")\\."));
        newsTypes.put(OUTGOING_DEFECTION, Pattern.compile(DATE + "\\s*(?<source>.+?) has defected to (<?<target>[^(]+ " + KD + ')'));
        newsTypes.put(DEAD_PROVINCE, Pattern.compile(DATE + "\\s*Alas, the truant .*? has lead (?<source>.+?) into a state of neglect\\. " +
                ".*? has now collapsed and lies in ruins\\."));
        newsTypes.put(KILLED_PROVINCE,
                Pattern.compile(DATE + "\\s*Alas, the once proud province of (?<source>.+?) has collapsed and lies in ruins\\."));
        newsTypes.put(ABANDONED_PROVINCE, Pattern.compile(
                DATE + "\\s*Staying in the darkest shadows, .*? slips out of (?<source>.+?) unnoticed, never to be seen again\\."));
        newsTypes.put(PLAYER_LEAVING,
                Pattern.compile(DATE + "\\s*The leader of (?<source>.+?) has chosen to join [^(]+(?<target>" + KD + ")\\. " +
                        "All in castle black gather their possessions and depart this kingdom forever\\."));
        newsTypes.put(PLAYER_LEAVING_DESTROYING,
                Pattern.compile(DATE + "\\s*As the ultimate betrayal, .*? destroys all in the land of (?<source>.+?) " +
                        "before leaving for a new kingdom\\."));
        newsTypes.put(PLAYER_JOINING, Pattern.compile(
                DATE + "\\s*The leader of (?<value>.+?) has wisely chosen to join us from [^(]+(?<source>" + KD + ")\\."));
        newsTypes.put(INCOMING_CF_OFFERED,
                Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has proposed a formal ceasefire with our kingdom\\."));
        newsTypes.put(OUTGOING_CF_OFFERED,
                Pattern.compile(DATE + "\\s*We have proposed a ceasefire offer to [^(]+(?<target>" + KD + ")\\."));
        newsTypes.put(INCOMING_CF_ACCEPTED,
                Pattern.compile(DATE + "\\s*We have entered into a formal ceasefire with [^(]+(?<target>" + KD + ")\\."));
        newsTypes.put(OUTGOING_CF_ACCEPTED, Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has accepted our ceasefire proposal!"));
        newsTypes.put(INCOMING_CF_BROKEN,
                Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has broken their ceasefire agreement with us!"));
        newsTypes.put(OUTGOING_CF_BROKEN, Pattern.compile(DATE + "\\s*We have cancelled our ceasefire with [^(]+(?<target>" + KD + ")!"));
        newsTypes.put(INCOMING_CF_REJECTED,
                Pattern.compile(DATE + "\\s*We have rejected a ceasefire offer from [^(]+(?<source>" + KD + ")\\."));
        newsTypes.put(OUTGOING_CF_REJECTED, Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has declined our ceasefire proposal!"));
        newsTypes.put(CF_WITHDRAWN, Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has withdrawn their ceasefire proposal\\."));
        newsTypes.put(INCOMING_WAR_DECLARED, Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has declared WAR with our kingdom!"));
        newsTypes.put(OUTGOING_WAR_DECLARED, Pattern.compile(DATE + "\\s*We have declared WAR on [^(]+(?<target>" + KD + ")!"));
        newsTypes.put(MAX_HOSTILITY_AUTO_WAR_DECLARATION, Pattern.compile(DATE + "\\s*Our prolonged hostility with [^(]+(?<target>" + KD + ") has forced us into a WAR"));
        newsTypes.put(INCOMING_WITHDRAWAL,
                Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has withdrawn from war. Our people rejoice at our victory!"));
        newsTypes.put(OUTGOING_WITHDRAWAL, Pattern.compile(
                DATE + "\\s*Unable to achieve victory, our Kingdom has withdrawn from war with [^(]+(?<target>" + KD + ")\\. " +
                        "Our failed war has finally ended!"));
        newsTypes.put(INCOMING_MUTUAL_PEACE_ACCEPTED,
                Pattern.compile(DATE + "\\s*We have accepted an offer of Peace by [^(]+(?<source>" + KD + ")\\. " +
                        "The people celebrate the end of our War!"));
        newsTypes.put(OUTGOING_MUTUAL_PEACE_ACCEPTED,
                Pattern.compile(DATE + "\\s*[^(]+(?<source>" + KD + ") has accepted our offer of Peace\\. " +
                        "The people celebrate the end of our War!"));
        newsTypes.put(END_OF_WAR_CF_CANCELLED,
                Pattern.compile(DATE + "\\s*We have ordered an early end to the post-war period with [^(]+(?<target>" + KD + ")\\."));
    }

    public Pattern getPattern(final String name) {
        return newsTypes.get(name);
    }

    public Map<String, Pattern> getAll() {
        return Collections.unmodifiableMap(newsTypes);
    }
}
