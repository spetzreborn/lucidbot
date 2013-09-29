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

package internal.irc;

import api.events.irc.NoticeEvent;
import api.events.irc.UserAuthenticationEvent;
import api.irc.BotIRCInstance;
import api.irc.communication.IRCAccess;
import api.irc.entities.IRCUser;
import api.settings.PropertiesCollection;
import api.settings.PropertiesConfig;
import api.tools.text.IRCFormattingUtil;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.log4j.Log4j;
import spi.events.EventListener;

import javax.inject.Inject;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.settings.PropertiesConfig.IRC_AUTH_REQUEST;
import static api.settings.PropertiesConfig.IRC_AUTH_RESPONSE;
import static api.tools.text.StringUtil.merge;
import static com.google.common.base.Preconditions.checkNotNull;

@Log4j
public class Authenticator implements EventListener {
    private static final Pattern MULTI_USERS_AUTH = Pattern.compile("\\$MULTI_USERS:(\\d+)\\$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_USER_AUTH = Pattern.compile("\\$SINGLE_USER\\$", Pattern.CASE_INSENSITIVE);

    private static final Pattern STATUS_NUMBER_RESULT = Pattern.compile("\\$STATUS_NUMBER\\$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_RESULT = Pattern.compile("\\$ACCOUNT\\$", Pattern.CASE_INSENSITIVE);
    private static final String CURRENT_NICK = "$CURRENT_NICK$";

    private final PropertiesCollection properties;
    private final IRCAccess access;
    private final EventBus eventBus;

    @Inject
    public Authenticator(final PropertiesCollection properties, final IRCAccess access, final EventBus eventBus) {
        this.properties = checkNotNull(properties);
        this.access = checkNotNull(access);
        this.eventBus = checkNotNull(eventBus);
    }

    @Subscribe
    public void onNotice(final NoticeEvent event) {
        if ("NickServ".equals(event.getSender())) {
            checkIfAuthenticated(event);
        }
    }

    public void sendAuthenticationCheck(final String user) {
        sendAuthenticationCheck(Lists.newArrayList(user));
    }

    public void sendAuthenticationCheck(final List<String> users) {
        String request = properties.get(IRC_AUTH_REQUEST);
        Matcher matcher = MULTI_USERS_AUTH.matcher(request);
        if (matcher.find()) {
            sendMultiUsersMessage(users, matcher);
        } else {
            matcher = SINGLE_USER_AUTH.matcher(request);
            if (matcher.find()) {
                sendSingleUserMessage(users, matcher);
            }
        }
    }

    private void sendMultiUsersMessage(final List<String> users, final Matcher matcher) {
        int maxPerCommand = Integer.parseInt(matcher.group(1));
        for (int i = 0; i < users.size(); i += maxPerCommand) {
            int end = Math.min(users.size(), i + maxPerCommand - 1);
            String nameList = merge(users.subList(i, end), ' ');
            String send = matcher.replaceFirst(nameList);
            access.sendPrivateMessage(new IRCUser("NickServ"), send);
        }
    }

    private void sendSingleUserMessage(final List<String> users, final Matcher matcher) {
        for (String user : users) {
            String send = matcher.replaceFirst(user);
            access.sendPrivateMessage(new IRCUser("NickServ"), send);
        }
    }

    private void checkIfAuthenticated(final NoticeEvent event) {
        String response = properties.get(IRC_AUTH_RESPONSE);
        if (!response.contains(CURRENT_NICK)) {
            Authenticator.log.error("The " + IRC_AUTH_RESPONSE + " property is not configured correctly. " +
                    "It has no $CURRENT_NICK$ placeholder");
            return;
        }

        String serverResponse = event.getMessage();
        Matcher matcher = STATUS_NUMBER_RESULT.matcher(response);
        if (matcher.find()) {
            parseStatusNumberResponse(serverResponse, matcher, event.getReceiver());
        } else {
            matcher = ACCOUNT_RESULT.matcher(response);
            if (matcher.find()) {
                parseAccountResponse(serverResponse, matcher, event.getReceiver());
            }
        }
    }

    private void parseStatusNumberResponse(final String serverResponse, final Matcher matcher, final BotIRCInstance receiver) {
        String catcher = matcher.replaceFirst("(?<status>\\\\d)");
        catcher = catcher.replace(CURRENT_NICK, "(?<current>[^ ]+)");
        String unformattedServerResponse = IRCFormattingUtil.removeFormattingAndColors(serverResponse.trim());
        Matcher statusMatcher = Pattern.compile(catcher, Pattern.CASE_INSENSITIVE).matcher(unformattedServerResponse);
        if (statusMatcher.matches()) {
            int status = Integer.parseInt(statusMatcher.group("status"));
            Integer authStatus = properties.getInteger(PropertiesConfig.IRC_STATUSREPLY);
            String currentNick = statusMatcher.group("current");
            if (authStatus != null && authStatus.equals(status)) {
                eventBus.post(new UserAuthenticationEvent(currentNick, currentNick, receiver));
            }
        }
    }

    private void parseAccountResponse(final String serverResponse, final Matcher matcher, final BotIRCInstance receiver) {
        String catcher = matcher.replaceFirst("(?<base>[^ .]+)");
        catcher = catcher.replace(CURRENT_NICK, "(?<current>[^ ]+)");
        String unformattedServerResponse = IRCFormattingUtil.removeFormattingAndColors(serverResponse.trim());
        Matcher accountMatcher = Pattern.compile(catcher, Pattern.CASE_INSENSITIVE).matcher(unformattedServerResponse);
        if (accountMatcher.matches()) {
            String baseNick = accountMatcher.group("base");
            String currentNick = accountMatcher.group("current");
            eventBus.post(new UserAuthenticationEvent(currentNick, baseNick, receiver));
        }
    }
}
