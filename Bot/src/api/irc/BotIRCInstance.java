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

import api.database.models.BotInstanceSettings;
import api.database.models.Channel;
import api.events.irc.*;
import api.runtime.ThreadingManager;
import api.settings.PropertiesCollection;
import api.tools.common.CleanupUtil;
import com.google.common.base.Charsets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import internal.irc.InputThread;
import internal.irc.OutputThread;
import internal.irc.ReconnectScheduler;
import internal.irc.communication.*;
import internal.irc.delays.DelayHandler;
import internal.main.Main;
import lombok.extern.log4j.Log4j;
import org.apache.log4j.Logger;
import spi.runtime.RequiresShutdown;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.settings.PropertiesConfig.*;
import static api.tools.text.StringUtil.isNotNullOrEmpty;
import static api.tools.text.StringUtil.lowerCase;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A bot instance, which is an entity that's capable of connecting to and interacting with IRC
 */
@Log4j
@ParametersAreNonnullByDefault
public final class BotIRCInstance implements RequiresShutdown {
    private static final Pattern PING_PATTERN = Pattern.compile("PING :(.*?)");

    private volatile Logger logger;
    private volatile BotInstanceSettings settings;
    private volatile String nick;

    private final PropertiesCollection properties;
    private final ThreadingManager threadingManager;
    private final OutputQueue outputQueue;
    private final EventBus eventBus;
    private final ServerCommandCommunication serverCommandCommunication;
    private final ServerCodedCommunication serverCodedCommunication;
    private final ServerErrorCommunication serverErrorCommunication;
    private final DelayHandler delayHandler;
    private final ReconnectScheduler reconnectScheduler;

    private boolean doNotAttemptReconnect;

    private InputThread inputThread;
    private OutputThread outputThread;
    private Socket socket;

    private final Set<String> mainInstanceIn = Collections.synchronizedSet(new HashSet<String>());
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicBoolean identified = new AtomicBoolean();
    private final AtomicInteger reconnectAttempts = new AtomicInteger();

    public BotIRCInstance(final ServerErrorCommunication serverErrorCommunication,
                          final ServerCodedCommunication serverCodedCommunication,
                          final ServerCommandCommunication serverCommandCommunication,
                          final EventBus eventBus,
                          final OutputQueue outputQueue,
                          final ThreadingManager threadingManager,
                          final PropertiesCollection properties,
                          final DelayHandler delayHandler,
                          final ReconnectScheduler reconnectScheduler) {
        this.delayHandler = delayHandler;
        this.reconnectScheduler = checkNotNull(reconnectScheduler);
        this.serverErrorCommunication = checkNotNull(serverErrorCommunication);
        this.serverCodedCommunication = checkNotNull(serverCodedCommunication);
        this.serverCommandCommunication = checkNotNull(serverCommandCommunication);
        this.eventBus = checkNotNull(eventBus);
        this.outputQueue = checkNotNull(outputQueue);
        this.threadingManager = checkNotNull(threadingManager);
        this.properties = checkNotNull(properties);
    }

    public void setSettings(final BotInstanceSettings settings) {
        this.settings = settings;
        this.nick = settings.getNick();
        this.logger = Logger.getLogger(lowerCase(nick));
    }

    public Logger getInstanceLogger() {
        return logger;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isIdentified() {
        return identified.get();
    }

    public void setIdentified(final boolean isIndentified) {
        identified.set(isIndentified);
    }

    /**
     * Sets this instance as the "main" instance in the specified channel. The channel name is lower cased before saved
     *
     * @param channel the channel
     */
    public void setAsMainInstanceInChannel(final String channel) {
        mainInstanceIn.add(checkNotNull(lowerCase(channel)));
    }

    /**
     * Removes this instance as the "main" instance in the specified channel
     *
     * @param channel the channel
     */
    public void removeAsMainInstanceInChannel(final String channel) {
        mainInstanceIn.remove(checkNotNull(lowerCase(channel)));
    }

    /**
     * @param channel the channel
     * @return true if this bot instance is the "main" instance in the specified channel
     */
    public boolean isMainInstancein(final String channel) {
        return mainInstanceIn.contains(lowerCase(channel));
    }

    /**
     * Connects this instance to IRC
     *
     * @throws IOException .
     */
    public synchronized void connect() throws IOException {
        doNotAttemptReconnect = false;
        dispose();

        String hostname = properties.get(IRC_SERVER);
        int port = properties.getInteger(IRC_PORT);
        String serverPassword = properties.get(IRC_SERVER_PASSWORD);

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            socket = new Socket(hostname, port);
            socket.setSoTimeout(300_000);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8));

