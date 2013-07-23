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

package api.irc;

import api.database.daos.BotUserDAO;
import api.database.daos.ChannelDAO;
import api.database.models.BotUser;
import api.database.models.Channel;
import api.events.bot.AdminStatusChangeEvent;
import api.events.bot.UserLoginEvent;
import api.events.bot.UserRemovedEvent;
import api.events.irc.*;
import api.irc.entities.IRCChannel;
import api.irc.entities.IRCUser;
import api.irc.entities.IRCUserOpType;
import api.runtime.ThreadingManager;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import internal.irc.Authenticator;
import spi.events.EventListener;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static api.tools.text.StringUtil.lowerCase;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A manager of information regarding IRC, such as users and channels
 */
@ParametersAreNonnullByDefault
public final class IRCEntityManager implements EventListener {
    private final Map<String, IRCUser> userMap = new HashMap<>();
    private final Map<String, IRCChannel> channelMap = new HashMap<>();
    private final ReadWriteLock mapsLock = new ReentrantReadWriteLock(true);
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<ChannelDAO> channelDAOProvider;
    private final Authenticator authenticator;
    private final ThreadingManager threadingManager;
    private final EventBus eventBus;
    private final ConcurrentMap<IRCUser, UserAuthenticationRequestSource> statusRequestSource = new ConcurrentHashMap<>();

    @Inject
    public IRCEntityManager(final Authenticator authenticator, final Provider<ChannelDAO> channelDAOProvider,
                            final Provider<BotUserDAO> botUserDAOProvider, final ThreadingManager threadingManager,
                            final EventBus eventBus) {
        this.authenticator = checkNotNull(authenticator);
        this.channelDAOProvider = checkNotNull(channelDAOProvider);
        this.botUserDAOProvider = checkNotNull(botUserDAOProvider);
        this.threadingManager = checkNotNull(threadingManager);
        this.eventBus = checkNotNull(eventBus);
    }

