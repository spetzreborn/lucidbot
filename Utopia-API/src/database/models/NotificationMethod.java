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

package database.models;

import api.common.HasName;
import api.database.models.BotUser;
import api.tools.communication.MailException;
import api.tools.text.StringUtil;
import lombok.extern.log4j.Log4j;
import org.jivesoftware.smack.XMPPException;
import tools.communication.NotificationDeliverer;

@Log4j
public enum NotificationMethod implements HasName {
    PM {
        @Override
        public void deliver(final NotificationDeliverer notificationDeliverer, final BotUser user, final String subject,
                            final String message) {
            notificationDeliverer.deliverIrcNotification(user, message);
        }

        @Override
        public boolean userIsSupported(final BotUser user) {
            return true;
        }
    }, SMS {
        @Override
        public void deliver(final NotificationDeliverer notificationDeliverer, final BotUser user, final String subject,
                            final String message) {
            try {
                notificationDeliverer.deliverEmailNotification(user.getSms(), subject, message);
            } catch (MailException e) {
                log.error("Failed to send notification to " + user.getMainNick(), e);
            }
        }

        @Override
        public boolean userIsSupported(final BotUser user) {
            return user.isSmsConfirmed() && user.getSms() != null;
        }
    }, EMAIL {
        @Override
        public void deliver(final NotificationDeliverer notificationDeliverer, final BotUser user, final String subject,
                            final String message) {
            try {
                notificationDeliverer.deliverEmailNotification(user.getEmail(), subject, message);
            } catch (MailException e) {
                log.error("Failed to send notification to " + user.getMainNick(), e);
            }
        }

        @Override
        public boolean userIsSupported(final BotUser user) {
            return user.getEmail() != null;
        }
    }, GTALK {
        @Override
        public void deliver(final NotificationDeliverer notificationDeliverer, final BotUser user, final String subject,
                            final String message) {
            try {
                notificationDeliverer.deliverGTalkNotification(user.getGtalk(), message);
            } catch (XMPPException e) {
                log.error("Failed to send notification to " + user.getMainNick(), e);
            }
        }

        @Override
        public boolean userIsSupported(final BotUser user) {
            return user.getGtalk() != null;
        }
    };

    public static NotificationMethod getByName(String name) {
        return valueOf(StringUtil.getAsEnumStyleName(name));
    }

    public static String getRegexGroup() {
        return StringUtil.mergeNamed(values(), '|');
    }

    @Override
    public String getName() {
        return toString();
    }

    public abstract void deliver(final NotificationDeliverer notificationDeliverer, final BotUser user, final String subject,
                                 final String message);

    public abstract boolean userIsSupported(final BotUser user);
}
