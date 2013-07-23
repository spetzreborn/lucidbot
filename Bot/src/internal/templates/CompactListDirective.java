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
import freemarker.core.Environment;
import freemarker.template.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static api.settings.PropertiesConfig.IRC_MAX_LENGTH;
import static api.tools.text.StringUtil.limitedTrim;
import static api.tools.text.StringUtil.splitOnEndOfLine;

final class CompactListDirective implements TemplateDirectiveModel {
    @Inject
    @Named(IRC_MAX_LENGTH)
    private int maxLength;

    @Override
    public void execute(final Environment env, final Map params, final TemplateModel[] loopVars, final TemplateDirectiveBody body) throws
            TemplateException,
            IOException {
        if (body == null) throw new TemplateException("No body", env);

        String intro = "";
        String separator = ", ";
        Object introParam = params.get("intro");
        Object sepParam = params.get("separator");
        if (introParam != null && introParam instanceof TemplateScalarModel) intro = ((TemplateScalarModel) introParam).getAsString();
        if (sepParam != null && sepParam instanceof TemplateScalarModel) separator = ((TemplateScalarModel) sepParam).getAsString();
        env.getOut().write(intro);

        StringWriter writer = new StringWriter();
        body.render(writer);
        String allLines = writer.toString();
        StringBuilder builder = new StringBuilder(maxLength);
        int introLength = intro.length();
        for (String line : splitOnEndOfLine(allLines)) {
            String trimmed = limitedTrim(line) + IRCFormatting.NORMAL;
            if (introLength + builder.length() + trimmed.length() >= maxLength) {
                env.getOut().write(builder.toString() + '\n');
                builder = new StringBuilder(maxLength);
                introLength = 0;
            }
            if (builder.length() > 0) builder.append(separator);
            builder.append(trimmed);
        }
        if (builder.length() > 0) env.getOut().write(builder.toString());
    }
}
