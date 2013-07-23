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

package api.tools.communication;

import api.settings.PropertiesCollection;
import api.settings.PropertiesConfig;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import spi.runtime.RequiresShutdown;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static api.tools.text.StringUtil.isNullOrEmpty;
import static api.tools.text.StringUtil.lowerCase;
import static org.jivesoftware.smack.util.StringUtils.parseBareAddress;

/**
 * A class that handles sending messages via Jabber (aka XMPP)
 */
@ParametersAreNonnullByDefault
public class JabberClient implements RequiresShutdown {
    private final ConnectionConfiguration configuration;
    private final String username;
    private final String password;
    private final boolean enabled;

    private final Set<String> connectedUsers = new HashSet<>();

    private Connection connection;

    @Inject
    public JabberClient(final PropertiesCollection properties) {
        configuration = new ConnectionConfiguration(properties.get(PropertiesConfig.GTALK_HOST),
                properties.getInteger(PropertiesConfig.GTALK_PORT),
                properties.get(PropertiesConfig.GTALK_SERVICE_NAME));
        this.username = properties.get(PropertiesConfig.GTALK_USERNAME);
        this.password = properties.get(PropertiesConfig.GTALK_PASSWORD);
        this.enabled = properties.getBoolean(PropertiesConfig.GTALK_ENABLED);
    }

    @Inject
    public void init() throws XMPPException {
        if (enabled) {
            connect();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void connect() throws XMPPException {
        if (isNullOrEmpty(username) || isNullOrEmpty(password))
            throw new XMPPException("Configuration of GTalk is incomplete");
        connection = new XMPPConnection(configuration);
        connection.connect();

        Roster roster = connection.getRoster();
        roster.addRosterListener(new OnlineUsersListener(roster, connectedUsers));

        connection.login(username, password);
        connection.sendPacket(new Presence(Presence.Type.available));

        roster = connection.getRoster();
        for (RosterEntry rosterEntry : roster.getEntries()) {
            String user = rosterEntry.getUser();
            Presence presence = roster.getPresence(user);
            if (presence != null && presence.isAvailable()) connectedUsers.add(user);
        }
    }

    public synchronized boolean sendGTalkMessage(final String userAddress, final String message) throws XMPPException {
        if (!enabled) throw new IllegalStateException("GTalk is not enabled");
        if (!connectedUsers.contains(userAddress)) return false;
        try {
            return sendMessage(userAddress, message);
        } catch (Exception e) {
            throw new XMPPException(e);
        }
    }

    private boolean sendMessage(final String userAddress, final String message) {
        Message toSend = new Message(userAddress, Message.Type.chat);
        toSend.setBody(message);
        connection.sendPacket(toSend);
        return true;
    }

    @Override
    public Runnable getShutdownRunner() {
        return new Runnable() {
            @Override
            public void run() {
                disconnect();
            }
        };
    }

    private void disconnect() {
        if (connection != null) connection.disconnect();
        connection = null;
    }

    private static class OnlineUsersListener implements RosterListener {
        private final Roster roster;
        private final Set<String> connectedUsers;

        private OnlineUsersListener(final Roster roster, final Set<String> connectedUsers) {
            this.roster = roster;
            this.connectedUsers = connectedUsers;
        }

        @Override
        public void entriesAdded(final Collection<String> addresses) {
            for (String address : addresses) {
                Presence presence = roster.getPresence(address);
                if (presence != null && presence.isAvailable()) connectedUsers.add(lowerCase(address));
            }
        }

        @Override
        public void entriesUpdated(final Collection<String> addresses) {
        }

        @Override
        public void entriesDeleted(final Collection<String> addresses) {
            for (String address : addresses) {
                connectedUsers.remove(lowerCase(address));
            }
        }

        @Override
        public void presenceChanged(final Presence presence) {
            String user = lowerCase(parseBareAddress(presence.getFrom()));
            Presence bestPresence = roster.getPresence(user);
            if (bestPresence == null || !bestPresence.isAvailable()) connectedUsers.remove(user);
            else connectedUsers.add(user);
        }
    }
}
