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

import api.irc.BotIRCInstance;
import api.irc.OutputQueue;
import api.irc.entities.IRCChannel;
import api.irc.entities.IRCEntity;
import api.irc.entities.IRCUser;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import internal.irc.communication.ChanServCommands;
import internal.irc.communication.IrcCommands;
import internal.irc.communication.NickServCommands;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;

import static api.settings.PropertiesConfig.IRC_CS_INVITE_REQUEST;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class used for sending commands and messages to IRC
 */
@ParametersAreNonnullByDefault
public final class IRCAccess {
    private final OutputQueue outputQueue;
    private final PropertiesCollection properties;

    @Inject
    public IRCAccess(final OutputQueue outputQueue, final PropertiesCollection properties) {
        this.properties = properties;
        this.outputQueue = checkNotNull(outputQueue);
    }

    public void enqueue(final IRCOutput output) {
        outputQueue.enqueueOutput(checkNotNull(output));
    }

    /**
     * Sends the specified message to the IRC server, optionally with the specified handler
     *
     * @param handler the bot instance that should send the message. May be null if it doesn't matter who sends it
     * @param message the message to send
     */
    public void send(@Nullable final BotIRCInstance handler, final IRCMessage message) {
        IRCOutput out = new IRCOutput(handler, message);
        enqueue(out);
    }

    /**
     * Sends the specified message either through a Notice or a PM, depending on the context. If it's a reply
     * to something someone wrote in a channel, a Notice is sent in reply. If it's a reply to something sent in
     * a PM it responds with a PM.
     *
     * @param ircContext the context of the original request
     * @param message    the message to send
     */
    public void sendNoticeOrPM(final IRCContext ircContext, final String message) {
        if (ircContext.getInputType() == IRCMessageType.MESSAGE) sendNotice(ircContext.getUser(), message);
        else sendPrivateMessage(ircContext.getUser(), message);
    }

    /**
     * Sends the specified message either through a Message or a PM, depending on the context. If it's a reply
     * to something someone wrote in a channel, a message to that channel is sent in reply. If it's a reply to something sent in
     * a PM it responds with a PM.
     *
     * @param ircContext the context of the original request
     * @param message    the message to send
     */
    public void sendMessageOrPM(final IRCContext ircContext, final String message) {
        if (ircContext.getInputType() == IRCMessageType.MESSAGE) sendMessage(ircContext.getChannel(), message);
        else sendPrivateMessage(ircContext.getUser(), message);
    }

    public void sendAction(final BotIRCInstance handler, final IRCEntity target, final String message) {
        send(handler, new IRCMessage(IRCMessageType.ACTION, target, 1, message, true));
    }

    public void sendAction(final IRCEntity target, final String message) {
        send(null, new IRCMessage(IRCMessageType.ACTION, target, 1, message, false));
    }

    public void sendCTCPCommand(final BotIRCInstance handler, final IRCEntity target, final String command) {
        send(handler, new IRCMessage(IRCMessageType.CTCP, target, 1, command, true));
    }

    public void sendCTCPCommand(final IRCEntity target, final String command) {
        send(null, new IRCMessage(IRCMessageType.CTCP, target, 1, command, true));
    }

    public void sendMessage(final BotIRCInstance handler, final IRCChannel channel, final String message) {
        send(handler, new IRCMessage(IRCMessageType.MESSAGE, channel, 1, message, true));
    }

    public void sendMessage(final IRCChannel channel, final String message) {
        send(null, new IRCMessage(IRCMessageType.MESSAGE, channel, 1, message, false));
    }

    public void sendPrivateMessage(final BotIRCInstance handler, final IRCUser target, final String message) {
        send(handler, new IRCMessage(IRCMessageType.PRIVATE_MESSAGE, target, 1, message, true));
    }

    public void sendPrivateMessage(final IRCUser target, final String message) {
        send(null, new IRCMessage(IRCMessageType.PRIVATE_MESSAGE, target, 1, message, false));
    }

    public void sendNotice(final BotIRCInstance handler, final IRCEntity target, final String message) {
        send(handler, new IRCMessage(IRCMessageType.NOTICE, target, 1, message, true));
    }

    public void sendNotice(final IRCEntity target, final String message) {
        send(null, new IRCMessage(IRCMessageType.NOTICE, target, 1, message, false));
    }

