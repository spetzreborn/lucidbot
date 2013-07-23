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

import api.database.models.AccessLevel;
import api.events.bot.NonCommandEvent;
import api.irc.ValidationType;
import api.runtime.IRCContext;
import api.runtime.ThreadingManager;
import api.settings.PropertiesCollection;
import api.tools.numbers.NumberUtil;
import api.tools.time.DateUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import database.models.AttackType;
import events.AttackInfoPastedEvent;
import lombok.extern.log4j.Log4j;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tools.UtopiaPropertiesConfig.TICK_LENGTH;

@Log4j
public class AttackParser implements EventListener {
    private static final Pattern TARGET = Pattern
            .compile("Your forces arrive at (?<target>[^(]+" + UtopiaValidationType.KDLOC.getPatternString() + ')');
    private static final Pattern BOUNCE_TARGET = Pattern
            .compile("Your army was no match for the defenses of (?<target>[^(]+" + UtopiaValidationType.KDLOC.getPatternString() + ')');
    private static final Pattern KILLS = Pattern.compile("We killed about (?<result>" + ValidationType.INT.getPattern() + ") enemy troops");
    private static final Pattern IMPRISONMENT = Pattern
            .compile("We also imprisoned (?<result>" + ValidationType.INT.getPattern() + ") additional troops in our Dungeons");
    private static final Pattern RETURN = Pattern
            .compile("Our forces will be available again in (?<result>" + ValidationType.DOUBLE.getPattern() + ") days");
    private static final Pattern GOT_PLAGUED = Pattern
            .compile("(The Plague has spread throughout our people!|It appears we have contracted The Plague!)");
    private static final Pattern SPREAD_PLAGUE = Pattern.compile("Our troops have spread the plague into");

    private final EventBus eventBus;
    private final PropertiesCollection properties;
    private final ThreadingManager threadingManager;

    @Inject
    public AttackParser(final EventBus eventBus,
                        final PropertiesCollection properties,
                        final ThreadingManager threadingManager) {
        this.eventBus = eventBus;
        this.properties = properties;
        this.threadingManager = threadingManager;
    }

    @Subscribe
    public void onNonCommandEvent(final NonCommandEvent event) {
        threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                parse(event);
            }
        });
    }

    private void parse(final NonCommandEvent event) {
        IRCContext context = event.getContext();
        if (!AccessLevel.USER.allows(context.getUser(), context.getChannel())) return;

        String text = context.getInput();

        Matcher matcher = TARGET.matcher(text);
        if (matcher.find()) {
            AttackInfoPastedEvent announceInfo = handleNormalAttack(context, text, matcher.group("target"));
            if (announceInfo != null) eventBus.post(announceInfo);
        }

        matcher = BOUNCE_TARGET.matcher(text);
        if (matcher.find()) {
            AttackInfoPastedEvent announceInfo = handleBounceAttack(context, matcher.group("target"));
            eventBus.post(announceInfo);
        }

        matcher = KILLS.matcher(text);
        if (matcher.find()) {
            AttackInfoPastedEvent announceInfo = handleKills(context, text, matcher.group("result"));
            eventBus.post(announceInfo);
        }

        matcher = RETURN.matcher(text);
        if (matcher.find()) {
            AttackInfoPastedEvent announceInfo = handleReturnTime(context, matcher.group("result"));
            eventBus.post(announceInfo);
        }

        matcher = GOT_PLAGUED.matcher(text);
        if (matcher.find()) {
            AttackInfoPastedEvent announceInfo = AttackInfoPastedEvent.createPlagueReceivedInfoEvent(context);
            eventBus.post(announceInfo);
        }

        matcher = SPREAD_PLAGUE.matcher(text);
        if (matcher.find()) {
            AttackInfoPastedEvent announceInfo = AttackInfoPastedEvent.createPlagueSpreadInfoEvent(context);
            eventBus.post(announceInfo);
        }
    }

    private static AttackInfoPastedEvent handleNormalAttack(final IRCContext context,
                                                            final String text,
                                                            final String target) {
        int kdLocStartIndex = target.indexOf('(');
        String provinceName = target.substring(0, kdLocStartIndex).trim();
        String kdLoc = target.substring(kdLocStartIndex);

        Integer gain = null;
        AttackType attackType = null;
        for (AttackType type : AttackType.values()) {
            Pattern attackRegex = type.getAttackRegex();
            if (attackRegex != null) {
                Matcher gainsMatcher = attackRegex.matcher(text);
                if (gainsMatcher.find() && gainsMatcher.groupCount() > 0) {
                    gain = NumberUtil.parseInt(gainsMatcher.group(1));
                    attackType = type;
                    break;
                }
            }
        }

        if (attackType == null) {
            log.warn("Unknown attack type: " + text);
            return null;
        }

        return AttackInfoPastedEvent.createNewAttackInfoEvent(context, provinceName, kdLoc, attackType, gain);
    }

    private static AttackInfoPastedEvent handleBounceAttack(final IRCContext context, final String target) {
        int kdLocStartIndex = target.indexOf('(');
        String provinceName = target.substring(0, kdLocStartIndex).trim();
        String kdLoc = target.substring(kdLocStartIndex);
        return AttackInfoPastedEvent.createNewAttackInfoEvent(context, provinceName, kdLoc, AttackType.BOUNCE, null);
    }

    private static AttackInfoPastedEvent handleKills(final IRCContext context, final String text, final String result) {
        int kills = NumberUtil.parseInt(result);
        Matcher matcher = IMPRISONMENT.matcher(text);
        if (matcher.find()) {
            kills += NumberUtil.parseInt(matcher.group("result"));
        }
        return AttackInfoPastedEvent.createKillsInfoEvent(context, kills);
    }

    private AttackInfoPastedEvent handleReturnTime(final IRCContext context, final String result) {
        long returnTime =
                System.currentTimeMillis() + DateUtil.minutesToMillis(properties.getInteger(TICK_LENGTH) * NumberUtil.parseDouble(result));
        return AttackInfoPastedEvent.createReturnTimeInfoEvent(context, returnTime);
    }
}
