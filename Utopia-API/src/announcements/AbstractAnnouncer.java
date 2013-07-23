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

package announcements;

import api.database.models.ChannelType;
import api.irc.IRCEntityManager;
import api.irc.IRCFormatting;
import api.irc.communication.IRCAccess;
import api.irc.entities.IRCChannel;
import api.templates.TemplateManager;
import api.tools.text.StringUtil;
import lombok.extern.log4j.Log4j;

import java.io.IOException;
import java.util.Map;

import static api.tools.text.StringUtil.isNotNullOrEmpty;

@Log4j
public class AbstractAnnouncer {
    private final TemplateManager templateManager;
    private final IRCEntityManager ircEntityManager;
    private final IRCAccess ircAccess;

    protected AbstractAnnouncer(final TemplateManager templateManager, final IRCEntityManager ircEntityManager, final IRCAccess ircAccess) {
        this.templateManager = templateManager;
        this.ircEntityManager = ircEntityManager;
        this.ircAccess = ircAccess;
    }

    protected String[] compileTemplateOutput(final Map<String, Object> templateData, final String templateName) {
        try {
            templateData.putAll(IRCFormatting.getFormattingOptionsMap());
            String rawOutput = templateManager.processTemplate(templateData, templateName);
            return StringUtil.splitOnEndOfLine(rawOutput);
        } catch (IOException | TemplateManager.TemplateProcessingException e) {
            log.error("Could not process template for announcement", e);
        }
        return null;
    }

    //TODO FUTURE allow config of channel type
    protected void announce(final ChannelType channelType, final String... output) {
        if (output == null) return;
        for (IRCChannel channel : ircEntityManager.getChannels()) {
            if (channel.getType() == channelType) {
                for (String line : output) {
                    if (isNotNullOrEmpty(line)) ircAccess.sendMessage(channel.getMainBotInstance(), channel, line);
                }
            }
        }
    }

    protected void announce(final String channel, final String... output) {
        if (output == null) return;
        IRCChannel ircChannel = ircEntityManager.getChannel(channel);
        if (ircChannel != null) {
            for (String line : output) {
                if (isNotNullOrEmpty(line)) ircAccess.sendMessage(ircChannel.getMainBotInstance(), ircChannel, line);
            }
        }
    }
}
