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
import database.models.DragonProjectType;
import events.DragonActionEvent;
import events.DragonProjectUpdateEvent;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DragonParser implements EventListener {
    private static final Pattern KILLING = Pattern.compile("the dragon is weakened by (" + ValidationType.INT.getPattern() + ") points");
    private static final Pattern DONATING = Pattern
            .compile("You have donated (" + ValidationType.INT.getPattern() + ") gold coins? to the quest of launching a dragon");
    private static final Pattern CURRENT_HP = Pattern
            .compile("We estimate him to have (" + ValidationType.INT.getPattern() + ") points of strength left");
    private static final Pattern CURRENT_DONATION_LEFT = Pattern
            .compile('(' + ValidationType.INT.getPattern() + ") gold coins are still needed to complete development");

    private final ThreadingManager threadingManager;
    private final EventBus eventBus;

    @Inject
    public DragonParser(final ThreadingManager threadingManager, final EventBus eventBus) {
        this.threadingManager = threadingManager;
        this.eventBus = eventBus;
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

        String text = event.getContext().getInput();

        Matcher matcher = KILLING.matcher(text);
        if (matcher.find()) {
            int kill = NumberUtil.parseInt(matcher.group(1));
            eventBus.post(new DragonActionEvent(event.getContext(), DragonProjectType.KILLING, kill));
        }

        matcher = DONATING.matcher(text);
        if (matcher.find()) {
            int donated = NumberUtil.parseInt(matcher.group(1));
            eventBus.post(new DragonActionEvent(event.getContext(), DragonProjectType.SENDING, donated));
        }

        matcher = CURRENT_HP.matcher(text);
        if (matcher.find()) {
            int hp = NumberUtil.parseInt(matcher.group(1));
            eventBus.post(new DragonProjectUpdateEvent(event.getContext(), DragonProjectType.KILLING, hp));
        }

        matcher = CURRENT_DONATION_LEFT.matcher(text);
        if (matcher.find()) {
            int left = NumberUtil.parseInt(matcher.group(1));
            eventBus.post(new DragonProjectUpdateEvent(event.getContext(), DragonProjectType.SENDING, left));
        }
    }
}
