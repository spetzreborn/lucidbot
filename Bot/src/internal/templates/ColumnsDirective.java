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
import api.tools.text.StringUtil;
import freemarker.core.Environment;
import freemarker.template.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Pattern;

final class ColumnsDirective implements TemplateDirectiveModel {
    private static final Pattern NORMAL_TEXT_PATTERN = Pattern.compile(IRCFormatting.NORMAL);

    @Override
    public void execute(final Environment env, final Map params, final TemplateModel[] loopVars, final TemplateDirectiveBody body) throws
            TemplateException,
            IOException {
        if (body == null) throw new TemplateException("No body", env);

        String separator = " I ";
        char padding = ' ';
        boolean underlined = false;
        int[] colLengths;

        Object colLengthsParam = params.get("colLengths");
        if (colLengthsParam == null || !(colLengthsParam instanceof TemplateSequenceModel))
            throw new TemplateException("colLengths param missing", env);
        Object sepParam = params.get("separator");
        Object paddingParam = params.get("padding");
        Object underlinedParam = params.get("underlined");

        try {
            if (sepParam != null && sepParam instanceof TemplateScalarModel) {
                separator = ((TemplateScalarModel) sepParam).getAsString();
            }
            if (paddingParam != null && paddingParam instanceof TemplateScalarModel) {
                padding = ((TemplateScalarModel) paddingParam).getAsString().charAt(0);
            }
            if (underlinedParam != null && underlinedParam instanceof TemplateBooleanModel) {
                underlined = ((TemplateBooleanModel) underlinedParam).getAsBoolean();
            }
            {
                TemplateSequenceModel param = (TemplateSequenceModel) colLengthsParam;
                colLengths = new int[param.size()];
                for (int i = 0; i < param.size(); ++i) {
                    colLengths[i] = ((TemplateNumberModel) param.get(i)).getAsNumber().intValue();
                }
            }

            StringWriter writer = new StringWriter();
            body.render(writer);
            String allLines = StringUtil.limitedTrim(writer.toString());
            if (underlined) allLines = NORMAL_TEXT_PATTERN.matcher(allLines).replaceAll(IRCFormatting.NORMAL + IRCFormatting.UNDERLINE);
            StringBuilder builder = new StringBuilder(500);
            if (underlined) builder.append(IRCFormatting.UNDERLINE);
            int counter = 1;
            for (String line : StringUtil.splitOnEndOfLine(allLines)) {
                String trimmed = StringUtil.limitedTrim(line) + IRCFormatting.NORMAL;
                if (underlined) trimmed += IRCFormatting.UNDERLINE;
                builder.append(separator);
                builder.append(StringUtil.toFixLength(trimmed, colLengths[counter - 1], padding));
                ++counter;
                if (counter > colLengths.length) {
                    builder.append(separator).append('\n');
                    env.getOut().write(builder.toString());
                    builder = new StringBuilder(500);
                    if (underlined) builder.append(IRCFormatting.UNDERLINE);
                    counter = 1;
                }
            }
        } catch (IOException | TemplateModelException e) {
            throw new TemplateException(e, env);
        }
    }
}
