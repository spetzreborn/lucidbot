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

package tools.communication;

import api.database.models.BotUser;
import api.irc.IRCEntityManager;
import api.irc.communication.IRCAccess;
import api.irc.entities.IRCChannel;
import api.irc.entities.IRCUser;
import api.tools.communication.JabberClient;
import api.tools.communication.MailClient;
import api.tools.communication.MailException;
import com.google.common.collect.Lists;
import database.models.Notification;
import org.jivesoftware.smack.XMPPException;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static api.tools.text.StringUtil.isNotNullOrEmpty;

/**
 * A class that can deliver notifications on IRC or through e-mail
 */
public class NotificationDeliverer {
    private final IRCEntityManager ircEntityManager;
    private final IRCAccess ircAccess;
    private final MailClient mailClient;
    private final JabberClient jabberClient;

    @Inject
    public NotificationDeliverer(final IRCEntityManager ircEntityManager, final IRCAccess ircAccess, final MailClient mailClient,
                                 final JabberClient jabberClient) {
        this.ircEntityManager = ircEntityManager;
        this.ircAccess = ircAccess;
        this.mailClient = mailClient;
        this.jabberClient = jabberClient;
    }

    /**
     * Sends all the appropriate notifications for the specified type
     *
     * @param notifications the notifications
     * @param subject       the subject of the message
     * @param message       the message
     */
    public void deliverNotifications(final Iterable<Notification> notifications, final String subject, final String message) {
        for (Notification notification : notifications) {
            notification.getMethod().deliver(this, notification.getUser(), subject, message);
        }
    }

    /**
     * Delivers the specified message to all of the user's currently connected instances.
     * Sends a private message.
     *
     * @param user    the user to notify
     * @param message the message to send
     */
    public void deliverIrcNotification(final BotUser user, final String message) {
        Set<IRCUser> deliveredTo = new HashSet<>();
        for (IRCChannel channel : ircEntityManager.getChannels()) {
            for (IRCUser ircUser : ircEntityManager.getAllOfUsersConnections(user.getMainNick())) {
                if (!deliveredTo.contains(ircUser)) {
                    ircAccess.sendPrivateMessage(channel.getMainBotInstance(), ircUser, message);
                    deliveredTo.add(ircUser);
                }
            }
        }
    }

    /**
     * Delivers a notification by email.
     *
     * @param address the email address, may not be null
     * @param subject the subject of the mail, may not be null
     * @param message the content of the email, may be null
     * @throws MailException if the message cannot be delivered
     */
    public void deliverEmailNotification(final String address, final String subject, final String message) throws MailException {
        if (isNotNullOrEmpty(address) && isNotNullOrEmpty(subject))
            mailClient.sendMail("Bot", Lists.newArrayList(address), subject, message);
    }

    /**
     * Delivers a notification through GTalk.
     *
     * @param address the user's address
     * @param message the content of the message
     * @return true if the message was delivered, false if the user wasn't online or GTalk isn't enabled
     * @throws XMPPException if the message cannot be delivered
     */
    public boolean deliverGTalkNotification(final String address, final String message) throws XMPPException {
        return isNotNullOrEmpty(address) && isNotNullOrEmpty(message) && jabberClient.isEnabled() &&
                jabberClient.sendGTalkMessage(address, message);
    }
}
