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

import api.irc.ValidationType;
import api.settings.PropertiesCollection;
import api.tools.text.StringUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static api.settings.PropertiesConfig.*;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class that handles sending emails
 */
@ParametersAreNonnullByDefault
public final class MailClient {
    private final PropertiesCollection properties;

    @Inject
    public MailClient(final PropertiesCollection properties) {
        this.properties = checkNotNull(properties);
    }

    /**
     * Sends an email
     *
     * @param from    from who
     * @param to      to who
     * @param subject subject of the email
     * @param content the content (body) of the email
     * @throws MessagingException .
     */
    public void sendMail(final String from, final Iterable<String> to, final String subject, final String content) throws
            MailException {
        String host = properties.get(EMAIL_HOST);
        String port = properties.get(EMAIL_PORT);
        String useTls = properties.get(EMAIL_TLS);
        String username = properties.get(EMAIL_USERNAME);
        String password = properties.get(EMAIL_PASSWORD);
        if (StringUtil.isNullOrEmpty(host) || StringUtil.isNullOrEmpty(port) || !StringUtil.validateInput(ValidationType.INT, port) ||
                StringUtil.isNullOrEmpty(useTls) || !StringUtil.validateInput(ValidationType.BOOLEAN, useTls) ||
                StringUtil.isNullOrEmpty(username)) throw new MailException("Cannot send emails due to missing email properties");

        java.util.Properties props = new java.util.Properties();
        props.setProperty("mail.smtp.host", host);
        props.setProperty("mail.smtp.port", port);
        props.setProperty("mail.smtp.starttls.enable", useTls);
        props.setProperty("mail.smtp.auth", "true");

        Transport t = null;
        try {
            final Session session = Session.getDefaultInstance(props, null);

            final Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            for (final String recepient : to) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recepient));
            }
            message.setSubject(from + " says: " + subject);
            message.setContent(content == null ? "" : content, "text/plain");

            t = session.getTransport("smtp");
            t.connect(username, password);
            t.sendMessage(message, message.getAllRecipients());
        } catch (Exception e) {
            throw new MailException(e);
        } finally {
            if (t != null) {
                try {
                    t.close();
                } catch (MessagingException ignore) {
                }
            }
        }
    }
}