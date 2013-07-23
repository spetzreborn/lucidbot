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

package tools;

import spi.settings.PropertiesSpecification;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UtopiaPropertiesConfig implements PropertiesSpecification {
    public static final String INTRA_KD_LOC = "Intra.KingdomLocation";
    public static final String MIN_GAIN = "Intra.MaxGains.MinimumGain";
    public static final String MAX_INTEL_AGE = "Intra.MaxGains.MaxIntelAge";
    public static final String DEFAULT_BPA = "Calculations.WPA.DefaultBPA";
    public static final String BUILT_ACRE_NW = "Calculations.Networth.BuiltAcreNw";
    public static final String TICK_LENGTH = "Core.Tick.LengthInMinutes";
    public static final String AGE_START = "Core.Tick.AgeStart";
    public static final String SEPERATE_INTEL_SERVER = "Core.SeparateIntelServer.Enabled";
    public static final String SEPERATE_INTEL_SERVER_PORT = "Core.SeparateIntelServer.Port";
    public static final String TIMERS_ANNOUNCE_ENEMY_ARMIES = "Core.Timers.AnnounceEnemyArmies";
    public static final String CACHE_UPDATE_INTERVAL = "Core.CommonEntities.UpdateInterval";
    public static final String FINDER_MAX_RESULTS = "Intel.Finder.MaxResults";
    public static final String ACTIVE_REMINDERS = "Reminders.Enabled";
    public static final String REMINDER_INTERVAL = "Reminders.Interval";
    public static final String SPELL_OP_MATRIX_COLUMNS = "SpellsOps.AllActiveMatrix.Columns";
    public static final String ANNOUNCE_EVENTS_ENABLED = "Announcers.Events.Enabled";
    public static final String ANNOUNCE_WAVE_ENABLED = "Announcers.Wave.Enabled";
    public static final String ANNOUNCE_SPELLS_ENABLED = "Announcers.Spells.Enabled";
    public static final String ANNOUNCE_OPS_ENABLED = "Announcers.Ops.Enabled";
    public static final String ANNOUNCE_RETURNING_ARMIES_ENABLED = "Announcers.ReturningArmies.Enabled";
    public static final String ANNOUNCE_NEW_TICK_ENABLED = "Announcers.NewTick.Enabled";
    public static final String ANNOUNCE_AID_ADDED_ENABLED = "Announcers.AidAdded.Enabled";
    public static final String ANNOUNCE_ARMY_ADDED_ENABLED = "Announcers.ArmyAdded.Enabled";
    public static final String ANNOUNCE_ARMY_HOME_ENABLED = "Announcers.ArmyHome.Enabled";
    public static final String ANNOUNCE_INTEL_SAVED_ENABLED = "Announcers.IntelSaved.Enabled";
    public static final String ANNOUNCE_DURATION_SPELL_ENABLED = "Announcers.DurationSpell.Enabled";
    public static final String ANNOUNCE_DURATION_OP_ENABLED = "Announcers.DurationOp.Enabled";
    public static final String ANNOUNCE_EVENT_ADDED_ENABLED = "Announcers.EventAdded.Enabled";
    public static final String ANNOUNCE_WAVE_ADDED_ENABLED = "Announcers.WaveAdded.Enabled";
    public static final String ANNOUNCE_BUILD_ADDED_ENABLED = "Announcers.BuildAdded.Enabled";
    public static final String ANNOUNCE_NAP_ADDED_ENABLED = "Announcers.NapAdded.Enabled";

    private final Map<String, String> defaults = new HashMap<>();

    public UtopiaPropertiesConfig() {
        defaults.put(INTRA_KD_LOC, "(1:1)");
        defaults.put(MIN_GAIN, "0.05");
        defaults.put(MAX_INTEL_AGE, "24");
        defaults.put(DEFAULT_BPA, "300");
        defaults.put(BUILT_ACRE_NW, "55");
        defaults.put(TICK_LENGTH, "60");
        defaults.put(AGE_START, "2012-11-16 18:00");
        defaults.put(SEPERATE_INTEL_SERVER, "false");
        defaults.put(SEPERATE_INTEL_SERVER_PORT, "49999");
        defaults.put(TIMERS_ANNOUNCE_ENEMY_ARMIES, "true");
        defaults.put(CACHE_UPDATE_INTERVAL, "10");
        defaults.put(FINDER_MAX_RESULTS, "50");
        defaults.put(ACTIVE_REMINDERS, "orders");
        defaults.put(REMINDER_INTERVAL, "5");
        defaults.put(SPELL_OP_MATRIX_COLUMNS, "Riots:ri,Storms:st,Vermin:ve,Greed:gr,Pitfalls:pf,Meteor Shower:ms");
        defaults.put(ANNOUNCE_EVENTS_ENABLED, "true");
        defaults.put(ANNOUNCE_WAVE_ENABLED, "true");
        defaults.put(ANNOUNCE_SPELLS_ENABLED, "true");
        defaults.put(ANNOUNCE_OPS_ENABLED, "true");
        defaults.put(ANNOUNCE_RETURNING_ARMIES_ENABLED, "true");
        defaults.put(ANNOUNCE_NEW_TICK_ENABLED, "true");
        defaults.put(ANNOUNCE_AID_ADDED_ENABLED, "false");
        defaults.put(ANNOUNCE_ARMY_ADDED_ENABLED, "true");
        defaults.put(ANNOUNCE_ARMY_HOME_ENABLED, "true");
        defaults.put(ANNOUNCE_INTEL_SAVED_ENABLED, "true");
        defaults.put(ANNOUNCE_DURATION_SPELL_ENABLED, "true");
        defaults.put(ANNOUNCE_DURATION_OP_ENABLED, "true");
        defaults.put(ANNOUNCE_EVENT_ADDED_ENABLED, "true");
        defaults.put(ANNOUNCE_WAVE_ADDED_ENABLED, "true");
        defaults.put(ANNOUNCE_BUILD_ADDED_ENABLED, "true");
        defaults.put(ANNOUNCE_NAP_ADDED_ENABLED, "true");
    }

    @Override
    public Path getFilePath() {
        return Paths.get("utopia.properties");
    }

    @Override
    public Map<String, String> getDefaults() {
        return Collections.unmodifiableMap(defaults);
    }
}
