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

package events;

import api.irc.entities.IRCUser;
import api.runtime.IRCContext;
import database.models.AttackType;
import lombok.Getter;

/**
 * An attack was pasted on irc, and this event contains some information about the attack, depending on which part of
 * the attack message was just parsed
 */
@Getter
public class AttackInfoPastedEvent {
    private final IRCContext context;
    private final IRCUser attacker;
    private final String provinceName;
    private final String kdLoc;
    private final AttackType attackType;
    private final Integer gain;
    private final Long returnTime;
    private final Integer kills;
    private final Boolean spreadPlague;
    private final Boolean gotPlagued;

    private AttackInfoPastedEvent(final IRCContext context, final String provinceName, final String kdLoc, final AttackType attackType,
                                  final Integer gain, final Long returnTime, final Integer kills, final Boolean spreadPlague,
                                  final Boolean gotPlagued) {
        this.context = context;
        this.attacker = context.getUser();
        this.provinceName = provinceName;
        this.kdLoc = kdLoc;
        this.attackType = attackType;
        this.gain = gain;
        this.returnTime = returnTime;
        this.kills = kills;
        this.spreadPlague = spreadPlague;
        this.gotPlagued = gotPlagued;
    }

    public static AttackInfoPastedEvent createNewAttackInfoEvent(final IRCContext context, final String provinceName, final String kdLoc,
                                                                 final AttackType attackType, final Integer gain) {
        return new AttackInfoPastedEvent(context, provinceName, kdLoc, attackType, gain, null, null, null, null);
    }

    public static AttackInfoPastedEvent createReturnTimeInfoEvent(final IRCContext context, final Long returnTime) {
        return new AttackInfoPastedEvent(context, null, null, null, null, returnTime, null, null, null);
    }

    public static AttackInfoPastedEvent createKillsInfoEvent(final IRCContext context, final Integer kills) {
        return new AttackInfoPastedEvent(context, null, null, null, null, null, kills, null, null);
    }

    public static AttackInfoPastedEvent createPlagueSpreadInfoEvent(final IRCContext context) {
        return new AttackInfoPastedEvent(context, null, null, null, null, null, null, Boolean.TRUE, null);
    }

    public static AttackInfoPastedEvent createPlagueReceivedInfoEvent(final IRCContext context) {
        return new AttackInfoPastedEvent(context, null, null, null, null, null, null, null, Boolean.TRUE);
    }
}