            inputThread = new InputThread(this, reader, eventBus);
            outputThread = new OutputThread(getNick(), writer, properties.getInteger(IRC_MAX_LENGTH), delayHandler);

            if (isNotNullOrEmpty(serverPassword)) {
                sendAndLogCommand(IrcCommands.PassCommand.format(serverPassword));
            }
            sendAndLogCommand(IrcCommands.NickCommand.format(nick));
            sendAndLogCommand(IrcCommands.UserCommand.format("LucidBot 8 * Lucidbot " + Main.VERSION));

            String line;
            int tries = 1;
            while ((line = reader.readLine()) != null) {
                int firstSpace = line.indexOf(' ');
                int secondSpace = firstSpace > 0 && firstSpace < line.length() - 1 ? line.indexOf(' ', firstSpace + 1) : -1;
                if (secondSpace != -1) {
                    String codeString = line.substring(firstSpace + 1, secondSpace);
                    int code = ValidationType.INT.matches(codeString) ? Integer.parseInt(codeString) : -1;

                    if (ServerCommand.NICK.isCommand(line)) {
                        Matcher matcher = ServerCommand.NICK.getPattern().matcher(line);
                        if (matcher.matches() && !matcher.group("target").equals(nick)) ghostTimedOutInstance();
                    } else if (ServerCode.MYINFO.hasCode(code)) {
                        sendAndLogCommand(NickServCommands.IdentifyCommand.format(settings.getPassword()));
                        break;
                    } else if (ServerError.NICKNAMEINUSE.hasCode(code)) {
                        sendAndLogCommand(IrcCommands.NickCommand.format(nick + tries));
                        ++tries;
                    } else if (ServerError.getFromCode(code) != null) {
                        throw new IOException("Could not log into the IRC server: " + line);
                    } else {
                        handleLine(line);
                    }
                } else {
                    Matcher matcher = PING_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        logger.info(line);
                        sendAndLogCommand(IrcCommands.PongCommand.format(matcher.group(1)));
                    }
                }
            }

            sendAndLogCommand("MODE " + getNick() + " +RB");

            inputThread.start();
            outputThread.start();
            outputThread.attachToMainQueue(this, outputQueue, threadingManager);
            connected.set(true);
            reconnectAttempts.set(0);
        } catch (UnknownHostException | SocketException | NumberFormatException e) {
            CleanupUtil.closeSilently(reader);
            CleanupUtil.closeSilently(writer);
            dispose();
            if (e instanceof SocketException && socket != null) {
                InetSocketAddress socketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
                String message = socketAddress.isUnresolved() ? "URL could not be resolved: " + hostname
                        : "Failed to open socket >> Host: " + socketAddress.getHostString() +
                        " Port: " + socketAddress.getPort();
                throw new IOException(message, e);
            } else throw e;
        }
    }

    private void sendAndLogCommand(final String command) throws IOException {
        outputThread.sendRawLine(command);
        logger.info(command);
    }

    private void ghostTimedOutInstance() throws IOException {
        sendAndLogCommand(NickServCommands.GhostCommand.format(nick, settings.getPassword()));
        try {
            Thread.sleep(3000);
        } catch (final InterruptedException e) {
            log.error("", e);
        }
        sendAndLogCommand(IrcCommands.NickCommand.format(nick));
        sendAndLogCommand(NickServCommands.IdentifyCommand.format(settings.getPassword()));
    }

    /**
     * Shuts down this bot instance and disposes of the threads and socket
     */
    public synchronized void dispose() {
        CleanupUtil.closeSilently(socket);
        if (outputThread != null) {
            outputThread.kill();
            outputThread = null;
        }
        if (inputThread != null) {
            inputThread.kill();
            inputThread = null;
        }
        connected.set(false);
        identified.set(false);
    }

    /**
     * Sends a quit command to the server
     */
    public void sendQuit() {
        try {
            if (outputThread != null && !outputThread.isInterrupted())
                sendAndLogCommand(IrcCommands.QuitCommand.format("Quitting"));
        } catch (IOException ignore) {
        }
    }

    /**
     * @return this bot instance's nickname on IRC
     */
    public String getNick() {
        return nick;
    }

    /**
     * Handles a raw line from the irc server
     *
     * @param line the message from the server
     * @throws IOException if the bot needs to respond to the message (for example server ping) but it fails
     */
    public void handleLine(final String line) throws IOException {
        Matcher matcher = PING_PATTERN.matcher(checkNotNull(line));
        if (matcher.matches()) {
            logger.info(line);
            sendAndLogCommand(IrcCommands.PongCommand.format(matcher.group(1)));
            return;
        }
        if (!(serverCommandCommunication.parseAndHandle(this, line) ||
                serverCodedCommunication.parseAndHandle(this, line) ||
                serverErrorCommunication.parseAndHandle(this, line))) {
            log.info("Encountered unknown command: " + line);
        }
    }

    /**
     * Pings the server
     *
     * @throws RuntimeException if it fails, which is not expected
     */
    public void pingServer() {
        try {
            sendAndLogCommand(IrcCommands.PingCommand.format(String.valueOf(System.currentTimeMillis() / 1000)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to even ping server", e);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof BotIRCInstance)) return false;

        BotIRCInstance that = (BotIRCInstance) o;
        return that.getNick().equals(getNick());
    }

    @Override
    public int hashCode() {
        return nick.hashCode();
    }

    @Subscribe
    public void onPing(final PingEvent event) {
        try {
            if (event.getReceiver() == this)
                sendAndLogCommand(IrcCommands.NoticeCommand.format(event.getSender(), "\u0001PING " + event.getMessage() + '\u0001'));
        } catch (IOException e) {
            log.error("Failed to respond to PING", e);
        }
    }

    @Subscribe
    public void onVersion(final VersionEvent event) {
        try {
            if (event.getReceiver() == this)
                sendAndLogCommand(IrcCommands.NoticeCommand.format(event.getSender(), "\u0001VERSION LucidBot " + Main.VERSION + '\u0001'));
        } catch (IOException e) {
            log.error("Failed to respond to VERSION", e);
        }
    }

    @Subscribe
    public void onInvite(final InviteEvent event) {
        try {
            if ("ChanServ".equalsIgnoreCase(event.getSender()) && this == event.getReceiver() &&
                    settings.getChannelNames(true).contains(lowerCase(event.getChannel())) &&
                    getNick().equals(event.getInvited())) {
                sendAndLogCommand(IrcCommands.JoinCommand.format(event.getChannel(), null));
            }
        } catch (IOException e) {
            log.error("Could not join the channel the bot was invited to: " + event.getChannel(), e);
        }
    }

    private static final Pattern nickNotRegistered = Pattern.compile("Your nickname isn't registered.*");
    private static final Pattern passwordAccepted = Pattern.compile("(Password accepted|I recognize you).*");

    @Subscribe
    public void onNotice(final NoticeEvent event) {
        if (event.getTarget().equals(nick) && "NickServ".equals(event.getSender())) {
            if (nickNotRegistered.matcher(event.getMessage()).matches()) {
                dispose();
                log.error("The bot's nick isn't registered: " + nick);
            } else if (passwordAccepted.matcher(event.getMessage()).matches()) {
                setIdentified(true);
                for (Channel channel : settings.getChannels()) {
                    try {
                        sendAndLogCommand(IrcCommands.JoinCommand.format(channel.getName(), null));
                    } catch (Exception e) {
                        log.error("Bot failed to issue the join command", e);
                    }
                }
            }
        }
    }

    @Subscribe
    public void onInviteRequired(final InviteRequiredEvent event) {
        if (event.getReceiver() == this) {
            try {
                sendAndLogCommand(ChanServCommands.InviteCommand.format(event.getChannel(), properties.get(IRC_CS_INVITE_REQUEST)));
            } catch (IOException e) {
                log.error("Bot failed to issue invite command", e);
            }
        }
    }

    @Subscribe
    public void onDisconnect(final DisconnectEvent event) {
        if (event.getInstance() == this) {
            connected.set(false);
            if (properties.getBoolean(AUTO_CONNECT_DISCONNECT) && !doNotAttemptReconnect) {
                attemptReconnect();
            } else {
                dispose();
            }
        }
    }

    private void attemptReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();
        if (attempts <= properties.getInteger(AUTO_CONNECT_ATTEMPTS))
            reconnectScheduler.scheduleReconnectAttempt(new ReconnectTask(this));
    }

    public void setDoNotAttemptReconnect(final boolean doNotAttemptReconnect) {
        this.doNotAttemptReconnect = doNotAttemptReconnect;
    }

    @Override
    public Runnable getShutdownRunner() {
        return new Runnable() {
            @Override
            public void run() {
                sendQuit();
                dispose();
            }
        };
    }

    private static class ReconnectTask implements Runnable {
        private final BotIRCInstance bot;

        private ReconnectTask(final BotIRCInstance bot) {
            this.bot = bot;
        }

        @Override
        public void run() {
            if (!bot.connected.get()) {
                try {
                    bot.connect();
                } catch (IOException e) {
                    bot.attemptReconnect();
                }
            }
        }
    }
}
