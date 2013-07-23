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
import api.tools.numbers.NumberUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import database.models.AidType;
import events.AidSentEvent;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses incoming text for "aid sent" messages and announces them when they're found
 */
public class AidParser implements EventListener {
    private static final Pattern AID = Pattern.compile("We have sent (?<aid>.+?) to (?<target>[^(]+" +
            UtopiaValidationType.KDLOC.getPatternString() + ')');
    private static final Pattern INDIVIDUAL_PACKAGES = Pattern
            .compile("(?<amount>" + ValidationType.INT.getPattern() + ") (?<resource>" + AidType.getAidMessageGroup() + ')');

    private final EventBus eventBus;
    private final ThreadingManager threadingManager;

    @Inject
    public AidParser(final EventBus eventBus, final ThreadingManager threadingManager) {
        this.eventBus = eventBus;
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

        Matcher matcher = AID.matcher(text);
        if (matcher.find()) {
            String target = matcher.group("target");
            String provinceName = target.substring(0, target.indexOf('(')).trim();

            Matcher packageMatcher = INDIVIDUAL_PACKAGES.matcher(matcher.group("aid"));
            while (packageMatcher.find()) {
                int amount = NumberUtil.parseInt(packageMatcher.group("amount"));
                AidType aidType = AidType.fromAidMessage(packageMatcher.group("resource"));
                if (amount < 1 || aidType == null) return;

                eventBus.post(new AidSentEvent(context.getUser(), aidType, amount, provinceName, context));
            }
        }
    }
}
