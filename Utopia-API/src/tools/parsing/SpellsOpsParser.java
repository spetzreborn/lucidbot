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
import api.runtime.IRCContext;
import api.runtime.ThreadingManager;
import api.tools.numbers.NumberUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.CommonEntitiesAccess;
import database.daos.ProvinceDAO;
import database.daos.UserSpellOpTargetDAO;
import database.models.OpType;
import database.models.Province;
import database.models.SpellType;
import events.OpPastedEvent;
import events.SpellPastedEvent;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpellsOpsParser implements EventListener {
    private final ThreadingManager threadingManager;
    private final EventBus eventBus;
    private final Provider<ProvinceDAO> provinceDAOProvider;
    private final Provider<UserSpellOpTargetDAO> spellOpTargetDAOProvider;

    private final ConcurrentMap<Pattern, SpellType> spellPatternsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Pattern, OpType> opPatternsMap = new ConcurrentHashMap<>();

    @Inject
    public SpellsOpsParser(final ThreadingManager threadingManager, final CommonEntitiesAccess commonEntitiesAccess,
                           final EventBus eventBus, final Provider<ProvinceDAO> provinceDAOProvider,
                           final Provider<UserSpellOpTargetDAO> spellOpTargetDAOProvider) {
        this.threadingManager = threadingManager;
        this.eventBus = eventBus;
        this.provinceDAOProvider = provinceDAOProvider;
        this.spellOpTargetDAOProvider = spellOpTargetDAOProvider;

        for (SpellType spellType : commonEntitiesAccess.getAllSpellTypes()) {
            if (spellType.getCastRegex() != null) spellPatternsMap.put(Pattern.compile(spellType.getCastRegex()), spellType);
        }

        for (OpType opType : commonEntitiesAccess.getAllOpTypes()) {
            if (opType.getOpRegex() != null) opPatternsMap.put(Pattern.compile(opType.getOpRegex()), opType);
        }
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
        Matcher matcher;
        for (Map.Entry<Pattern, SpellType> entry : spellPatternsMap.entrySet()) {
            matcher = entry.getKey().matcher(text);
            if (matcher.find()) {
                SpellType type = entry.getValue();
                Province target = type.getSpellCharacter()
                        .getTarget(provinceDAOProvider.get(), spellOpTargetDAOProvider.get(), event.getContext().getBotUser(),
                                matcher);
                if (target != null) {
                    eventBus.post(new SpellPastedEvent(target.getId(), type.getId(), NumberUtil.parseInt(matcher.group("result")),
                            event.getContext()));
                }
                return;
            }
        }
        for (Map.Entry<Pattern, OpType> entry : opPatternsMap.entrySet()) {
            matcher = entry.getKey().matcher(text);
            if (matcher.find()) {
                OpType type = entry.getValue();
                Province target = type.getOpCharacter()
                        .getTarget(provinceDAOProvider.get(), spellOpTargetDAOProvider.get(), event.getContext().getBotUser(),
                                matcher);
                if (target != null) {
                    eventBus.post(new OpPastedEvent(target.getId(), type.getId(), NumberUtil.parseInt(matcher.group("result")),
                            event.getContext()));
                }
                return;
            }
        }
    }
}
