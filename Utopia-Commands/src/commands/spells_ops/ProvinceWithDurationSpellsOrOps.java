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

package commands.spells_ops;

import database.models.DurationOp;
import database.models.DurationSpell;
import database.models.Province;
import database.models.SpellsOrOpsDurationInfo;

import java.util.*;

public class ProvinceWithDurationSpellsOrOps {

    public static ProvinceWithDurationSpellsOrOps create(final List<String> columnTypesInOrder,
                                                         final Province province) {
        Map<String, SpellsOrOpsDurationInfo> spellsAndOpsMap = createSpellsAndOpsMap(province);
        List<String> durations = new ArrayList<>(columnTypesInOrder.size());
        for (String type : columnTypesInOrder) {
            SpellsOrOpsDurationInfo durationInfo = spellsAndOpsMap.get(type);
            durations.add(durationInfo == null ? "-" : durationInfo.getTimeLeftInHours());
        }
        return new ProvinceWithDurationSpellsOrOps(province, durations);
    }

    private static Map<String, SpellsOrOpsDurationInfo> createSpellsAndOpsMap(final Province province) {
        Map<String, SpellsOrOpsDurationInfo> spellsAndOps = new HashMap<>();
        for (DurationSpell spell : province.getDurationSpells()) {
            spellsAndOps.put(spell.getType().getName(), spell);
        }
        for (DurationOp op : province.getDurationOps()) {
            spellsAndOps.put(op.getType().getName(), op);
        }
        return spellsAndOps;
    }

    private final Province province;
    private final List<String> durations;

    private ProvinceWithDurationSpellsOrOps(final Province province, final List<String> durations) {
        this.province = province;
        this.durations = durations;
    }

    public Province getProvince() {
        return province;
    }

    public List<String> getDurations() {
        return Collections.unmodifiableList(durations);
    }
}