    @Subscribe
    public void onAdminStatusChanged(final AdminStatusChangeEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                BotUser user = botUserDAOProvider.get().getUser(event.getUserId());
                String mainNick = user.getMainNick();
                boolean isAdmin = event.isPromotion();
                mapsLock.readLock().lock();
                try {
                    for (IRCUser ircUser : userMap.values()) {
                        if (ircUser.isAuthenticated() && ircUser.getMainNick().equals(mainNick))
                            ircUser.setAdmin(isAdmin);
                    }
                } finally {
                    mapsLock.readLock().unlock();
                }
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onDisconnect(final DisconnectEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                removeBotFromChannels(event.getInstance(), Collections.<String>emptyList());
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onJoin(final JoinEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                addChannelIfAbsent(event.getReceiver(), event.getChannel());
                if (event.getUser() != null) {
                    mapsLock.readLock().lock();
                    try {
                        String lowerCaseNick = lowerCase(event.getUser());
                        IRCUser user = userMap.get(lowerCaseNick);
                        IRCChannel ircChannel = channelMap.get(lowerCase(event.getChannel()));
                        if (user == null) {
                            user = new IRCUser(event.getUser());
                            userMap.put(lowerCaseNick, user);
                            ircChannel.addUser(user);
                        } else if (!ircChannel.hasUser(user)) {
                            ircChannel.addUser(user);
                        }

                        if (!user.isAuthenticated() && !event.getUser().equals(event.getReceiver().getNick())) {
                            statusRequestSource.put(user, UserAuthenticationRequestSource.USER_JOIN);
                            authenticator.sendAuthenticationCheck(event.getUser());
                        }
                    } finally {
                        mapsLock.readLock().unlock();
                    }
                } else {
                    channelMap.get(lowerCase(event.getChannel())).addBotInstance(event.getReceiver());
                }
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onKick(final KickEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (event.getUser() == null)
                    removeBotFromChannels(event.getReceiver(), Arrays.asList(event.getChannel()));
                removeUserFromChannel(event.getUser(), event.getChannel());
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onNickChange(final NickChangeEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mapsLock.readLock().lock();
                try {
                    IRCUser ircUser = userMap.remove(lowerCase(event.getOldNick()));
                    ircUser.setCurrentNick(event.getNewNick());
                    userMap.put(lowerCase(event.getNewNick()), ircUser);

                    if (!ircUser.isAuthenticated()) {
                        authenticator.sendAuthenticationCheck(ircUser.getCurrentNick());
                    }
                } finally {
                    mapsLock.readLock().unlock();
                }
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onPart(final PartEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (event.getUser() == null)
                    removeBotFromChannels(event.getReceiver(), Arrays.asList(event.getChannel()));
                removeUserFromChannel(event.getUser(), event.getChannel());
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onQuit(final QuitEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mapsLock.readLock().lock();
                try {
                    IRCUser ircUser = userMap.remove(lowerCase(event.getUser()));
                    for (IRCChannel channel : channelMap.values()) {
                        channel.removeUser(ircUser);
                    }
                    statusRequestSource.remove(ircUser);
                } finally {
                    mapsLock.readLock().unlock();
                }
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onTopicChange(final TopicEvent event) {
        addChannelIfAbsent(event.getReceiver(), event.getChannel());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mapsLock.readLock().lock();
                try {
                    getChannel(event.getChannel()).setTopic(event.getTopic());
                } finally {
                    mapsLock.readLock().unlock();
                }
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onUserList(final UserListEvent event) {
        addChannelIfAbsent(event.getReceiver(), event.getChannel());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mapsLock.writeLock().lock();
                try {
                    List<String> toAuth = new ArrayList<>(event.getUserInfo().size());
                    for (Map.Entry<String, Set<IRCUserOpType>> entry : event.getUserInfo().entrySet()) {
                        String lowerCaseNick = lowerCase(entry.getKey());
                        IRCUser user = userMap.get(lowerCaseNick);
                        if (user == null) {
                            user = new IRCUser(entry.getKey());
                            userMap.put(lowerCaseNick, user);
                        }
                        IRCChannel ircChannel = channelMap.get(lowerCase(event.getChannel()));
                        ircChannel.addUser(user, entry.getValue());
                        if (!user.isAuthenticated()) toAuth.add(user.getCurrentNick());
                    }
                    if (!toAuth.isEmpty()) {
                        for (String nick : toAuth) {
                            IRCUser user = userMap.get(lowerCase(nick));
                            statusRequestSource.put(user, UserAuthenticationRequestSource.USER_LIST);
                        }
                        authenticator.sendAuthenticationCheck(toAuth);
                    }
                } finally {
                    mapsLock.writeLock().unlock();
                }
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onModeChange(final ModeEvent event) {
        if (event.getReceiver().isMainInstancein(event.getChannel())) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    boolean isAdd = event.getModes().startsWith("+");
                    char[] chars = event.getModes().substring(1).toCharArray();
                    List<UserOp> ops = new ArrayList<>();
                    for (int i = 0; i < chars.length && i < event.getRecipients().length; ++i) {
                        if (chars[i] == '+') {
                            isAdd = true;
                        } else if (chars[i] == '-') {
                            isAdd = false;
                        } else {
                            IRCUserOpType type = IRCUserOpType.get(chars[i]);
                            if (type != null) ops.add(new UserOp(isAdd, event.getRecipients()[i], type));
                        }
                    }

                    mapsLock.readLock().lock();
                    try {
                        IRCChannel ircChannel = channelMap.get(lowerCase(event.getChannel()));
                        for (UserOp op : ops) {
                            IRCUser ircUser = userMap.get(lowerCase(op.user));
                            if (op.isAdd) ircChannel.addUserOp(ircUser, op.type);
                            else ircChannel.removeUserOp(ircUser, op.type);

                            if (!ircUser.isAuthenticated()) {
                                statusRequestSource.put(ircUser, UserAuthenticationRequestSource.USER_MODE_CHANGE);
                                authenticator.sendAuthenticationCheck(ircUser.getCurrentNick());
                            }
                        }
                    } finally {
                        mapsLock.readLock().unlock();
                    }
                }
            };
            threadingManager.execute(runnable);
        }
    }

    @Subscribe
    public void onUserAuthentication(final UserAuthenticationEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                BotUser user = botUserDAOProvider.get().getUser(event.getBaseNick());
                if (user == null) return;
                mapsLock.readLock().lock();
                try {
                    IRCUser ircUser = userMap.get(lowerCase(event.getCurrentNick()));
                    boolean authenticated = ircUser.authenticate(user.getMainNick(), user.isAdmin());
                    if (authenticated) {
                        UserAuthenticationRequestSource requestSource = statusRequestSource.get(ircUser);
                        if (requestSource == null) requestSource = UserAuthenticationRequestSource.MANUAL_REQUEST;
                        eventBus.post(new UserLoginEvent(user.getId(), ircUser, requestSource, event.getReceiver()));
                    }
                } finally {
                    mapsLock.readLock().unlock();
                }
            }
        };
        threadingManager.execute(runnable);
    }

    @Subscribe
    public void onUserRemoved(final UserRemovedEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mapsLock.readLock().lock();
                try {
                    for (IRCUser ircUser : userMap.values()) {
                        if (ircUser.isAuthenticated() && ircUser.getMainNick().equals(event.getMainNick())) {
                            ircUser.deauthenticate();
                            statusRequestSource.remove(ircUser);
                        }
                    }
                } finally {
                    mapsLock.readLock().unlock();
                }
            }
        };
        threadingManager.execute(runnable);
    }

    private void addChannelIfAbsent(final BotIRCInstance botIRCInstance, final String channel) {
        mapsLock.writeLock().lock();
        try {
            if (!channelMap.containsKey(lowerCase(channel))) {
                Channel dbChannel = channelDAOProvider.get().getChannel(channel);
                channelMap.put(lowerCase(channel), new IRCChannel(dbChannel, botIRCInstance));
                botIRCInstance.setAsMainInstanceInChannel(channel);
            }
        } finally {
            mapsLock.writeLock().unlock();
        }
    }

    private void removeUserFromChannel(final String user, final String channel) {
        mapsLock.readLock().lock();
        try {
            String lowerCaseNick = lowerCase(user);
            IRCUser ircUser = userMap.get(lowerCaseNick);
            IRCChannel ircChannel = channelMap.get(lowerCase(channel));
            ircChannel.removeUser(ircUser);

            boolean isGone = true;
            for (IRCChannel chan : channelMap.values()) {
                if (chan.hasUser(ircUser)) {
                    isGone = false;
                }
            }
            if (isGone) {
                userMap.remove(lowerCaseNick);
                statusRequestSource.remove(ircUser);
            }
        } finally {
            mapsLock.readLock().unlock();
        }
    }

    private void removeBotFromChannels(final BotIRCInstance receiver, final Collection<String> channelNames) {
        mapsLock.writeLock().lock();
        try {
            if (channelNames.isEmpty()) channelNames.addAll(channelMap.keySet());
            boolean channelWasRemoved = false;
            for (String channel : channelNames) {
                IRCChannel ircChannel = channelMap.get(lowerCase(channel));
                boolean noBotLeftInChannel = ircChannel.removeBotInstance(receiver);
                if (noBotLeftInChannel) {
                    channelMap.remove(lowerCase(channel));
                    channelWasRemoved = true;
                }
            }

            if (channelWasRemoved) {
                for (IRCUser user : userMap.values()) {
                    boolean isInAnotherChannel = false;
                    for (IRCChannel chan : channelMap.values()) {
                        if (chan.hasUser(user)) {
                            isInAnotherChannel = true;
                            break;
                        }
                    }
                    if (!isInAnotherChannel) userMap.remove(lowerCase(user.getCurrentNick()));
                }
            }
        } finally {
            mapsLock.writeLock().unlock();
        }
    }

    private static class UserOp {
        private final boolean isAdd;
        private final IRCUserOpType type;
        private final String user;

        private UserOp(boolean add, String user, IRCUserOpType type) {
            isAdd = add;
            this.user = user;
            this.type = type;
        }
    }

    /**
     * @param mainNick the main nick of the user
     * @return a Collection of all the nicknames the specified user is currently connected with
     */
    public Collection<String> getAllOfUsersNicks(final String mainNick) {
        Collection<String> out = new ArrayList<>();
        mapsLock.readLock().lock();
        try {
            for (IRCUser ircUser : userMap.values()) {
                if (mainNick.equals(ircUser.getMainNick())) out.add(ircUser.getCurrentNick());
            }
        } finally {
            mapsLock.readLock().unlock();
        }
        return out;
    }

    /**
     * @param mainNick the main nick of the user
     * @return a Collection of all the user's IRCUser instances
     */
    public Collection<IRCUser> getAllOfUsersConnections(final String mainNick) {
        Collection<IRCUser> out = new HashSet<>();
        mapsLock.readLock().lock();
        try {
            for (IRCUser ircUser : userMap.values()) {
                if (mainNick.equals(ircUser.getMainNick())) out.add(ircUser);
            }
        } finally {
            mapsLock.readLock().unlock();
        }
        return out;
    }

    /**
     * @param channel the name of the channel
     * @return the IRCChannel with the specified name, or null if none exists
     */
    public IRCChannel getChannel(final String channel) {
        mapsLock.readLock().lock();
        try {
            return channelMap.get(lowerCase(channel));
        } finally {
            mapsLock.readLock().unlock();
        }
    }

    /**
     * @return a Collection of all the channels collectively known to all of the bot instances
     */
    public Collection<IRCChannel> getChannels() {
        mapsLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(channelMap.values());
        } finally {
            mapsLock.readLock().unlock();
        }
    }

    /**
     * @param nick    the nickname of the user
     * @param channel the channel to look for the user in
     * @return the IRCUser with the specified nick, if he/she is actually in the specified channel, otherwise null
     */
    public IRCUser getUser(final String nick, final String channel) {
        mapsLock.readLock().lock();
        try {
            IRCUser ircUser = userMap.get(lowerCase(nick));
            if (ircUser != null && getChannel(channel).hasUser(ircUser)) return ircUser;
        } finally {
            mapsLock.readLock().unlock();
        }
        return null;
    }

    /**
     * {@link #getUser(String, String)} but looks in all channels instead
     *
     * @param nick the nickname of the user
     * @return the IRCUser with the specified nick, or null if no user is found
     */
    public IRCUser getUser(final String nick) {
        mapsLock.readLock().lock();
        try {
            return userMap.get(lowerCase(nick));
        } finally {
            mapsLock.readLock().unlock();
        }
    }

    /**
     * @param nick the nickname of the user
     * @return true if a user with that nick is currently online
     */
    public boolean userIsOnline(final String nick) {
        mapsLock.readLock().lock();
        try {
            return userMap.containsKey(lowerCase(nick));
        } finally {
            mapsLock.readLock().unlock();
        }
    }

    /**
     * @param mainNick the main nick of the user
     * @return true if a user with the specified main nick is currently online (will be found even if the user is connected with some
     *         other nick at the moment)
     */
    public boolean userIsOnlineMainNick(final String mainNick) {
        mapsLock.readLock().lock();
        try {
            for (IRCUser ircUser : userMap.values()) {
                if (mainNick.equals(ircUser.getMainNick())) return true;
            }
            return false;
        } finally {
            mapsLock.readLock().unlock();
        }
    }
}
