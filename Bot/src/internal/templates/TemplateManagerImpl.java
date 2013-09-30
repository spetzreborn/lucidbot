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

package internal.templates;

import api.irc.IRCFormatting;
import api.irc.communication.IRCMessage;
import api.irc.communication.IRCMessageFactory;
import api.irc.communication.IRCOutput;
import api.runtime.IRCContext;
import api.templates.TemplateManager;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static api.tools.text.StringUtil.isNotNullOrEmpty;
import static com.google.common.base.Preconditions.checkNotNull;

@Log4j
final class TemplateManagerImpl implements TemplateManager {
    private final Configuration cfg;
    private final IRCMessageFactory ircMessageFactory;

    @Inject
    TemplateManagerImpl(final Configuration cfg, final IRCMessageFactory ircMessageFactory) {
        this.cfg = checkNotNull(cfg);
        this.ircMessageFactory = checkNotNull(ircMessageFactory);
    }

    @Override
    public Collection<IRCOutput> createOutputFromTemplate(final Map<String, Object> data,
                                                          final String templateName,
                                                          final IRCContext context) {
        if (context.getCommandPrefixesUsed() > 1) {
            String alternativeTemplate = resolveAlternativeTemplateName(templateName, context.getCommandPrefixesUsed());
            try {
                return process(data, alternativeTemplate, context);
            } catch (IOException e) {
                log.error("", e);
            }
        }
        try {
            return process(data, templateName, context);
        } catch (IOException e) {
            log.error("", e);
        }
        return Collections.emptyList();
    }

    private Collection<IRCOutput> process(final Map<String, Object> data, final String templateName, final IRCContext context) throws IOException {
        try {
            String view = processTemplate(data, templateName);
            return processIRCView(view, context);
        } catch (TemplateProcessingException e) {
            log.error("", e);
        }
        return Collections.emptyList();
    }

    private static String resolveAlternativeTemplateName(final String originalName, final int templateVersion) {
        int fileEndingIndex = originalName.lastIndexOf(TEMPLATE_FILE_EXTENSION);
        return fileEndingIndex == -1 ? originalName + templateVersion : originalName.substring(0, fileEndingIndex) + templateVersion + TEMPLATE_FILE_EXTENSION;
    }

    @Override
    public String processTemplate(final Map<String, Object> data, final String templateName) throws IOException, TemplateProcessingException {
        Template template = cfg.getTemplate(templateName.endsWith(TEMPLATE_FILE_EXTENSION) ? templateName : templateName + TEMPLATE_FILE_EXTENSION);
        StringWriter writer = new StringWriter();

        data.putAll(IRCFormatting.getFormattingOptionsMap());

        try {
            template.process(data, writer);
            return writer.toString();
        } catch (TemplateException e) {
            throw new TemplateProcessingException(e);
        }
    }

    private Collection<IRCOutput> processIRCView(final String view, final IRCContext context) {
        Map<String, IRCOutput> groupingMap = new HashMap<>();
        for (IRCMessage message : ircMessageFactory.parseAndCreateNewMessages(view, context)) {
            if (isNotNullOrEmpty(message.getRawMessage())) {
                String compoundKey = message.getTarget().getName() + ' ' + message.getPriority();
                if (groupingMap.containsKey(compoundKey)) {
                    groupingMap.get(compoundKey).addOutput(message);
                } else {
                    IRCOutput ircOutput = message.isHandlingReceiverUsed() ? new IRCOutput(context.getReceiver(), message) : new IRCOutput(message);
                    groupingMap.put(compoundKey, ircOutput);
                }
            }
        }
        return new TreeSet<>(groupingMap.values());
    }
}
