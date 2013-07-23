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

package internal.irc;

import api.commands.Command;
import api.commands.CommandCache;
import api.database.daos.AliasDAO;
import api.database.daos.BotInstanceSettingsDAO;
import api.database.models.Alias;
import api.database.models.BotInstanceSettings;
import api.events.bot.AliasRemovedEvent;
import api.events.bot.AliasUpdateEvent;
import api.events.bot.CommandCalledEvent;
import api.events.bot.NonCommandEvent;
import api.events.irc.IRCMessageEvent;
import api.runtime.ContextFactory;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import spi.events.EventListener;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.settings.PropertiesConfig.COMMANDS_PREFIX;
import static api.tools.text.StringUtil.splitOnEndOfLine;

/**
 * A class that's responsible for listening for IRC messages and parsing them
 */
@ParametersAreNonnullByDefault
final class IRCInputParser implements EventListener {
    private static final Pattern PLACE_HOLDER = Pattern.compile("\\{(\\d+)\\}");

    private final PropertiesCollection properties;
    private final CommandCache commandCache;
    private final EventBus eventBus;
    private final ContextFactory contextFactory;

    private final Set<String> botNicks = new HashSet<>();
    private final AliasHandler aliasHandler;

    @Inject
    IRCInputParser(final PropertiesCollection properties, final EventBus eventBus, final CommandCache commandCache,
                   final ContextFactory contextFactory, final Provider<AliasDAO> aliasDAOProvider) {
        this.properties = properties;
        this.commandCache = commandCache;
        this.eventBus = eventBus;
        this.contextFactory = contextFactory;
        this.aliasHandler = new AliasHandler(aliasDAOProvider);
    }

    @Inject
    public void init(final BotInstanceSettingsDAO instanceSettingsDAO) {
        for (BotInstanceSettings settings : instanceSettingsDAO.getAll()) {
            botNicks.add(settings.getNick());
        }
        aliasHandler.init();
    }

    @Subscribe
    public void onAliasUpdateEvent(final AliasUpdateEvent event) {
        aliasHandler.registerUpdate(event.getAliasId());
    }

    @Subscribe
    public void onAliasRemovedEvent(final AliasRemovedEvent event) {
        aliasHandler.registerRemoval(event.getAliasId());
    }

    @Subscribe
    public void onIRCMessage(final IRCMessageEvent event) {
        if (botNicks.contains(event.getSender())) return;

        Object[] parsedEvents = parse(event);
        if (parsedEvents != null) {
            for (Object parsedEvent : parsedEvents) {
                if (parsedEvent != null) eventBus.post(parsedEvent);
            }
        }
    }

    private Object[] parse(final IRCMessageEvent event) {
        String[] inputs = transform(event.getMessage());
        Object[] out = new Object[inputs.length];
        for (int i = 0; i < out.length; i++) {
            String input = inputs[i];

            String commandPrefix = properties.get(COMMANDS_PREFIX);
            int prefixOccurrences = countCommandPrefixesAtStartOfString(input, commandPrefix);

            String command = prefixOccurrences > 0 ? extractCommandName(input, commandPrefix, prefixOccurrences) : null;
            if (command != null) {
                Command actualCommand = commandCache.getCommandFromName(command);
                if (actualCommand != null) {
                    IRCContext context = contextFactory.newIRCContext(event, input, actualCommand, prefixOccurrences);
                    if (context == null) return null;
                    out[i] = new CommandCalledEvent(actualCommand, context);
                    continue;
                }
            }

            IRCContext context = contextFactory.newIRCContext(event, input, null, 0);
            if (context == null) return null;
            out[i] = new NonCommandEvent(context);
        }
        return out;
    }

    private String[] transform(final String input) {
        return aliasHandler.withAliases(new Transformer<String[]>() {
            @Override
            public String[] transform(final Map<Pattern, String> aliases) {
                Matcher matcher;
                for (Map.Entry<Pattern, String> entry : aliases.entrySet()) {
                    matcher = entry.getKey().matcher(input);
                    if (matcher.matches()) {
                        String transformed = entry.getValue();
                        Matcher placeHolderMatcher = PLACE_HOLDER.matcher(transformed);
                        StringBuffer buffer = new StringBuffer(100);
                        while (placeHolderMatcher.find()) {
                            int number = Integer.parseInt(placeHolderMatcher.group(1));
                            if (number <= matcher.groupCount()) {
                                String replacement = matcher.group(number);
                                placeHolderMatcher.appendReplacement(buffer, replacement == null ? "" : replacement);
                            }
                        }
                        placeHolderMatcher.appendTail(buffer);
                        return splitOnEndOfLine(buffer.toString());
                    }
                }
                return new String[]{input};
            }
        });
    }

    private static int countCommandPrefixesAtStartOfString(final String possibleCommand, final String prefix) {
        int prefixLength = prefix.length();
        if (possibleCommand.length() < prefixLength) return 0;

        int occurrences = 0;
        for (int i = 0; i < possibleCommand.length(); i = i + prefixLength) {
            String substring = possibleCommand.substring(i, i + prefixLength);
            if (substring.equals(prefix)) ++occurrences;
            else break;
        }
        return occurrences;
    }

    private static String extractCommandName(final String fullCommand, final String prefix, final int prefixOccurrences) {
        String commandWithoutPrefix = fullCommand.substring(prefix.length() * prefixOccurrences);
        return commandWithoutPrefix.indexOf(' ') == -1 ? commandWithoutPrefix : commandWithoutPrefix.substring(0, commandWithoutPrefix.indexOf(' '));
    }

    private static class AliasHandler {
        private final Provider<AliasDAO> aliasDAOProvider;
        private final Map<Pattern, String> patternMap = new HashMap<>();
        private final Map<Long, Pattern> aliasMap = new HashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

        AliasHandler(final Provider<AliasDAO> aliasDAOProvider) {
            this.aliasDAOProvider = aliasDAOProvider;
        }

        void init() {
            lock.writeLock().lock();
            try {
                patternMap.clear();
                aliasMap.clear();
                for (Alias alias : aliasDAOProvider.get().getAllAliases()) {
                    addAlias(alias);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        //ONLY CALL WITH LOCK HELD!!!
        private void addAlias(final Alias alias) {
            Pattern pattern = Pattern.compile(alias.getAlias(), Pattern.CASE_INSENSITIVE);
            patternMap.put(pattern, alias.getTransform());
            aliasMap.put(alias.getId(), pattern);
        }

        //ONLY CALL WITH LOCK HELD!!!
        private void removeAlias(final long aliasId) {
            Pattern remove = aliasMap.remove(aliasId);
            if (remove != null) {
                patternMap.remove(remove);
            }
        }

        void registerUpdate(final long aliasId) {
            Alias alias = aliasDAOProvider.get().getAlias(aliasId);
            if (alias == null) return;
            lock.writeLock().lock();
            try {
                removeAlias(aliasId);
                addAlias(alias);
            } finally {
                lock.writeLock().unlock();
            }
        }

        void registerRemoval(final long aliasId) {
            lock.writeLock().lock();
            try {
                removeAlias(aliasId);
            } finally {
                lock.writeLock().unlock();
            }
        }

        <E> E withAliases(final Transformer<E> action) {
            lock.readLock().lock();
            try {
                return action.transform(patternMap);
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    private interface Transformer<E> {
        E transform(Map<Pattern, String> aliases);
    }
}
