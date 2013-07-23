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

import api.irc.communication.IRCMessageType;
import freemarker.core.Environment;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import static api.settings.PropertiesConfig.IRC_DEFAULT_PRIORITY;

final class IRCMessageDirective implements TemplateDirectiveModel {
    @Inject
    @Named(IRC_DEFAULT_PRIORITY)
    private int defaultPriority;

    @Override
    public void execute(final Environment env, final Map params, final TemplateModel[] loopVars, final TemplateDirectiveBody body) throws
                                                                                                                                   TemplateException,
                                                                                                                                   IOException {
        if (body == null) throw new TemplateException("No body", env);

        String type = "";
        String target;
        int priority = defaultPriority;
        boolean replyWithRecipient = false;

        Object targetParam = params.get("target");
        Object typeParam = params.get("type");
        Object priorityParam = params.get("priority");
        Object replyParam = params.get("replyWithRecipient");

        {
            target = targetParam == null ? null : ((TemplateScalarModel) targetParam).getAsString();
        }
        if (typeParam != null && typeParam instanceof TemplateScalarModel) {
            type = ((TemplateScalarModel) typeParam).getAsString();
            if (type == null)
                throw new TemplateException("Illegal value for param type. Legal values are: " + IRCMessageType.allNames(), env);
        }
        if (priorityParam != null && priorityParam instanceof TemplateNumberModel) {
            priority = ((TemplateNumberModel) priorityParam).getAsNumber().intValue();
        }
        if (replyParam != null && replyParam instanceof TemplateBooleanModel) {
            replyWithRecipient = ((TemplateBooleanModel) replyParam).getAsBoolean();
        }

        Writer out = env.getOut();
        out.append(type).append('(');
        out.append(String.valueOf(priority));
        if (target != null) out.append(',').append(target);
        if (replyWithRecipient) out.append(',').append("true");
        out.append(")<{");
        body.render(out);
        out.append("}>");
    }
}
