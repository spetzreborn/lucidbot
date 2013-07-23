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

package internal.commands;

import api.commands.*;
import api.database.CallableTransactionTask;
import api.database.models.AccessLevel;
import api.database.models.ChannelType;
import api.events.DelayedEventPoster;
import api.events.bot.CommandCalledEvent;
import api.events.bot.LockEvent;
import api.events.bot.UnlockEvent;
import api.filters.FilterParser;
import api.irc.communication.IRCAccess;
import api.irc.communication.IRCMessageFactory;
import api.irc.communication.IRCMessageType;
import api.irc.communication.IRCOutput;
import api.irc.entities.IRCChannel;
import api.irc.entities.IRCEntity;
import api.irc.entities.IRCUser;
import api.runtime.IRCContext;
import api.runtime.ThreadingManager;
import api.settings.PropertiesConfig;
import api.templates.TemplateManager;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import lombok.extern.log4j.Log4j;
import spi.commands.CommandHandlerFactory;
import spi.events.EventListener;
import spi.filters.Filter;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;

import static api.database.Transactions.inTransaction;
import static api.tools.text.StringUtil.lowerCase;

/**
 * Responsible for handling all the supported commands of the bot
 */
@ParametersAreNonnullByDefault
@Log4j
final class CommandManager implements EventListener {
    private final ThreadingManager threadingManager;
    private final CommandCache commandCache;
    private final IRCAccess ircAccess;
    private final TemplateManager templateManager;
    private final FilterParser filterParser;
    private final IRCMessageFactory ircMessageFactory;
    private final String commandPrefix;

    private final ConcurrentMap<String, Boolean> lockedChannelsMap = new ConcurrentHashMap<>();


    @Inject
    CommandManager(final IRCAccess ircAccess,
                   final CommandCache commandCache,
                   final ThreadingManager threadingManager,
                   final TemplateManager templateManager,
                   final FilterParser filterParser,
                   final IRCMessageFactory ircMessageFactory,
                   @Named(PropertiesConfig.COMMANDS_PREFIX) final String commandPrefix) {
        this.ircAccess = ircAccess;
        this.commandCache = commandCache;
        this.threadingManager = threadingManager;
        this.templateManager = templateManager;
        this.filterParser = filterParser;
        this.ircMessageFactory = ircMessageFactory;
        this.commandPrefix = commandPrefix;
    }

    @Subscribe
    public void onCommandCalled(final CommandCalledEvent event) {
        final Command command = event.getCommand();
        final IRCContext context = event.getContext();
        threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Collection<IRCOutput> result = inTransaction(new CallableTransactionTask<Collection<IRCOutput>>() {
                        @Override
                        public Collection<IRCOutput> call(final DelayedEventPoster delayedEventPoster) throws Exception {
                            IRCChannel channel = context.getChannel();
                            if (isNonPublicCommandInPublicChannel(channel, command)) return null;

                            if (userHasAccess(context.getUser(), channel, command.getRequiredAccessLevel())) {
                                CommandHandlerFactory handlerFactory = commandCache.getFactoryForCommand(command);
                                return handleCommand(handlerFactory, context, templateManager, delayedEventPoster);
                            }
                            return null;
                        }
                    });
                    if (result != null && !result.isEmpty()) {
                        for (IRCOutput output : result) {
                            if (output.getHandler() == null && context.getInputType() == IRCMessageType.PRIVATE_MESSAGE)
                                output.setHandler(context.getReceiver());
                            ircAccess.enqueue(output);
                        }
                    }
                } catch (Exception e) {
                    CommandManager.log.error("Command could not be handled: " + command.getName(), e);
                }
            }
        });
    }

    private static boolean isNonPublicCommandInPublicChannel(final IRCChannel channel, final Command command) {
        return channel != null && channel.getType() == ChannelType.PUBLIC &&
                command.getRequiredAccessLevel() != AccessLevel.PUBLIC;
    }

    @Subscribe
    public void onLockEvent(final LockEvent event) {
        for (IRCChannel ircChannel : event.getChannels()) {
            lockedChannelsMap.put(lowerCase(ircChannel.getName()), true);
        }
    }

    @Subscribe
    public void onUnlockEvent(final UnlockEvent event) {
        for (IRCChannel ircChannel : event.getChannels()) {
            lockedChannelsMap.put(lowerCase(ircChannel.getName()), false);
        }
    }

    private boolean userHasAccess(final IRCUser user, final IRCChannel channel, final AccessLevel commandAccessLevel) {
        if (channel != null && lockedChannelsMap.containsKey(lowerCase(channel.getName())))
            return user.isAdmin();
        return commandAccessLevel.allows(user, channel);
    }

    private Collection<IRCOutput> handleCommand(final CommandHandlerFactory commandHandlerFactory,
                                                final IRCContext context,
                                                final TemplateManager templateManager,
                                                final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        String input = context.getInput().trim();
        Collection<Filter<?>> filters = new ArrayList<>();

        Matcher matcher = FilterParser.getInCommandFilterPattern().matcher(input);
        if (matcher.find()) {
            filters = filterParser.parseFilters(matcher.group(1));
            input = matcher.replaceFirst("").trim();
        }

        CommandResponse data = null;
        for (CommandParser parser : commandHandlerFactory.getParsers()) {
            if (parser.matches(input)) {
                data = commandHandlerFactory.getCommandHandler().handleCommand(context, parser.parse(input), filters, delayedEventPoster);
                break;
            }
        }
        if (data == null) data = CommandResponse
                .errorResponse("Syntax error. Check " + commandPrefix + "syntax " + commandHandlerFactory.getHandledCommand().getName());

        if (data.isEmpty() && data.getErrorMessage() == null) return Collections.emptyList();

        if (data.isError()) {
            IRCOutput output = getDefaultOutputForContext(
                    context.getInputType() == IRCMessageType.PRIVATE_MESSAGE ? context.getInputType() : IRCMessageType.NOTICE,
                    context.getUser(), data.getErrorMessage());
            return Lists.newArrayList(output);
        }

        data.put("channel", context.getChannel() == null ? "" : context.getChannel().getName());
        data.put("sender", context.getUser().getCurrentNick());
        return templateManager.createOutputFromTemplate(data.asMap(), context.getCommand().getTemplateFile(), context);
    }

    private IRCOutput getDefaultOutputForContext(final IRCMessageType messageType,
                                                 final IRCEntity target,
                                                 final String message) {
        return target == null ? null : new IRCOutput(ircMessageFactory.newIRCMessage(messageType, target, 5, false, message));
    }
}
