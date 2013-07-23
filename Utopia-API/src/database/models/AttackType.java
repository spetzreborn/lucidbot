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

package database.models;

import api.common.HasName;
import api.tools.text.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static tools.parsing.NewsParser.*;

public enum AttackType implements HasName {
    AMBUSH("Ambush", "AMB", GainsSpecification.GAINS_LAND, null,
           //Incoming news regex
           DATE + "\\s*(?<source>[^(]+" + KD + ") ambushed armies from (?<target>[^(]+" + KD + ") " + "and took (?<value>" + INT +
           ") acres of land\\.",
           //Outgoing news regex
           DATE + "\\s*(?<source>[^(]+" + KD + ") recaptured (?<value>" + INT + ") " + "acres of land from (?<target>[^(]+" + KD + ")\\.",
           //Result message regex
           "Your army has recaptured ([0-9,]{1,})") {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            return new GainVsDamage(gain, gain);
        }
    },

    BOUNCE("Bounce", "BO", GainsSpecification.NON_LAND, null,
           //Incoming news regex
           DATE + "\\s*(?<source>[^(]+" + KD + ") attempted to invade (?<target>[^(]+" + KD + ")\\.",
           //Outgoing news regex
           DATE + "\\s*(?<source>[^(]+" + KD + ") attempted an invasion of (?<target>[^(]+" + KD + "), " + "but was repelled\\.",
           //Result message regex
           "(Alas, .+? it appears our army was much too weak to break their defenses!|" +
           "Our army appears to have failed, .+?\\.  I am truly sorry\\.|" +
           "Your troops march onto the battlefield and are quickly driven back, unable to break through!)") {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            return GainVsDamage.ZERO;
        }
    },

    CONQUEST("Conquest", "CQ", GainsSpecification.GAINS_LAND, 0.068,
             //Incoming news regex
             null,
             //Outgoing news regex
             DATE + "\\s*(?<source>[^(]+" + KD + "), captured (?<value>" + INT + ')' + " acres of land from (?<target>[^(]+" + KD + ")\\.",
             //Result message regex
             "(?:Our troops were able to get a small foothold into enemy territory\\.|" +
             "Our army broke our enemy's initial defenses but was held from going further\\.|" +
             "Our troops had a moderate amount of success getting behind enemy defenses\\.|" +
             "Our army has broken their primary lines of defense!|" +
             "We crushed opposing defenses and conquered all of the lands we targeted\\.)" +
             ".*?Your army has taken ([0-9,]{1,})") {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            if (isIncomingHit) return new GainVsDamage(isWar ? gain + gain / 10 : gain, gain);
            return new GainVsDamage(gain, gain);
        }
    },

    INTRA_RAZE("Intra Raze", "IRA", GainsSpecification.DESTROYS_LAND, null,
               //Incoming news regex
               null,
               //Outgoing news regex
               DATE + "\\s*In local kingdom strife (?<source>[^(]+" + KD + ") invaded (?<target>[^(]+" + KD + ") " +
               "and razed (?<value>" + INT + ") acres of land\\.",
               //Result message regex
               null) {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            return new GainVsDamage(0, gain);
        }
    },

    INTRA_TM("Intra Traditional March", "ITM", GainsSpecification.GAINS_LAND, null,
             //Incoming news regex
             null,
             //Outgoing news regex
             DATE + "\\s*In local kingdom strife, (?<source>[^(]+" + KD + ") invaded (?<target>[^(]+" + KD + ") " +
             "and captured (?<value>" + INT + ") acres of land\\.",
             //Result message regex
             null) {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            return new GainVsDamage(gain / 20, gain);
        }
    },

    LEARN("Learn", "LEA", GainsSpecification.NON_LAND, 0.09375,
          //Incoming news regex
          DATE + "\\s*(?<source>[^(]+" + KD + ") attacked and stole from (?<target>.+? " + KD + ")\\.",
          //Outgoing news regex
          DATE + "\\s*(?<source>[^(]+" + KD + ") invaded and stole from (?<target>[^(]+" + KD + ")\\.",
          //Result message regex
          "Your army stole ([0-9,]{1,})") {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            return GainVsDamage.ZERO;
        }
    },

    MASSACRE("Massacre", "MASS", GainsSpecification.NON_LAND, null,
             //Incoming news regex
             DATE + "\\s*(?<source>[^(]+" + KD + ") invaded (?<target>[^(]+" + KD + ") " + "and killed (?<value>" + INT + ") people\\.",
             //Outgoing news regex
             DATE + "\\s*(?<source>[^(]+" + KD + ") killed (?<value>" + INT + ") " + "people within (?<target>[^(]+" + KD + ")\\.",
             //Result message regex
             "Your army massacred ([0-9,]{1,})") {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            return new GainVsDamage(gain);
        }
    },

    PLUNDER("Plunder", "PL", GainsSpecification.NON_LAND, 0.5,
            //Incoming news regex
            DATE + "\\s*(?<source>[^(]+" + KD + ") " + "attacked and pillaged the lands of (?<target>[^(]+" + KD + ")\\.",
            //Outgoing news regex
            DATE + "\\s*(?<source>[^(]+" + KD + ") invaded and pillaged (?<target>[^(]+" + KD + ")\\.",
            //Result message regex
            "Your army looted ([0-9,]{1,})") {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            return GainVsDamage.ZERO;
        }
    },

    RAZE("Raze", "RA", GainsSpecification.DESTROYS_LAND_OOW, null,
         //Incoming news regex
         DATE + "\\s*(?<source>[^(]+" + KD + ") razed (?<value>" + INT + ") " + "acres of (?<target>[^(]+" + KD + ")\\.",
         //Outgoing news regex
         DATE + "\\s*(?<source>[^(]+" + KD + ") invaded (?<target>[^(]+" + KD + ") " + "and razed (?<value>" + INT + ") acres of land\\.",
         //Result message regex
         "Your army burned and (?:razed|destroyed) ([0-9,]{1,})") {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            return new GainVsDamage(0, isWar ? 0 : gain);
        }
    },

    //NOTE: this should always be after Conquest, since they can't be distinguished from the gains part alone
    TM("Traditional March", "TM", GainsSpecification.GAINS_LAND, 0.12,
       //Incoming news regex
       DATE + "\\s*(?<source>[^(]+" + KD + ") invaded (?<target>[^(]+" + KD + ") " + "and captured (?<value>" + INT + ") acres of land\\.",
       //Outgoing news regex
       DATE + "\\s*(?<source>[^(]+" + KD + ") captured (?<value>" + INT + ") " + "acres of land from (?<target>[^(]+" + KD + ")\\.",
       //Result message regex
       "Your army has taken ([0-9,]{1,})") {
        @Override
        public GainVsDamage calcGain(int gain, boolean isWar, boolean isIncomingHit) {
            if (isIncomingHit) return new GainVsDamage(gain, gain);
            return new GainVsDamage(gain, gain);
        }
    };

    private final String name;
    private final String shortName;
    private final GainsSpecification gainsSpecification;
    private final Pattern incomingAttackNewsPattern;
    private final Pattern outgoingAttackNewsPattern;
    private final Pattern attackRegex;
    private final Double baseGainFactor;

    AttackType(String name, String shortName, GainsSpecification gainsSpecification, Double baseGainFactor,
               String incomingAttackNewsPattern, String outgoingAttackNewsPattern, String attackRegex) {
        this.name = name;
        this.shortName = shortName;
        this.gainsSpecification = gainsSpecification;
        this.incomingAttackNewsPattern = incomingAttackNewsPattern == null ? null : Pattern.compile(incomingAttackNewsPattern);
        this.outgoingAttackNewsPattern = outgoingAttackNewsPattern == null ? null : Pattern.compile(outgoingAttackNewsPattern);
        this.attackRegex = attackRegex == null ? null : Pattern.compile(attackRegex);
        this.baseGainFactor = baseGainFactor;
    }

    public static String getAttackTypeGroup() {
        AttackType[] values = values();
        List<String> names = new ArrayList<>(values.length * 2);
        for (AttackType value : values) {
            names.add(value.name);
            names.add(value.shortName);
        }
        return StringUtil.merge(names, '|');
    }

    public static AttackType fromName(final String nameOrShortName) {
        for (AttackType attackType : values()) {
            if (attackType.name.equalsIgnoreCase(nameOrShortName) || attackType.shortName.equalsIgnoreCase(nameOrShortName))
                return attackType;
        }
        return null;
    }

    public abstract GainVsDamage calcGain(final int gain, final boolean isWar, final boolean isIncomingHit);

    @Override
    public String getName() {
        return name;
    }

    public String getTypeName() {
        return getName();
    }

    public String getShortName() {
        return shortName;
    }

    public GainsSpecification getGainsSpecification() {
        return gainsSpecification;
    }

    public Pattern getIncomingAttackNewsPattern() {
        return incomingAttackNewsPattern;
    }

    public Pattern getOutgoingAttackNewsPattern() {
        return outgoingAttackNewsPattern;
    }

    public Pattern getAttackRegex() {
        return attackRegex;
    }

    public Double getBaseGainFactor() {
        return baseGainFactor;
    }

    public static class GainVsDamage {
        public static final GainVsDamage ZERO = new GainVsDamage(0);
        private final int gain;
        private final int damage;
        private final int nonLandDamage;

        public GainVsDamage(int gain, int damage) {
            this.gain = gain;
            this.damage = damage;
            this.nonLandDamage = 0;
        }

        public GainVsDamage(int nonLandDamage) {
            this.gain = 0;
            this.damage = 0;
            this.nonLandDamage = nonLandDamage;
        }

        public int getGain() {
            return gain;
        }

        public int getDamage() {
            return damage;
        }

        public int getNonLandDamage() {
            return nonLandDamage;
        }
    }
}
