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

package api.irc.communication;

import api.common.HasName;
import api.irc.IRCEntityManager;
import api.irc.ValidationType;
import api.irc.entities.IRCEntity;
import api.runtime.IRCContext;
import api.tools.text.StringUtil;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.settings.PropertiesConfig.IRC_DEFAULT_PRIORITY;
import static api.tools.text.StringUtil.*;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory for creating IRCMessages
 */
@ParametersAreNonnullByDefault
public final class IRCMessageFactory {
    private static final Pattern MESSAGE_PATTERN;

    private enum SmartMessageType implements HasName {
        REPLY_NOTICE {
            @Override
            IRCMessageType inferMessageType(final IRCContext context) {
                return context.getInputType() == IRCMessageType.PRIVATE_MESSAGE ? IRCMessageType.PRIVATE_MESSAGE : IRCMessageType.NOTICE;
            }
        }, REPLY_MESSAGE {
            @Override
            IRCMessageType inferMessageType(final IRCContext context) {
                return context.getInputType() == IRCMessageType.PRIVATE_MESSAGE ? IRCMessageType.PRIVATE_MESSAGE : IRCMessageType.MESSAGE;
            }
        };

        static SmartMessageType fromName(final String name) {
            for (SmartMessageType type : values()) {
                if (type.name().equalsIgnoreCase(name)) return type;
            }
            return null;
        }

        @Override
        public String getName() {
            return name();
        }

        abstract IRCMessageType inferMessageType(final IRCContext context);
    }

    static {
        StringBuilder typeGroup = new StringBuilder(200);
        typeGroup.append("(?<type>");
        typeGroup.append(StringUtil.mergeNamed(IRCMessageType.values(), '|'));
        typeGroup.append('|');
        typeGroup.append(StringUtil.merge(IRCMessageType.nameValues(), '|'));
        typeGroup.append('|');
        typeGroup.append(StringUtil.mergeNamed(SmartMessageType.values(), '|'));
        typeGroup.append(')');
        StringBuilder params = new StringBuilder(100);
        params.append("\\s*\\(");
        params.append("(?:\\s*\\s*(?<priority>\\d+))");
        params.append("(?:\\s*,\\s*(?<target>");
        params.append(ValidationType.CHANNEL.getPattern()).append('|').append(ValidationType.NICKNAME.getPattern());
        params.append("))?");
        params.append("(?:\\s*,\\s*(?<return>true))?");
        params.append("\\)\\s*");
        StringBuilder message = new StringBuilder(540);
        message.append("<\\{");
        message.append("(?<message>(?:.(?!\\}>))+.)");
        message.append("\\}>");
        MESSAGE_PATTERN = Pattern.compile(typeGroup.append(params).append(message).toString(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }

    private final IRCEntityManager ircEntityManager;
    private final int defaultPriority;

    @Inject
    public IRCMessageFactory(final IRCEntityManager ircEntityManager, @Named(IRC_DEFAULT_PRIORITY) final int defaultPriority) {
        this.ircEntityManager = checkNotNull(ircEntityManager);
        this.defaultPriority = defaultPriority;
    }

    /**
     * Parses text and creates IRCMessage instances from what it finds. Mainly supposed to be used to parse from templates
     * <p/>
     * Accepted syntax:
     * <ul>
     * <li>MESSAGE(#somechannel, 3){send this to irc}</li>
     * <li>ACTION(#somechannel){says weeeeeeeeeeeeeeeee}</li>
     * <li>MESSAGE(#somechannel){some message to send goes here}</li>
     * <li>MESSAGE(someuser){this is the same as using PRIVATE_MESSAGE}</li>
     * <li>NOTICE(#someotherchannel, 42, true){send this to irc too}</li>
     * <li>PRIVATE_MESSAGE(someusersnick){
     * send this to irc too
     * and this, with line breaks retained (i.e. it becomes two messages)
     * }</li>
     * </ul>
     * <p/>
     * The parameters within () are:
     * <ol>
     * <li>target (channel or nick, required)</li>
     * <li>priority (1-100, optional)</li>
     * <li>return (true if the bot instance that got the message should also return it, false otherwise, optional)</li>
     * </ol>
     *
     * @param rawMessage the message to parseAndCreateNewMessages
     * @return a list of all IRCMessages that could be parsed from the specified string
     * @throws IllegalArgumentException if the rawMessage is not in a parsable format
     */
    public List<IRCMessage> parseAndCreateNewMessages(final String rawMessage, final IRCContext context) {
        Matcher matcher = MESSAGE_PATTERN.matcher(checkNotNull(rawMessage));

        List<IRCMessage> out = new ArrayList<>();
        while (matcher.find()) {
            int priority = isNullOrEmpty(matcher.group("priority")) ? defaultPriority : Integer.parseInt(matcher.group("priority"));
            List<IRCMessage> parsedMessages = newIRCMessages(matcher.group("type"), matcher.group("target"), context, priority,
                    isNotNullOrEmpty(matcher.group("return")), matcher.group("message"));
            out.addAll(parsedMessages);
        }

        if (out.isEmpty()) throw new IllegalArgumentException("String cannot be parsed: " + rawMessage);
        else return out;
    }

    private List<IRCMessage> newIRCMessages(final String type,
                                            final String target,
                                            final IRCContext context,
                                            final Integer priority,
                                            final boolean useReceivingHandler,
                                            final String message) {
        String[] split = splitOnEndOfLine(message);
        List<IRCMessage> out = new ArrayList<>(split.length);
        for (String line : split) {
            SmartMessageType smartMessageType = SmartMessageType.fromName(type);
            IRCMessageType messageType = smartMessageType == null ? IRCMessageType.fromName(type) : smartMessageType.inferMessageType(context);

            if (isNullOrEmpty(target)) {
                IRCEntity actualTarget = isMessageOrAction(messageType) ? context.getChannel() : context.getUser();
                out.add(newIRCMessage(messageType, actualTarget, priority, useReceivingHandler, limitedTrim(line)));
            } else out.add(newIRCMessage(messageType, target, priority, useReceivingHandler, limitedTrim(line)));
        }
        return out;
    }

    private static boolean isMessageOrAction(final IRCMessageType messageType) {
        return messageType == IRCMessageType.MESSAGE || messageType == IRCMessageType.ACTION;
    }

    private IRCMessage newIRCMessage(final IRCMessageType type,
                                     final String target,
                                     final Integer priority,
                                     final boolean useReceivingHandler,
                                     final String message) {
        IRCEntity messageTarget =
                ValidationType.CHANNEL.matches(target) ? ircEntityManager.getChannel(target) : ircEntityManager.getUser(target);
        return new IRCMessage(type, messageTarget, priority == null ? defaultPriority : priority, message, useReceivingHandler);
    }

    /**
     * Creates a new IRCMessage
     *
     * @param type                the type of message to be created
     * @param target              the target/recipient of the message
     * @param priority            the priority of the message, may be null
     * @param useReceivingHandler whether to deliver the message with the bot instance that received the command
     * @param message             the message to send
     * @return a new IRCMessage
     */
    public IRCMessage newIRCMessage(final IRCMessageType type,
                                    final IRCEntity target,
                                    @Nullable final Integer priority,
                                    final boolean useReceivingHandler,
                                    final String message) {
        return new IRCMessage(type, target, priority == null ? defaultPriority : priority, message, useReceivingHandler);
    }
}
