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

package api.irc.entities;

import api.database.models.Channel;
import api.database.models.ChannelType;
import api.irc.BotIRCInstance;
import api.irc.communication.IRCMessageType;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import static api.tools.text.StringUtil.lowerCase;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a channel on IRC, and contains information about it
 */
@ParametersAreNonnullByDefault
public final class IRCChannel implements IRCEntity {
    private final Channel channel;
    private final ConcurrentMap<IRCUser, SortedSet<IRCUserOpType>> users = new ConcurrentHashMap<>();
    private final AtomicReference<BotIRCInstance> mainInstance = new AtomicReference<>();
    private final Set<BotIRCInstance> botInstances = Collections.synchronizedSet(new HashSet<BotIRCInstance>());

    /**
     * The topic of this channel
     */
    @Setter
    @Getter
    private volatile String topic;

    /**
     * @param channel      the database info object about this channel
     * @param mainInstance the main instance of this channel
     */
    public IRCChannel(final Channel channel, final BotIRCInstance mainInstance) {
        this.channel = checkNotNull(channel);
        this.mainInstance.set(checkNotNull(mainInstance));
    }

    /**
     * @return the bot instance which is the "main" instance in this channel
     */
    public BotIRCInstance getMainBotInstance() {
        return mainInstance.get();
    }

    public void addBotInstance(final BotIRCInstance instance) {
        botInstances.add(checkNotNull(instance));
    }

    /**
     * Removes the specified instance from this channel (if it exists). If the instance happens
     * to be the main instance for this channel, a new one is set (if there are others to choose from) randomly.
     *
     * @param instance the instance to remove
     * @return true unless the list of instances connected to this channel is empty as a result of this call
     */
    public boolean removeBotInstance(final BotIRCInstance instance) {
        botInstances.remove(instance);
        if (instance == mainInstance.get()) {
            if (!botInstances.isEmpty()) {
                mainInstance.get().removeAsMainInstanceInChannel(lowerCase(channel.getName()));
                BotIRCInstance next = botInstances.iterator().next();
                mainInstance.set(next);
                next.setAsMainInstanceInChannel(channel.getName());
            }
        }
        return !botInstances.isEmpty();
    }

    /**
     * @return the type of channel this is
     */
    public ChannelType getType() {
        return channel.getType();
    }

    /**
     * Adds the specified user to this channel
     *
     * @param user the user to add
     */
    public void addUser(final IRCUser user) {
        users.putIfAbsent(checkNotNull(user), new ConcurrentSkipListSet<IRCUserOpType>());
    }

    /**
     * Adds the specified user to this channel
     *
     * @param user the user to add
     * @param ops  the user ops
     */
    public void addUser(final IRCUser user, final Set<IRCUserOpType> ops) {
        ConcurrentSkipListSet<IRCUserOpType> ircUserOpTypes = new ConcurrentSkipListSet<>(ops);
        users.putIfAbsent(checkNotNull(user), ircUserOpTypes);
    }

    /**
     * @return a Collection of the users currently in the channel
     */
    public Collection<IRCUser> getUsers() {
        return new HashSet<>(users.keySet());
    }

    /**
     * @param mainNick the main nick of the user
     * @return a Set of all the connections the user has to this channel
     */
    public Set<IRCUser> getUsersFromMainnick(final String mainNick) {
        Set<IRCUser> out = new HashSet<>();
        for (IRCUser ircUser : users.keySet()) {
            if (mainNick.equals(ircUser.getMainNick())) out.add(ircUser);
        }
        return out;
    }

    /**
     * @param user the user
     * @return true if the specified user is in this channel
     */
    public boolean hasUser(final IRCUser user) {
        return users.containsKey(user);
    }

    /**
     * Removes the specified user from this channel
     *
     * @param user the user to remove
     */
    public void removeUser(final IRCUser user) {
        users.remove(user);
    }

    /**
     * @param user the user
     * @return a SortedSet of the ops the specified user has in this channel, or null if the user isn't in the channel at all
     */
    public SortedSet<IRCUserOpType> getUserOps(final IRCUser user) {
        SortedSet<IRCUserOpType> ircUserOpTypes = users.get(user);
        return ircUserOpTypes == null ? null : Collections.unmodifiableSortedSet(ircUserOpTypes);
    }

    /**
     * Adds an op mode to the user
     *
     * @param user the user
     * @param ops  the op mode to add
     */
    public void addUserOp(final IRCUser user, final IRCUserOpType ops) {
        SortedSet<IRCUserOpType> ircUserOpTypes = users.get(user);
        if (ircUserOpTypes != null) ircUserOpTypes.add(ops);
    }

    /**
     * @param user the user
     * @return the "highest" op mode the user has
     */
    public IRCUserOpType getHighestOp(final IRCUser user) {
        SortedSet<IRCUserOpType> ircUserOpTypes = users.get(user);
        return ircUserOpTypes == null || ircUserOpTypes.isEmpty() ? null : ircUserOpTypes.last();
    }

    /**
     * Removes the specified op mode from the user
     *
     * @param user the user
     * @param ops  the op mode to remove
     */
    public void removeUserOp(final IRCUser user, final IRCUserOpType ops) {
        SortedSet<IRCUserOpType> ircUserOpTypes = users.get(user);
        if (ircUserOpTypes != null) ircUserOpTypes.remove(ops);
    }

    @Override
    public boolean requiresOutputBlocking(final IRCMessageType type) {
        return true;
    }

    @Override
    public String getName() {
        return channel.getName();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this || obj instanceof IRCChannel && getName().equalsIgnoreCase(((IRCEntity) obj).getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