    public void pingServer(final BotIRCInstance instance) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.PingCommand.format(String.valueOf(System.currentTimeMillis() / 1000)), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void invite(final BotIRCInstance instance, final String nick, final String channel) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.InviteCommand.format(nick, channel), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void join(final BotIRCInstance instance, final String channel, final String password) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.JoinCommand.format(channel, password), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void part(final BotIRCInstance instance, final String channel, final String reason) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.PartCommand.format(channel, reason), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void nick(final BotIRCInstance instance, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.NickCommand.format(nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void quitServer(final BotIRCInstance instance, final String reason) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.QuitCommand.format(reason), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void silence(final BotIRCInstance instance, final String details) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.SilenceCommand.format(details), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void topic(final BotIRCInstance instance, final String channel, final String topic) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.TopicCommand.format(channel, topic), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void whois(final BotIRCInstance instance, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.WhoIsCommand.format(nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void whowas(final BotIRCInstance instance, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.WhoWasCommand.format(nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void setMode(final BotIRCInstance instance, final String channel, final String modes, final String users) {
        IRCMessage ircMessage = new IRCMessage(1, IrcCommands.ModeCommand.format(channel, modes, users), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csAccess(final BotIRCInstance instance, final String channel, final String subCommand, final String... params) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.AccessCommand.format(channel, subCommand, params), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csAkick(final BotIRCInstance instance, final String channel, final String subCommand, final String... params) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.AKickCommand.format(channel, subCommand, params), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csAop(final BotIRCInstance instance, final String channel, final String subCommand, final String... params) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.AopCommand.format(channel, subCommand, params), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csClear(final BotIRCInstance instance, final String subCommand) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.ClearCommand.format(subCommand), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csDehalfop(final BotIRCInstance instance, final String channel, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.DehalfopCommand.format(channel, nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csDeop(final BotIRCInstance instance, final String channel, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.DeopCommand.format(channel, nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csDeprotect(final BotIRCInstance instance, final String channel, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.DeprotectCommand.format(channel, nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csDevoice(final BotIRCInstance instance, final String channel, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.DevoiceCommand.format(channel, nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csHalfop(final BotIRCInstance instance, final String channel, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.HalfopCommand.format(channel, nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csHop(final BotIRCInstance instance, final String channel, final String subCommand, final String... params) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.HopCommand.format(channel, subCommand, params), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csIdentify(final BotIRCInstance instance, final String channel, final String password) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.IdentifyCommand.format(channel, password), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csInvite(final BotIRCInstance instance, final String channel) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.InviteCommand.format(channel, properties.get(IRC_CS_INVITE_REQUEST)),
                                               true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csKick(final BotIRCInstance instance, final String channel, final String nick, final String reason) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.KickCommand.format(channel, nick, reason), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csOp(final BotIRCInstance instance, final String channel, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.OpCommand.format(channel, nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csProtect(final BotIRCInstance instance, final String channel, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.ProtectCommand.format(channel, nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csSop(final BotIRCInstance instance, final String channel, final String subCommand, final String... params) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.SopCommand.format(channel, subCommand, params), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csStatus(final BotIRCInstance instance, final String channel, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.StatusCommand.format(channel, nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csTopic(final BotIRCInstance instance, final String channel, final String topic) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.TopicCommand.format(channel, topic), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csUnban(final BotIRCInstance instance, final String channel) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.UnbanCommand.format(channel), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csVoice(final BotIRCInstance instance, final String channel, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.VoiceCommand.format(channel, nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void csVop(final BotIRCInstance instance, final String channel, final String subCommand, final String... params) {
        IRCMessage ircMessage = new IRCMessage(1, ChanServCommands.VopCommand.format(channel, subCommand, params), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void nsGhost(final BotIRCInstance instance, final String nick, final String password) {
        IRCMessage ircMessage = new IRCMessage(1, NickServCommands.GhostCommand.format(nick, password), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void nsIdentify(final BotIRCInstance instance, final String password) {
        IRCMessage ircMessage = new IRCMessage(1, NickServCommands.IdentifyCommand.format(password), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }

    public void nsInfo(final BotIRCInstance instance, final String nick) {
        IRCMessage ircMessage = new IRCMessage(1, NickServCommands.InfoCommand.format(nick), true);
        enqueue(new IRCOutput(instance, ircMessage));
    }
}
