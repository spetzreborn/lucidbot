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

package intel;

public enum ProvinceResourceType {
    BE("be", false),
    BOOKS("books", true),
    BPA("bpa", true),
    BUILDING_PERCENTAGE(null, false),
    BUILDING_AMOUNT(null, false),
    DEF_SPECS("dspecs", false),
    DEF_SPECS_PER_ACRE("dspa", false),
    ELITES("elites", false),
    ELITES_OUT("elitesout", false),
    ELITES_PER_ACRE("epa", false),
    ESTIMATED_CURRENT_DEFENSE("estimateddef", false),
    ESTIMATED_CURRENT_OFFENSE("estimatedoff", false),
    FOOD("food", true),
    GC("gc", true),
    GC_PER_ACRE("gcpa", false),
    HORSES("horses", true),
    LAND("land", false),
    MANA("mana", false),
    MOD_DEFENSE("def", false),
    MOD_DEFENSE_PER_ACRE("dpa", false),
    MOD_OFFENSE("off", false),
    MOD_OFFENSE_PER_ACRE("opa", false),
    MOD_THIEVES_PER_ACRE("mtpa", false),
    MOD_WIZARDS_PER_ACRE("mwpa", false),
    NETWORTH("nw", false),
    OFF_SPECS("ospecs", false),
    OFF_SPECS_PER_ACRE("ospa", false),
    PEASANTS("peasants", true),
    PEASANTS_PER_ACRE("ppa", false),
    PRACTICAL_MOD_DEF("pmd", false),
    PRACTICAL_MOD_DEF_PER_ACRE("pmdpa", false),
    PRACTICAL_MOD_OFF("pmo", false),
    PRACTICAL_MOD_OFF_PER_ACRE("pmopa", false),
    RUNES("runes", true),
    SOLDIERS("soldiers", true),
    STEALTH("stealth", false),
    TB("tb", false),
    THIEVES("thieves", false),
    THIEVES_PER_ACRE("tpa", false),
    WIZARDS("wizards", false),
    WIZARDS_PER_ACRE("wpa", false);

    private final String command;
    private final boolean sum;

    ProvinceResourceType(final String command, final boolean sum) {
        this.command = command;
        this.sum = sum;
    }

    public String getCommand() {
        return command;
    }

    public boolean shouldSum() {
        return sum;
    }
}
