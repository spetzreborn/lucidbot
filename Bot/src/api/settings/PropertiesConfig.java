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

package api.settings;

import spi.settings.PropertiesSpecification;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that contains defaults for properties
 */
public class PropertiesConfig implements PropertiesSpecification {
    public static final String SETUP_ALLOWED_IPS = "Setup.Allowed.IPs";
    public static final String DB_HOST = "Core.Database.Host";
    public static final String DB_NAME = "Core.Database.Name";
    public static final String DB_USERNAME = "Core.Database.User";
    public static final String DB_PASSWORD = "Core.Database.Password";
    public static final String WEB_SERVER_PORT = "Core.WebServer.Port";
    public static final String EMAIL_HOST = "Core.Email.Host";
    public static final String EMAIL_PORT = "Core.Email.Port";
    public static final String EMAIL_TLS = "Core.Email.UseTLS";
    public static final String EMAIL_USERNAME = "Core.Email.Username";
    public static final String EMAIL_PASSWORD = "Core.Email.Password";
    public static final String GTALK_HOST = "Core.GTalk.Host";
    public static final String GTALK_SERVICE_NAME = "Core.GTalk.ServiceName";
    public static final String GTALK_PORT = "Core.GTalk.Port";
    public static final String GTALK_USERNAME = "Core.GTalk.Username";
    public static final String GTALK_PASSWORD = "Core.GTalk.Password";
    public static final String GTALK_ENABLED = "Core.GTalk.Enabled";
    public static final String IRC_SERVER = "Core.IRC.Server";
    public static final String IRC_PORT = "Core.IRC.Port";
    public static final String IRC_SERVER_PASSWORD = "Core.IRC.ServerPassword";
    public static final String IRC_AUTH_REQUEST = "IRC.Authentication.Request";
    public static final String IRC_AUTH_RESPONSE = "IRC.Authentication.Response";
    public static final String IRC_CS_INVITE_REQUEST = "IRC.CSInvite.Request";
    public static final String IRC_STATUSREPLY = "IRC.StatusReply.Identified";
    public static final String IRC_DEFAULT_PRIORITY = "IRC.Message.DefaultPriority";
    public static final String IRC_MAX_LENGTH = "IRC.Message.MaxLineLength";
    public static final String IRC_DELAY_STRATEGY = "IRC.DelayStrategy";
    public static final String IRC_FIXED_DELAY = "IRC.Delay.Millis";
    public static final String AUTO_CONNECT_STARTUP = "Core.AutoConnect.OnStartup";
    public static final String AUTO_CONNECT_DISCONNECT = "Core.AutoConnect.OnDisconnect";
    public static final String AUTO_CONNECT_ATTEMPTS = "Core.AutoConnect.Attempts";
    public static final String AUTO_CONNECT_DELAY = "Core.AutoConnect.AttemptDelay";
    public static final String COMMANDS_PREFIX = "Commands.Prefix";
    public static final String LOGIN_COMMAND = "Commands.OnLoginCall";
    public static final String ALLOW_USER_REGISTRATION = "Users.AllowRegistration";
    public static final String DEFAULT_PASSWORD_ACCESS_ENABLED = "Users.DefaultPassword.AccessEnabled";
    public static final String TRAY_TOOL_TIP = "Core.TrayIcon.ToolTip";

    private final Map<String, String> defaults = new HashMap<>();

    public PropertiesConfig() {
        defaults.put(SETUP_ALLOWED_IPS, "127.0.0.1, 192.168.*.*");
        defaults.put(ALLOW_USER_REGISTRATION, "false");
        defaults.put(DEFAULT_PASSWORD_ACCESS_ENABLED, "false");
        defaults.put(IRC_AUTH_REQUEST, "STATUS $MULTI_USERS:16$");
        defaults.put(IRC_AUTH_RESPONSE, "STATUS $CURRENT_NICK$ $STATUS_NUMBER$");
        defaults.put(IRC_CS_INVITE_REQUEST, "INVITE $CHANNEL$");
        defaults.put(IRC_STATUSREPLY, "3");
        defaults.put(COMMANDS_PREFIX, "!");
        defaults.put(LOGIN_COMMAND, "");
        defaults.put(WEB_SERVER_PORT, "49998");
        defaults.put(AUTO_CONNECT_STARTUP, "true");
        defaults.put(AUTO_CONNECT_DISCONNECT, "true");
        defaults.put(AUTO_CONNECT_ATTEMPTS, "500");
        defaults.put(AUTO_CONNECT_DELAY, "75");
        defaults.put(IRC_SERVER, "irc.utonet.org");
        defaults.put(IRC_PORT, "6667");
        defaults.put(IRC_SERVER_PASSWORD, "");
        defaults.put(IRC_DEFAULT_PRIORITY, "5");
        defaults.put(IRC_MAX_LENGTH, "350");
        defaults.put(IRC_DELAY_STRATEGY, "2");
        defaults.put(IRC_FIXED_DELAY, "750");
        defaults.put(EMAIL_HOST, "smtp.gmail.com");
        defaults.put(EMAIL_PORT, "587");
        defaults.put(EMAIL_TLS, "true");
        defaults.put(GTALK_HOST, "talk.google.com");
        defaults.put(GTALK_SERVICE_NAME, "gmail.com");
        defaults.put(GTALK_PORT, "5222");
        defaults.put(GTALK_ENABLED, "false");
        defaults.put(TRAY_TOOL_TIP, "LucidBot");
    }

    @Override
    public Path getFilePath() {
        return Paths.get("lucidbot.properties");
    }

    @Override
    public Map<String, String> getDefaults() {
        return Collections.unmodifiableMap(defaults);
    }
}
